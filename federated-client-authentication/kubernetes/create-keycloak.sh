#!/bin/bash -e

KEYCLOAK_URL=https://keycloak.$(minikube ip).nip.io

echo "---------------------------------------"
echo "Creating single pod Keycloak deployment"
echo "---------------------------------------"

kubectl create -f keycloak.yaml

echo "-----------------------------------------------------------------------------"
echo "Creating ingress for Keycloak to be accessible outside the Kubernetes cluster"
echo "-----------------------------------------------------------------------------"

cat keycloak-ingress.yaml | \
sed "s/KEYCLOAK_HOST/keycloak.$(minikube ip).nip.io/" | \
kubectl create -f -

echo "-----------------------------------------------------------------------------"
echo "Waiting for Keycloak to be available"
echo "-----------------------------------------------------------------------------"

HTTP_CODE=0
while [ "$HTTP_CODE" -ne 302 ]; do
  sleep 2
  HTTP_CODE=$(curl -s --insecure -o /dev/null $KEYCLOAK_URL -w "%{http_code}")
  echo -n "."
done

echo ""
echo ""
echo "Keycloak available on $KEYCLOAK_URL"