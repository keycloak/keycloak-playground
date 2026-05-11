#!/bin/bash -e

# Evaluate whether Jerry (viewer) can create a todo.
# Expected result: {"decision": false} because Jerry only has the 'viewer' role.

ACCESS_TOKEN=$(./get-access-token.sh)

curl -s -X POST "http://localhost:8080/realms/demo/authzen/access/v1/evaluation" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "subject": {
      "type": "user",
      "id": "jerry"
    },
    "resource": {
      "type": "todo",
      "id": "todo-1"
    },
    "action": {
      "name": "can_create_todo"
    }
  }' | jq
