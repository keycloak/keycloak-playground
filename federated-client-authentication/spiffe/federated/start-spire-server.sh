#!/bin/bash
set -euo pipefail
umask 077

cd "$(dirname "$0")"
domain=${1:?usage: start-spire-server.sh east|west}
case "$domain" in
  east) port=8081; federation_port=8543 ;;
  west) port=8082; federation_port=8544 ;;
  *) echo 'domain must be east or west' >&2; exit 2 ;;
esac

SPIRE_VERSION=1.13.3
RUNTIME="$PWD/.runtime"
mkdir -p "$RUNTIME"
if [ ! -d "$RUNTIME/spire" ]; then
  curl -fLsS "https://github.com/spiffe/spire/releases/download/v$SPIRE_VERSION/spire-$SPIRE_VERSION-linux-amd64-musl.tar.gz" -o "$RUNTIME/spire.tar.gz"
  tar -xzf "$RUNTIME/spire.tar.gz" -C "$RUNTIME"
  mv "$RUNTIME/spire-$SPIRE_VERSION" "$RUNTIME/spire"
  rm "$RUNTIME/spire.tar.gz"
fi

DIR="$RUNTIME/$domain"
mkdir -p "$DIR"
cat > "$DIR/server.conf" <<CONFIG
server {
  bind_address = "127.0.0.1"
  bind_port = "$port"
  trust_domain = "$domain.example.test"
  data_dir = "$DIR/server-data"
  socket_path = "$DIR/server.sock"
  federation {
    bundle_endpoint {
      address = "127.0.0.1"
      port = $federation_port
      profile "https_spiffe" {}
    }
  }
}
plugins {
  DataStore "sql" {
    plugin_data {
      database_type = "sqlite3"
      connection_string = "$DIR/server-data/datastore.sqlite3"
    }
  }
  KeyManager "disk" {
    plugin_data {
      keys_path = "$DIR/server-data/keys.json"
    }
  }
  NodeAttestor "join_token" { plugin_data {} }
}
CONFIG

# Each trust domain has its own server, socket and federation endpoint.
exec "$RUNTIME/spire/bin/spire-server" run -config "$DIR/server.conf"
