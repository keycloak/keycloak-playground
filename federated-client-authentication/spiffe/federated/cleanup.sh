#!/bin/bash
set -euo pipefail

cd "$(dirname "$0")"

# Delete only the resources created by this demo.
for domain in east west; do
  if k3d cluster list -o json | jq -e --arg name "kc-spiffe-$domain" 'any(.[]; .name == $name)' >/dev/null; then
    k3d cluster delete "kc-spiffe-$domain"
  fi
done
if docker network inspect kc-spiffe-federation >/dev/null 2>&1; then
  docker network rm kc-spiffe-federation
fi
rm -rf .runtime
