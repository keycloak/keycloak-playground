#!/bin/bash -e

KEYCLOAK_URL=https://keycloak.$(minikube ip).nip.io
TOKEN=$(kubectl exec my-pod -- cat /var/run/secrets/tokens/kctoken)

curl --insecure -X POST \
  -d grant_type=client_credentials \
  -d client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-bearer \
  -d client_assertion="$TOKEN" \
  $KEYCLOAK_URL/realms/kubernetes/protocol/openid-connect/token
