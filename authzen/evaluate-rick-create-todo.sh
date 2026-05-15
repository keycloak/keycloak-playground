#!/bin/bash -e

# Evaluate whether Rick (admin) can create a todo.
# Expected result: {"decision": true} because Rick has the 'admin' role.

ACCESS_TOKEN=$(./get-access-token.sh)

curl -s -X POST "http://localhost:8080/realms/demo/authzen/access/v1/evaluation" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "subject": {
      "type": "user",
      "id": "rick"
    },
    "resource": {
      "type": "todo",
      "id": "todo-1"
    },
    "action": {
      "name": "can_create_todo"
    }
  }' | jq
