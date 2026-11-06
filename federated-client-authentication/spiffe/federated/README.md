# Federated SPIRE + Keycloak demo

This demo runs SPIRE in two isolated k3d clusters, then authenticates Keycloak clients from both trust domains. The [SPIFFE Federation specification](https://spiffe.io/docs/latest/spiffe-specs/spiffe_federation/) defines bundle exchange between domains. The [simple demo](../simple/README.md) runs a single SPIRE server and agent.

## Scripts

The names and steps follow the simple demo:

| Script | Simple demo | Federated demo |
|---|---|---|
| `start-spire-server.sh` | Start a local SPIRE server | Create two clusters, install SPIRE and configure federation |
| `start-spire-agent.sh` | Start a local agent | Not needed: the Helm chart starts agents on each node |
| `start-bundle-endpoint.sh` | Not needed: the server exposes its bundle | Publish east's known bundles over HTTPS |
| `start-keycloak.sh` | Start Keycloak trusting the bundle certificate | Start the local Keycloak build trusting the bundle-map certificate |
| `configure-keycloak.sh` | Create a realm, IdP and client | Create a realm and one IdP/client per domain |
| `fetch-jwt-svid.sh` | Fetch a JWT-SVID from the local agent | Fetch a JWT-SVID from an in-cluster workload |
| `client-credential-grant.sh` | Exchange the JWT-SVID for a token | Exchange an east or west JWT-SVID for a token |

## Topology and trust

`east` and `west` are separate k3d clusters with independent SPIRE servers and trust domains (`east.example.test` and `west.example.test`). SPIRE federation uses the `https_spiffe` profile. Setup exchanges their initial trust bundles; SPIRE then polls authenticated peer endpoints and updates its federated bundle store.

The local bundle endpoint reads only east SPIRE's server API. Bash and `jq` assemble its current known bundles into a SPIFFE Bundle Map; OpenSSL serves the map over HTTPS on localhost. It never fetches a foreign endpoint. Keycloak explicitly trusts the generated demo certificate.

Only public bundles are served. Private keys and kubeconfigs stay outside the served directory. The map is replaced atomically every two seconds and removed if SPIRE fails or no refresh succeeds for fifteen seconds. Do not expose the demo credentials or endpoint outside this local test environment.

## Prerequisites

macOS or Linux with Bash, Docker, k3d, kubectl, Helm, `jq`, OpenSSL and a local Keycloak distribution containing bundle-map support. No Python is required.

Setup creates only `kc-spiffe-east`, `kc-spiffe-west`, and the `kc-spiffe-federation` Docker network. It refuses to overwrite existing resources and does not change your default kubeconfig context.

## Run

Run these commands from this directory. Set up SPIRE first:

```sh
./start-spire-server.sh
```

Start the bundle endpoint and leave it running:

```sh
./start-bundle-endpoint.sh
```

In another terminal, start Keycloak and leave it running:

```sh
./start-keycloak.sh
```

By default, the script extracts the adjacent Keycloak checkout's `quarkus/dist/target/keycloak-999.0.0-SNAPSHOT.tar.gz`. Set `KC_DIST` to use another local archive. It replaces the generated installation on each run so a rebuilt distribution is used without old dependencies.

Once Keycloak is ready, configure the realm and clients:

```sh
./configure-keycloak.sh
```

## Verify

Both grants must return an access token:

```sh
./client-credential-grant.sh east
./client-credential-grant.sh west
```

An unmapped east subject must return HTTP 401:

```sh
./client-credential-grant.sh east unmapped
```

For domain isolation, temporarily map the east client's issuer to the west IdP. An east grant must return HTTP 401. Restore the east issuer afterward.

Check bundle-map assembly without clusters:

```sh
./test-bundle-map.sh
```

### Rotation

These helpers address each SPIRE server through its own cluster:

```sh
export KUBECONFIG="$PWD/.runtime/kc-spiffe-east.kubeconfig:$PWD/.runtime/kc-spiffe-west.kubeconfig"
spire_east() { kubectl --context k3d-kc-spiffe-east -n spire exec spire-server-0 -c spire-server -- /opt/spire/bin/spire-server "$@"; }
spire_west() { kubectl --context k3d-kc-spiffe-west -n spire exec spire-server-0 -c spire-server -- /opt/spire/bin/spire-server "$@"; }
```

Prepare west's next JWT key. Wait for SPIRE federation to deliver it to east, then for the local endpoint to publish it:

```sh
new_kid=$(spire_west localauthority jwt prepare -output json | jq -er '.prepared_authority.authority_id')
until spire_east bundle list -id spiffe://west.example.test -format spiffe | jq -e --arg kid "$new_kid" 'any(.keys[]; .kid == $kid)' >/dev/null; do sleep 2; done
until curl --silent --fail --cacert .runtime/demo-map.crt https://localhost:9443/bundle | jq -e --arg kid "$new_kid" 'any(.trust_domains["west.example.test"].keys[]; .kid == $kid)' >/dev/null; do sleep 2; done
spire_west localauthority jwt activate -authorityID "$new_kid"
./client-credential-grant.sh west
```

The fresh grant must succeed without restarting Keycloak or manually updating the map.

### Trust withdrawal

Remove west's federation relationship from east, then dissociate its cached bundle:

```sh
kubectl --context k3d-kc-spiffe-east delete clusterfederatedtrustdomain demo-west
spire_east bundle delete -id spiffe://west.example.test -mode dissociate
until curl --silent --fail --cacert .runtime/demo-map.crt https://localhost:9443/bundle | jq -e '.trust_domains | has("west.example.test") | not' >/dev/null; do sleep 2; done
```

Wait at least five minutes for Keycloak's bundle cache to expire. A fresh west grant must return HTTP 401; an east grant must still succeed. Do not clear caches, change the IdP or restart Keycloak during this check.

## Cleanup

Stop `start-keycloak.sh` and `start-bundle-endpoint.sh` with Ctrl-C, then run:

```sh
./cleanup.sh
```

Cleanup deletes only the two demo clusters, their Docker network and generated `.runtime` files.
