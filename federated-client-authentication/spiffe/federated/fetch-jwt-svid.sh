#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")"
domain=${1:?usage: fetch-jwt-svid.sh east|west [subject]}
case "$domain" in east|west) ;; *) echo 'domain must be east or west' >&2; exit 2 ;; esac
subject=${2:-$domain}
case "$subject" in ''|*[!a-z0-9-]*) echo 'subject must contain only lowercase letters, digits and hyphens' >&2; exit 2 ;; esac

SPIRE="$PWD/.runtime/spire/bin"
DIR="$PWD/.runtime/$domain"
spiffe_id="spiffe://$domain.example.test/client/$subject"

if [ "$subject" != "$domain" ]; then
  "$SPIRE/spire-server" entry create -socketPath "$DIR/server.sock" \
    -parentID "$(cat "$DIR/agent-id")" \
    -spiffeID "$spiffe_id" -selector "unix:uid:$(id -u)" >/dev/null
fi

"$SPIRE/spire-agent" api fetch jwt -output json -socketPath "$DIR/agent.sock" \
  -audience http://localhost:8180/realms/spiffe -spiffeID "$spiffe_id" | jq -er '.[0].svids[0].svid'
