#!/bin/bash -e

echo "-----------------------"
echo "Login kcadm to Keycloak"
echo "-----------------------"

KC_POD=$(kubectl get pods | grep keycloak | cut -f 1 -d ' ')
KCADMIN="kubectl exec $KC_POD -- /opt/keycloak/bin/kcadm.sh"

$KCADMIN config credentials --server http://localhost:8080 --realm master --user admin --password admin

echo "----------------------------"
echo "Create demo kubernetes realm"
echo "----------------------------"

$KCADMIN create realms -s realm=kubernetes -s enabled=true

echo "------------------------------------------"
echo "Create Kubernetes Identity Provider config"
echo "------------------------------------------"

$KCADMIN create identity-provider/instances -r kubernetes -s alias=kubernetes -s providerId=kubernetes -s config='{"issuer": "https://kubernetes.default.svc.cluster.local"}'

echo "------------------------------------------------------------"
echo "Create client authenticating with Kubernetes service account"
echo "------------------------------------------------------------"

$KCADMIN create clients -r kubernetes -s clientId=myclient -s serviceAccountsEnabled=true -s clientAuthenticatorType=federated-jwt -s attributes='{ "jwt.credential.issuer": "kubernetes", "jwt.credential.sub": "system:serviceaccount:default:my-serviceaccount" }'