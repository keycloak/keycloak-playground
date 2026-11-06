#!/bin/bash -e

cd tmp/spire

JOIN_TOKEN=$(bin/spire-server token generate -spiffeID spiffe://example.org/myclient | cut -d ' ' -f 2)

bin/spire-server entry create -parentID spiffe://example.org/myclient -spiffeID spiffe://example.org/myclient -selector unix:uid:$(id -u)

bin/spire-agent run -config conf/agent/agent.conf -joinToken $JOIN_TOKEN

