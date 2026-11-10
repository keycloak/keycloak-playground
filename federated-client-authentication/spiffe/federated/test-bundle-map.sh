#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")"
source ./start-bundle-endpoint.sh

# Replace SPIRE with fixtures, preserving its command and JSON formats.
spire() {
  case "$*" in
    'bundle show -format spiffe') printf '%s\n' "$local_bundle" ;;
    'bundle list -output json') printf '%s\n' "$remote_domains" ;;
    'bundle list -id spiffe://west.example.test -format spiffe') echo '{"keys":[{"kid":"west"}]}' ;;
    'bundle list -id spiffe://third.example.test -format spiffe') echo '{"keys":[{"kid":"third"}]}' ;;
    *) return 1 ;;
  esac
}

local_bundle='{"keys":[{"kid":"east"}]}'
remote_domains='{"bundles":[{"trust_domain":"west.example.test"},{"trustDomain":"third.example.test"}]}'
bundle_map | jq -e '.trust_domains | keys == ["east.example.test", "third.example.test", "west.example.test"]' >/dev/null
bundle_map | jq -e '.trust_domains["west.example.test"].keys[0].kid == "west"' >/dev/null

# A withdrawn domain disappears rather than surviving in the previous map.
remote_domains='{"bundles":[]}'
bundle_map | jq -e '.trust_domains | keys == ["east.example.test"]' >/dev/null

local_bundle='{"keys":"invalid"}'
if bundle_map >/dev/null; then
  echo 'Malformed bundle was accepted' >&2
  exit 1
fi
local_bundle='{"keys":[]}'
remote_domains='{}'
if bundle_map >/dev/null 2>&1; then
  echo 'Malformed bundle list was accepted' >&2
  exit 1
fi

echo 'Bundle map checks passed'
