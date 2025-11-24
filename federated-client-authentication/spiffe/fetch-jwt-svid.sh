#!/bin/bash -e

cd tmp/spire

bin/spire-agent api fetch jwt -output json -audience http://localhost:8080/realms/spiffe | jq  -r .[0].svids.[0].svid