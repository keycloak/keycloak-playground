#!/bin/bash
set -euo pipefail
umask 077

# Read only the local SPIRE server; SPIRE handles federation with other domains.
spire() {
  .runtime/spire/bin/spire-server "$@" -socketPath "$PWD/.runtime/east/server.sock"
}


bundle_map() {
  local map domains domain bundle
  bundle=$(spire bundle show -format spiffe | jq -ce 'select(type == "object" and (.keys | type == "array"))') || return
  map=$(jq -cn --argjson bundle "$bundle" '{trust_domains: {"east.example.test": $bundle}}')
  domains=$(spire bundle list -output json | jq -r '
    .bundles | if type != "array" then error("Invalid bundle list") else . end |
    map(.trustDomain // .trust_domain) | unique[] |
    if type == "string" and length > 0 then . else error("Invalid trust domain") end') || return
  while IFS= read -r domain; do
    case "$domain" in ""|east.example.test) continue ;; esac
    bundle=$(spire bundle list -id "spiffe://$domain" -format spiffe | jq -ce 'select(type == "object" and (.keys | type == "array"))') || return
    map=$(jq -c --arg domain "$domain" --argjson bundle "$bundle" '.trust_domains[$domain] = $bundle' <<< "$map")
  done <<< "$domains"
  printf '%s\n' "$map"
}

# Sourcing this file lets the test replace the SPIRE command with fixtures.
if [ "${BASH_SOURCE[0]}" != "$0" ]; then
  return
fi

cd "$(dirname "$0")"
RUNTIME="$PWD/.runtime"
mkdir -p "$RUNTIME/public"
rm -f "$RUNTIME/public/bundle" "$RUNTIME/last-refresh"

refresh() {
  while true; do
    temporary=$(mktemp "$RUNTIME/public/.bundle.XXXXXX")
    if bundle_map > "$temporary"; then
      mv "$temporary" "$RUNTIME/public/bundle"
      date +%s > "$RUNTIME/last-refresh.new"
      mv "$RUNTIME/last-refresh.new" "$RUNTIME/last-refresh"
    else
      rm -f "$temporary" "$RUNTIME/public/bundle"
    fi
    sleep 2
  done
}

# Stop serving cached keys even if a SPIRE request hangs.
expire() {
  while sleep 2; do
    if [ -f "$RUNTIME/last-refresh" ] && (( $(date +%s) - $(cat "$RUNTIME/last-refresh") > 15 )); then
      rm -f "$RUNTIME/public/bundle"
    fi
  done
}

refresh &
refresh_pid=$!
expire &
expire_pid=$!
server_pid=
trap 'kill "$refresh_pid" "$expire_pid" "$server_pid" 2>/dev/null || true; wait "$refresh_pid" "$expire_pid" "$server_pid" 2>/dev/null || true' EXIT
trap 'exit 0' INT TERM

echo "---------------------------------------"
echo "Serve the SPIFFE bundle map over HTTPS"
echo "---------------------------------------"

# Serve only public bundles, never certificates, private keys.
cd "$RUNTIME/public"
openssl s_server -quiet -WWW -accept 127.0.0.1:9443 \
  -cert "$RUNTIME/demo-map.crt" -key "$RUNTIME/demo-map.key" &
server_pid=$!
wait "$server_pid"
