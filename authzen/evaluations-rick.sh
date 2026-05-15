#!/bin/bash -e

# Batch evaluate whether Rick (admin) can read and create todos in a single request.
# Uses the Evaluations API to reduce round-trips to the PDP.
# Expected result: both decisions are true because Rick has the 'admin' role.

ACCESS_TOKEN=$(./get-access-token.sh)

curl -s -X POST "http://localhost:8080/realms/demo/authzen/access/v1/evaluations" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "subject": {
      "type": "user",
      "id": "username:rick"
    },
    "evaluations": [
      {
        "resource": {
          "type": "todo",
          "id": "todo-1"
        },
        "action": {
          "name": "can_read_todos"
        }
      },
      {
        "resource": {
          "type": "todo",
          "id": "todo-1"
        },
        "action": {
          "name": "can_create_todo"
        }
      }
    ]
  }' | jq
