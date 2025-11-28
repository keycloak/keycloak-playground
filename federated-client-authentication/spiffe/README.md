# SPIFFE and Keycloak demo

This demo shows how to leverage SPIFFE SVIDs to authenticate clients with Keycloak.

## Prerequisites

* Linux
* OpenJDK 17 or 21

## Starting the Spire Server and Agent

Install and start SPIRE Server:
```shell
./start-spire-server.sh
```

Start the SPIRE Agent:
```shell
./start-spire-agent.sh
```

Fetch a JWT SVID to verify things are working so far:
```shell
./fetch-jwt-svid.sh
```

Verify the bundle endpoint is working with:
```shell
curl --insecure https://localhost:8543
```

## Running Keycloak

You need to run Keycloak with the client-auth-federated and spiffe features enabled. You also need to disable the 
trust manager as currently the SPIFFE servers certificate is not been added to the Keycloak truststore.

To run Keycloak with the required configuration run the script `start-keycloak.sh`. This will download and start
Keycloak with the required configuration.

## Configuring Keycloak

The demo requires setting up a realm in Keycloak, with a Kubernetes identity provider, and a client configured to
use Kubernetes service accounts for authentication.

This can be created by running `configure-keycloak.sh`.

## Try it out

To verify the client can now authenticate to Keycloak with the SPIFFE JWT SVID we'll do a client credential grant:
```shell
./client-credential-grant.sh
```

The output should be an access token response including an access token.

## `kct`

[kct](https://github.com/stianst/keycloak-tokens-cli) can be used to decode the SPIFFE JWT SVID and the Access Token
returned from Keycloak:

To decode the SPIFFE JWT SVID run:
```
kct decode $(./fetch-jwt-svid.sh)
```

To decode the access token run:
```
kct decode $(./client-credential-grant.sh | jq .access_token)
```