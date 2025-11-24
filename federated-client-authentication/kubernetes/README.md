# Client Authentication with Kubernetes Service Accounts demo

## Prerequisites

* Linux
* Minikube with `ingress` addon enabled

## Deploying Keycloak

To deploy Keycloak run `create-keycloak.sh`. This script will deploy a very basic instance of Keycloak with an
ingress so Keycloak can be accessed outside the cluster.

## Configuring Keycloak

The demo requires setting up a realm in Keycloak, with a Kubernetes identity provider, and a client configured to
use Kubernetes service accounts for authentication.

This can be created by running `configure-keycloak.sh`.

## Create a pod with a service account

For a client to authenticate via Kubernetes service accounts it needs a service account token with an audience
matching the Keycloak realm issuer URL. This can be issued by Kubernetes using Token Volume Projection.

The `mypod.yaml` template shows a very simple pod where such a token is mounted at `/var/run/secrets/tokens/kctoken`.

To deploy this run the script `create-mypod.sh`.

## Try it out

The simplest way to try out if a client can authenticate with a Kubernetes service account is to do a client credential
grant, as this flow does not require a user to authenticate and can be done as a simple rest call.

Run the script `client-credential-grant.sh` this will grab the service account token and do a client credential grant
request to Keycloak.