#!/bin/bash -e

# Evaluate whether Jerry can read todos, looking up Jerry by his email address.
# Demonstrates the email-based subject lookup strategy using the 'email:' prefix.
# Expected result: {"decision": true} because Jerry has the 'viewer' role.

ACCESS_TOKEN=$(./get-access-token.sh)

curl -s -X POST "http://localhost:8080/realms/demo/authzen/access/v1/evaluation" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "subject": {
      "type": "user",
      "id": "email:jerry@example.org"
    },
    "resource": {
      "type": "todo",
      "id": "todo-1"
    },
    "action": {
      "name": "can_read_todos"
    }
  }' | jq
