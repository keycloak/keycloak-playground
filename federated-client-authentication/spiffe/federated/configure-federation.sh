#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")"
RUNTIME="$PWD/.runtime"
SPIRE="$RUNTIME/spire/bin/spire-server"

# Bootstrap each peer from its local trust bundle; SPIRE then refreshes it.
for pair in 'east west 8544' 'west east 8543'; do
  read -r domain peer port <<< "$pair"
  "$SPIRE" bundle show -socketPath "$RUNTIME/$peer/server.sock" -format spiffe > "$RUNTIME/$peer/bundle.json"
  "$SPIRE" federation create -socketPath "$RUNTIME/$domain/server.sock" \
    -trustDomain "$peer.example.test" \
    -bundleEndpointURL "https://127.0.0.1:$port" \
    -bundleEndpointProfile https_spiffe \
    -endpointSpiffeID "spiffe://$peer.example.test/spire/server" \
    -trustDomainBundlePath "$RUNTIME/$peer/bundle.json" -trustDomainBundleFormat spiffe
done

openssl req -x509 -newkey rsa:2048 -nodes -days 2 \
  -keyout "$RUNTIME/demo-map.key" -out "$RUNTIME/demo-map.crt" \
  -subj '/CN=localhost' -addext 'subjectAltName=DNS:localhost,IP:127.0.0.1' >/dev/null 2>&1
