#!/bin/bash -e

cd ~/kc/bin

# Authenticate with the Keycloak admin CLI
echo "----------------------------"
echo "Login kcadm to Keycloak"
echo "----------------------------"

./kcadm.sh config credentials --server http://localhost:8080 --realm master --user admin --password admin

# Create a dedicated realm for the AuthZEN demo
echo "----------------------------"
echo "Create demo realm"
echo "----------------------------"

./kcadm.sh create realms -s realm=demo -s enabled=true

# Create realm roles: 'admin' for full access and 'viewer' for read-only access
echo "----------------------------"
echo "Create realm roles"
echo "----------------------------"

./kcadm.sh create roles -r demo -s name=admin
./kcadm.sh create roles -r demo -s name=viewer

# Create user 'rick' with the 'admin' role
echo "----------------------------"
echo "Create user rick (admin)"
echo "----------------------------"

./kcadm.sh create users -r demo -s username=rick -s enabled=true -s firstName=Rick -s lastName=Sanchez -s email=rick@example.org
./kcadm.sh set-password -r demo --username rick --new-password password
./kcadm.sh add-roles -r demo --uusername rick --rolename admin

# Create user 'jerry' with the 'viewer' role
echo "----------------------------"
echo "Create user jerry (viewer)"
echo "----------------------------"

./kcadm.sh create users -r demo -s username=jerry -s enabled=true -s firstName=Jerry -s lastName=Smith -s email=jerry@example.org
./kcadm.sh set-password -r demo --username jerry --new-password password
./kcadm.sh add-roles -r demo --uusername jerry --rolename viewer

# Create the 'authzen-pdp' client with authorization services enabled.
# This client acts as the Policy Decision Point (PDP) and will evaluate authorization requests.
echo "-------------------------------------------"
echo "Create authzen-pdp client"
echo "-------------------------------------------"

CLIENT_ID=$(./kcadm.sh create clients -r demo \
  -s clientId=authzen-pdp \
  -s enabled=true \
  -s secret=secret \
  -s serviceAccountsEnabled=true \
  -s directAccessGrantsEnabled=false \
  -s authorizationServicesEnabled=true \
  -i)

# Create authorization scopes that map to the actions in our todo application
echo "-------------------------------------------"
echo "Create authorization scopes"
echo "-------------------------------------------"

./kcadm.sh create "clients/$CLIENT_ID/authz/resource-server/scope" -r demo -s name=can_read_todos
./kcadm.sh create "clients/$CLIENT_ID/authz/resource-server/scope" -r demo -s name=can_create_todo

# Create a resource representing a todo item, associated with both scopes
echo "-------------------------------------------"
echo "Create todo resource"
echo "-------------------------------------------"

./kcadm.sh create "clients/$CLIENT_ID/authz/resource-server/resource" -r demo \
  -s name=todo-1 \
  -s type=todo \
  -s 'scopes=[{"name":"can_read_todos"},{"name":"can_create_todo"}]'

# Create a role-based policy that grants access to users with the 'admin' role
echo "-------------------------------------------"
echo "Create admin role policy"
echo "-------------------------------------------"

./kcadm.sh create "clients/$CLIENT_ID/authz/resource-server/policy/role" -r demo \
  -s name=admin-policy \
  -s 'roles=[{"id":"admin"}]'

# Create a role-based policy that grants access to users with the 'viewer' role
echo "-------------------------------------------"
echo "Create viewer role policy"
echo "-------------------------------------------"

./kcadm.sh create "clients/$CLIENT_ID/authz/resource-server/policy/role" -r demo \
  -s name=viewer-policy \
  -s 'roles=[{"id":"viewer"}]'

# Create a scope-based permission that allows both admins and viewers to read todos.
# Uses AFFIRMATIVE decision strategy: access is granted if any associated policy permits it.
echo "-------------------------------------------"
echo "Create read-todos permission"
echo "-------------------------------------------"

./kcadm.sh create "clients/$CLIENT_ID/authz/resource-server/permission/scope" -r demo \
  -s name=read-todos-permission \
  -s decisionStrategy=AFFIRMATIVE \
  -s 'resources=["todo-1"]' \
  -s 'scopes=["can_read_todos"]' \
  -s 'policies=["admin-policy","viewer-policy"]'

# Create a scope-based permission that allows only admins to create todos
echo "-------------------------------------------"
echo "Create create-todo permission"
echo "-------------------------------------------"

./kcadm.sh create "clients/$CLIENT_ID/authz/resource-server/permission/scope" -r demo \
  -s name=create-todo-permission \
  -s 'resources=["todo-1"]' \
  -s 'scopes=["can_create_todo"]' \
  -s 'policies=["admin-policy"]'

echo ""
echo "Realm 'demo' configured successfully."
echo "  Users: rick (admin), jerry (viewer)"
echo "  Client: authzen-pdp with authorization scopes can_read_todos and can_create_todo"
