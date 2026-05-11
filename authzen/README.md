# Keycloak AuthZEN Demo

This demo shows how Keycloak can act as an [AuthZEN](https://openid.net/specs/openid-authzen-authorization-api-1_0.html) Policy Decision Point (PDP), exposing its authorization capabilities through the AuthZEN Evaluation and Evaluations API.

The demo sets up a simple todo application scenario using role-based access control. Two users are created: Rick (admin) who can read and create todos, and Jerry (viewer) who can only read them. The AuthZEN API is then used to evaluate authorization decisions for these users.

## Prerequisites

* Linux
* OpenJDK 17 or 21
* [jq](https://jqlang.org/)

## Starting Keycloak

Run the `start-keycloak.sh` script to download and start Keycloak with the `authzen` experimental feature enabled:

```shell
./start-keycloak.sh
```

This downloads the `kcw` helper script, which fetches a nightly Keycloak distribution and starts it in dev mode with the `authzen` feature flag.

## Configuring Keycloak

In a new terminal, run `configure-keycloak.sh` to set up the realm, users, client, and authorization policies:

```shell
./configure-keycloak.sh
```

This script creates:
- A `demo` realm with two roles: `admin` and `viewer`
- User `rick` with the `admin` role
- User `jerry` with the `viewer` role
- An `authzen-pdp` client with authorization services enabled
- Two authorization scopes: `can_read_todos` and `can_create_todo`
- A `todo-1` resource associated with both scopes
- Role-based policies for `admin` and `viewer`
- A `read-todos-permission` that allows both admins and viewers to read todos
- A `create-todo-permission` that allows only admins to create todos

## PDP Metadata Discovery

Keycloak exposes AuthZEN PDP metadata via a `.well-known` endpoint. PEPs can use this to dynamically discover the available authorization endpoints without hardcoding URLs:

```shell
curl -s "http://localhost:8080/realms/demo/.well-known/authzen-configuration" | jq
```

Expected response:

```json
{
  "policy_decision_point": "http://localhost:8080/realms/demo",
  "access_evaluation_endpoint": "http://localhost:8080/realms/demo/authzen/access/v1/evaluation",
  "access_evaluations_endpoint": "http://localhost:8080/realms/demo/authzen/access/v1/evaluations"
}
```

## Individual Requests with the Evaluation API

The Evaluation API handles single authorization requests. You provide a subject, resource, and action, and Keycloak returns a `{"decision": true}` or `{"decision": false}` response.

### Rick can create todos (admin)

Rick has the `admin` role, so he should be allowed to create todos:

```shell
./evaluate-rick-create-todo.sh
```

Expected response:

```json
{
  "decision": true
}
```

### Jerry cannot create todos (viewer)

Jerry only has the `viewer` role, so creating a todo should be denied:

```shell
./evaluate-jerry-create-todo.sh
```

Expected response:

```json
{
  "decision": false
}
```

### Subject Lookup Strategies

Keycloak supports two `subject.type` values:

* `client` -- Evaluate whether the client is authorized to perform the specified action on a resource
* `user` -- Evaluate whether the specified user is authorized to perform the specified action on a resource

When `subject.type` is `user`, the `subject.id` value is treated as a UUID if it matches the UUID regex, otherwise the user is looked up by username. You can also use explicit namespace prefixes to control how the user is resolved:

* `username:<username>` -- lookup via username
* `id:<uuid>` -- lookup by UUID
* `email:<email>` -- lookup by email address

**Note:** Email-based lookups will return a `400 Bad Request` if the realm has duplicate emails enabled, as the lookup cannot guarantee a unique result.

#### Lookup by UUID

This script retrieves Jerry's Keycloak UUID via the Admin API and uses it as the `subject.id`:

```shell
./evaluate-jerry-by-uuid.sh
```

Expected response:

```json
{
  "decision": true
}
```

#### Lookup by email

This script uses the `email:` prefix to look up Jerry by his email address:

```shell
./evaluate-jerry-by-email.sh
```

Expected response:

```json
{
  "decision": true
}
```

## Batch Requests with the Evaluations API

The Evaluations API allows you to batch multiple authorization checks into a single request, reducing the number of round-trips to the PDP.

### Batch evaluation for Rick (admin)

Check whether Rick can both read and create todos:

```shell
./evaluations-rick.sh
```

Expected response (both allowed):

```json
{
  "evaluations": [
    {
      "decision": true
    },
    {
      "decision": true
    }
  ]
}
```

### Batch evaluation for Jerry (viewer)

Check whether Jerry can both read and create todos:

```shell
./evaluations-jerry.sh
```

Expected response (read allowed, create denied):

```json
{
  "evaluations": [
    {
      "decision": true
    },
    {
      "decision": false
    }
  ]
}
```
f