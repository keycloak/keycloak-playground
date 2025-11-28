#!/bin/bash

SPIFFE_JWT_SVID=$(./fetch-jwt-svid.sh)

curl -s -X POST \
  -d grant_type=client_credentials \
  -d client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-spiffe \
  -d client_assertion="$SPIFFE_JWT_SVID" \
  http://localhost:8080/realms/spiffe/protocol/openid-connect/token