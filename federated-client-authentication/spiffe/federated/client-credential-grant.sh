#!/bin/bash
set -euo pipefail
umask 077

cd "$(dirname "$0")"
domain=${1:?usage: client-credential-grant.sh east|west [subject]}

# Keep the JWT-SVID out of curl's process arguments.
mkdir -p .runtime
SPIFFE_JWT_SVID=$(mktemp .runtime/assertion.XXXXXX)
trap 'rm -f "$SPIFFE_JWT_SVID"' EXIT
./fetch-jwt-svid.sh "$domain" "${2:-$domain}" | tr -d '\r\n' > "$SPIFFE_JWT_SVID"

curl --silent --show-error --fail-with-body -X POST \
  -d grant_type=client_credentials \
  -d client_id="$domain-client" \
  -d client_assertion_type=urn:ietf:params:oauth:client-assertion-type:jwt-spiffe \
  --data-urlencode "client_assertion@$SPIFFE_JWT_SVID" \
  http://localhost:8180/realms/spiffe/protocol/openid-connect/token
