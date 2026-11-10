# Federated SPIRE + Keycloak demo

This follows the [simple demo](../simple/README.md), but starts two local SPIRE servers and agents. East and west have separate trust domains. SPIRE exchanges their bundles; a small local HTTPS endpoint publishes east's view as a SPIFFE Bundle Map for Keycloak.

## Prerequisites

Linux with Bash, `curl`, `jq`, OpenSSL and a local Keycloak distribution containing the SPIFFE bundle-map support. The scripts download the same SPIRE 1.13.3 Linux archive as the simple demo. No Kubernetes or Python is needed.

Run each foreground process in its own terminal from this directory:

```sh
./start-spire-server.sh east
./start-spire-server.sh west
```

Once both servers are ready, configure federation:

```sh
./configure-federation.sh
```

Start the two agents, then the bundle endpoint:

```sh
./start-spire-agent.sh east
./start-spire-agent.sh west
./start-bundle-endpoint.sh
```

Start Keycloak in another terminal. By default, it uses the adjacent Keycloak checkout's `quarkus/dist/target/keycloak-999.0.0-SNAPSHOT.tar.gz`; set `KC_DIST` for another local archive.

```sh
./start-keycloak.sh
./configure-keycloak.sh
```

Fetch a JWT-SVID or make a client credentials grant from either trust domain:

```sh
./fetch-jwt-svid.sh east
./fetch-jwt-svid.sh west
./client-credential-grant.sh east
./client-credential-grant.sh west
```

Each grant should return an access token. An unmapped subject should fail with HTTP 401:

```sh
./client-credential-grant.sh east unmapped
```

The bundle map is served at `https://localhost:9443/bundle`. Keycloak trusts its locally generated certificate. Only public bundles are served; the endpoint removes a stale map if SPIRE stops responding. Check its assembly with `./test-bundle-map.sh`.

## Rotation and withdrawal

The SPIRE server API is available through the local sockets in `.runtime/east/server.sock` and `.runtime/west/server.sock`. For example, rotate west's JWT authority, wait for federation and the bundle map to update, then retry its grant:

```sh
spire_west() { .runtime/spire/bin/spire-server "$@" -socketPath "$PWD/.runtime/west/server.sock"; }
new_kid=$(spire_west localauthority jwt prepare -output json | jq -er '.prepared_authority.authority_id')
until curl --silent --fail --cacert .runtime/demo-map.crt https://localhost:9443/bundle | jq -e --arg kid "$new_kid" 'any(.trust_domains["west.example.test"].keys[]; .kid == $kid)' >/dev/null; do sleep 2; done
spire_west localauthority jwt activate -authorityID "$new_kid"
./client-credential-grant.sh west
```

To withdraw west, remove east's federation relationship and its cached bundle:

```sh
spire_east() { .runtime/spire/bin/spire-server "$@" -socketPath "$PWD/.runtime/east/server.sock"; }
spire_east federation delete -id spiffe://west.example.test
spire_east bundle delete -id spiffe://west.example.test -mode dissociate
```

The map then drops west. After Keycloak's bundle cache expires (up to five minutes), a west grant should fail while east still succeeds.

Stop the foreground processes with Ctrl-C, then run `./cleanup.sh` to delete generated `.runtime` files.
