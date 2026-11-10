#!/bin/bash
set -euo pipefail
umask 077

cd "$(dirname "$0")"
KCADM="$PWD/.runtime/keycloak-999.0.0-SNAPSHOT/bin/kcadm.sh"
KCADM_CONFIG="$PWD/.runtime/kcadm.config"

echo "-----------------------"
echo "Login kcadm to Keycloak"
echo "-----------------------"

"$KCADM" config credentials --config "$KCADM_CONFIG" --server http://localhost:8180 --realm master --user admin --password admin

echo "------------------------"
echo "Create demo SPIFFE realm"
echo "------------------------"

"$KCADM" create realms --config "$KCADM_CONFIG" -s realm=spiffe -s enabled=true

for domain in east west; do
  echo "----------------------------------------"
  echo "Create $domain SPIFFE identity provider"
  echo "----------------------------------------"

  "$KCADM" create identity-provider/instances --config "$KCADM_CONFIG" -r spiffe \
    -s "alias=$domain" -s providerId=spiffe \
    -s "config={\"trustDomain\":\"spiffe://$domain.example.test\",\"bundleEndpoint\":\"https://localhost:9443/bundle\"}"

  echo "-------------------------------------------"
  echo "Create $domain client authenticating with SPIFFE"
  echo "-------------------------------------------"

  "$KCADM" create clients --config "$KCADM_CONFIG" -r spiffe \
    -s "clientId=$domain-client" -s serviceAccountsEnabled=true \
    -s clientAuthenticatorType=federated-jwt \
    -s "attributes={\"jwt.credential.issuer\":\"$domain\",\"jwt.credential.sub\":\"spiffe://$domain.example.test/client/$domain\"}"
done
