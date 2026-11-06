#!/bin/bash
set -euo pipefail
umask 077

cd "$(dirname "$0")"
export KUBECONFIG="$PWD/.runtime/kc-spiffe-east.kubeconfig:$PWD/.runtime/kc-spiffe-west.kubeconfig"
domain=${1:?usage: fetch-jwt-svid.sh east|west [subject]}
subject=${2:-$domain}
case "$domain" in east|west) ;; *) echo 'domain must be east or west' >&2; exit 2 ;; esac
case "$subject" in ''|*[!a-z0-9-]*) echo 'subject must contain only lowercase letters, digits and hyphens' >&2; exit 2 ;; esac
context="k3d-kc-spiffe-$domain"
node="k3d-kc-spiffe-$domain-agent-0"
spiffe_id="spiffe://$domain.example.test/client/$subject"

server_exec() {
  kubectl --context "$context" -n spire exec spire-server-0 -c spire-server -- /opt/spire/bin/spire-server "$@"
}

# Register the workload against the SPIRE agent on its node.
parent_id=$(server_exec agent list -output json | jq -er --arg node "$node" '
  first(.agents[] | select(any(.selectors[]; .type == "k8s_psat" and .value == ("agent_node_name:" + $node)))) |
  "spiffe://" + .id.trust_domain + .id.path')
entry_id="demo-$domain-$subject"
if ! server_exec entry show -entryID "$entry_id" >/dev/null 2>&1; then
  server_exec entry create -entryID "$entry_id" -parentID "$parent_id" -spiffeID "$spiffe_id" \
    -selector k8s:ns:default -selector "k8s:sa:demo-$domain-client" -selector "k8s:pod-label:app:$domain-client" -jwtSVIDTTL 300 >/dev/null
fi

# Fetch the JWT-SVID inside the workload using the SPIFFE CSI socket.
job="svid-$domain-$subject"
kubectl --context "$context" -n default delete job "$job" --ignore-not-found >/dev/null
kubectl --context "$context" -n default create serviceaccount "demo-$domain-client" --dry-run=client -o yaml | kubectl --context "$context" apply -f - >/dev/null
kubectl --context "$context" apply -f - <<MANIFEST >/dev/null
apiVersion: batch/v1
kind: Job
metadata:
  name: $job
  namespace: default
spec:
  backoffLimit: 2
  template:
    metadata:
      labels:
        app: $domain-client
    spec:
      nodeName: $node
      serviceAccountName: demo-$domain-client
      restartPolicy: Never
      containers:
        - name: fetch
          image: ghcr.io/spiffe/spire-agent:1.11.0
          imagePullPolicy: IfNotPresent
          command: ["/opt/spire/bin/spire-agent"]
          args: ["api", "fetch", "jwt", "-output", "json", "-audience", "http://localhost:8180/realms/spiffe", "-spiffeID", "$spiffe_id", "-socketPath", "/spiffe-workload-api/spire-agent.sock"]
          volumeMounts:
            - name: workload-api
              mountPath: /spiffe-workload-api
      volumes:
        - name: workload-api
          csi:
            driver: csi.spiffe.io
            readOnly: true
MANIFEST
kubectl --context "$context" -n default wait --for=condition=complete "job/$job" --timeout=90s >/dev/null
pod=$(kubectl --context "$context" -n default get pods -l "job-name=$job" -o json | jq -er 'first(.items[] | select(.status.phase == "Succeeded")) | .metadata.name')
kubectl --context "$context" -n default logs "$pod" | jq -er '.[0].svids[0].svid'
