#!/bin/bash
set -euo pipefail
umask 077

cd "$(dirname "$0")"
domain=${1:?usage: start-spire-agent.sh east|west}
case "$domain" in east) port=8081 ;; west) port=8082 ;; *) echo 'domain must be east or west' >&2; exit 2 ;; esac
DIR="$PWD/.runtime/$domain"
rm -rf "$DIR/agent-data" "$DIR/agent-keys"
mkdir -p "$DIR/agent-keys"
SPIRE="$PWD/.runtime/spire/bin"

# As in the simple demo, the agent joins with a one-time token.
"$SPIRE/spire-server" bundle show -socketPath "$DIR/server.sock" -format pem > "$DIR/bundle.pem"
token=$("$SPIRE/spire-server" token generate -socketPath "$DIR/server.sock" | awk '/^Token:/ {print $2}')
parent_id="spiffe://$domain.example.test/spire/agent/join_token/$token"
printf "%s\n" "$parent_id" > "$DIR/agent-id"

"$SPIRE/spire-server" entry create -socketPath "$DIR/server.sock" \
  -parentID "$parent_id" \
  -spiffeID "spiffe://$domain.example.test/client/$domain" -selector "unix:uid:$(id -u)"

cat > "$DIR/agent.conf" <<CONFIG
agent {
  trust_domain = "$domain.example.test"
  trust_bundle_path = "$DIR/bundle.pem"
  data_dir = "$DIR/agent-data"
  server_address = "127.0.0.1"
  server_port = "$port"
  socket_path = "$DIR/agent.sock"
}
plugins {
  NodeAttestor "join_token" { plugin_data {} }
  KeyManager "disk" {
    plugin_data { directory = "$DIR/agent-keys" }
  }
  WorkloadAttestor "unix" { plugin_data {} }
}
CONFIG

exec "$SPIRE/spire-agent" run -config "$DIR/agent.conf" -joinToken "$token"
