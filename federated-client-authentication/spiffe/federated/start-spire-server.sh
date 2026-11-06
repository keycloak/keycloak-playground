#!/bin/bash
set -euo pipefail
umask 077

cd "$(dirname "$0")"
mkdir -p .runtime
NETWORK=kc-spiffe-federation
SPIRE_CHART=https://github.com/spiffe/helm-charts-hardened/releases/download/spire-0.24.0/spire-0.24.0.tgz
CRDS_CHART=https://github.com/spiffe/helm-charts-hardened/releases/download/spire-crds-0.5.0/spire-crds-0.5.0.tgz

# Refuse to overwrite an existing demo or change the default kubeconfig.
for domain in east west; do
  if k3d cluster list -o json | jq -e --arg name "kc-spiffe-$domain" 'any(.[]; .name == $name)' >/dev/null; then
    echo "Refusing to reuse existing cluster: kc-spiffe-$domain" >&2
    exit 1
  fi
done
if docker network inspect "$NETWORK" >/dev/null 2>&1; then
  echo "Refusing to reuse existing Docker network: $NETWORK" >&2
  exit 1
fi

echo "------------------------------------------"
echo "Start SPIRE servers and agents in Kubernetes"
echo "------------------------------------------"

docker network create "$NETWORK" >/dev/null
for domain in east west; do
  k3d cluster create "kc-spiffe-$domain" --servers 1 --agents 1 --network "$NETWORK" \
    --kubeconfig-update-default=false --kubeconfig-switch-context=false --wait
  k3d kubeconfig merge "kc-spiffe-$domain" --output "$PWD/.runtime/kc-spiffe-$domain.kubeconfig" \
    --overwrite --kubeconfig-switch-context=false
done
export KUBECONFIG="$PWD/.runtime/kc-spiffe-east.kubeconfig:$PWD/.runtime/kc-spiffe-west.kubeconfig"

# The chart runs one SPIRE server and an agent on each node.
for domain in east west; do
  context="k3d-kc-spiffe-$domain"
  helm upgrade --install spire-crds "$CRDS_CHART" --kube-context "$context" --wait
  helm upgrade --install spire "$SPIRE_CHART" --kube-context "$context" --namespace spire --create-namespace --wait \
    --set global.spire.trustDomain="$domain.example.test" \
    --set global.spire.clusterName="$domain" \
    --set global.spire.recommendations.enabled=false \
    --set spire-server.federation.enabled=true \
    --set spire-server.federation.tls.spire.enabled=true \
    --set spire-server.service.type=NodePort
  kubectl --context "$context" -n spire patch service spire-server --type=json \
    -p='[{"op":"add","path":"/spec/ports/1/nodePort","value":30443}]'
done

echo "-----------------------------------------"
echo "Exchange trust bundles to start federation"
echo "-----------------------------------------"

# Bootstrap each peer once; SPIRE refreshes its bundle over authenticated HTTPS.
for pair in 'east west' 'west east'; do
  read -r domain peer <<< "$pair"
  peer_ip=$(docker inspect --format "{{(index .NetworkSettings.Networks \"$NETWORK\").IPAddress}}" "k3d-kc-spiffe-$peer-server-0")
  peer_bundle=$(kubectl --context "k3d-kc-spiffe-$peer" -n spire exec spire-server-0 -c spire-server -- /opt/spire/bin/spire-server bundle show -format spiffe)
  jq -n --arg peer "$peer" --arg endpoint "https://$peer_ip:30443" --arg bundle "$peer_bundle" '{
    apiVersion: "spire.spiffe.io/v1alpha1",
    kind: "ClusterFederatedTrustDomain",
    metadata: {name: ("demo-" + $peer)},
    spec: {
      className: "spire-spire",
      trustDomain: ($peer + ".example.test"),
      bundleEndpointURL: $endpoint,
      bundleEndpointProfile: {
        type: "https_spiffe",
        endpointSPIFFEID: ("spiffe://" + $peer + ".example.test/spire/server")
      },
      trustDomainBundle: $bundle
    }
  }' | kubectl --context "k3d-kc-spiffe-$domain" apply -f -
done

# Keycloak trusts this local HTTPS endpoint certificate explicitly.
openssl req -x509 -newkey rsa:2048 -nodes -days 2 \
  -keyout .runtime/demo-map.key -out .runtime/demo-map.crt \
  -subj '/CN=localhost' -addext 'subjectAltName=DNS:localhost,IP:127.0.0.1' >/dev/null 2>&1

echo 'SPIRE is ready. Run ./start-bundle-endpoint.sh, then ./start-keycloak.sh.'
