#!/bin/bash -e

# Evaluate whether Jerry can read todos, looking up Jerry by his Keycloak UUID.
# Demonstrates the UUID-based subject lookup strategy.
# Expected result: {"decision": true} because Jerry has the 'viewer' role.

cd ~/kc/bin
JERRY_ID=$(./kcadm.sh get users -r demo -q username=jerry -q exact=true --fields id --format csv --noquotes)
cd - > /dev/null

ACCESS_TOKEN=$(./get-access-token.sh)

curl -s -X POST "http://localhost:8080/realms/demo/authzen/access/v1/evaluation" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "subject": {
      "type": "user",
      "id": "'"$JERRY_ID"'"
    },
    "resource": {
      "type": "todo",
      "id": "todo-1"
    },
    "action": {
      "name": "can_read_todos"
    }
  }' | jq
