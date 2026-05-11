#!/bin/bash -e

mkdir -p tmp
cd tmp

curl -s -O https://raw.githubusercontent.com/keycloak/keycloak/refs/heads/main/misc/scripts/kcw && chmod +x kcw

./kcw nightly start-dev --features=authzen
