#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")"
KC_DIST=${KC_DIST:-"$PWD/../../../../keycloak/quarkus/dist/target/keycloak-999.0.0-SNAPSHOT.tar.gz"}
KC_HOME="$PWD/.runtime/keycloak-999.0.0-SNAPSHOT"
SPIFFE_BUNDLE_CERT="$PWD/.runtime/demo-map.crt"

# Replace the generated installation so old snapshot dependencies cannot survive.
rm -rf "$KC_HOME"
mkdir -p .runtime
tar -xzf "$KC_DIST" -C .runtime

exec "$KC_HOME/bin/kc.sh" start-dev --db=dev-mem --features=client-auth-federated,spiffe \
  --bootstrap-admin-username=admin --bootstrap-admin-password=admin \
  --http-port=8180 --truststore-paths="$SPIFFE_BUNDLE_CERT"
