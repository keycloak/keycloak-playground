# Simple SPIFFE demo

This workflow runs one local SPIRE server and agent, then authenticates a Keycloak client with a JWT-SVID. Read the [SPIFFE concepts](https://spiffe.io/docs/latest/spiffe-about/spiffe-concepts/) and [JWT-SVID specification](https://spiffe.io/docs/latest/spiffe-specs/jwt-svid/) for the identity model. [SPIRE](https://spiffe.io/docs/latest/spire-about/spire-concepts/) implements the server and agent used here.

## Prerequisites

- Linux
- OpenJDK 17 or 21

Run the commands from this `simple/` directory. Start the server in one terminal and the agent in another:

```sh
./start-spire-server.sh
./start-spire-agent.sh
```

Fetch a JWT-SVID and check the local bundle endpoint:

```sh
./fetch-jwt-svid.sh
curl --insecure https://localhost:8543
```

In another terminal, start Keycloak and wait for it to become ready:

```sh
./start-keycloak.sh
```

Once Keycloak is ready, configure the demo realm and client in another terminal:

```sh
./configure-keycloak.sh
```

The Keycloak script uses the SPIRE demo certificate as its truststore. Make a client-credentials grant:

```sh
./client-credential-grant.sh
```

The response includes an access token. If you have [`kct`](https://github.com/stianst/keycloak-tokens-cli), decode the SVID or access token with:

```sh
kct decode "$(./fetch-jwt-svid.sh)"
kct decode "$(./client-credential-grant.sh | jq -r .access_token)"
```
