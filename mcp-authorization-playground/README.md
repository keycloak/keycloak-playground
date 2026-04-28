# MCP Authorization Playground

This playground shows how keycloak supports some of features recommended by MCP but are not supported by the current keycloak. It also includes the example providers for that.

## Audience

Keycloak users who want to use keycloak as an authorization server in MCP.

## Background

There are some features that MCP does not mandate but recommend to support by an authorization server. However, the current keycloak does not support some of them:

- [Token Audience Binding](https://modelcontextprotocol.io/specification/2025-11-25/basic/authorization#token-audience-binding-and-validation) for MCP 2025-06-18 and 2025-11-25 versions
- [OAuth Client ID Metadata Document (Internet Draft)](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-client-id-metadata-document-00) for MCP 2025-11-25 versions

Keycloak community are working on supporting the features to Keycloak. Until the works are completed, we can consider the tentative measures for that without modifying keycloak's source codes, namely with custom providers.

### Token Audience Binding

Regarding the token audience binding, other workaround exists. You can use OAuth 2.0’s `scope` parameter instead of the `resource` parameter. To show the binding, please consider the following situation:

* An MCP server's URL is `https://example.com/mcp`
* The MCP supports the following three scopes: `mcp:tools`, `mcp:prompts` and  `mcp:resources`.
* To get an access token for accessing the MCP server, an MCP client sends to keycloak an authorization request whose `resource` parameter value is `https://example.com/mcp` and `scope` parameter includes any combination of the three scopes.
* We want keycloak to issue an access token whose `aud` claim's value is MCP server's URL, namely `https://example.com/mcp`.

To make keycloak issue such the access token, we could configure keycloak as follows:

* Add a client scope `mcp:tools` whose type is `Optional`.
* Add to the client scope a new `Audience` mapper whose `Included Custom Audience` field is `https://example.com/mcp`.
* Add a client scope `mcp:prompts` whose type is `Optional`.
* Add to the client scope a new `Audience` mapper whose `Included Custom Audience` field is `https://example.com/mcp`.
* Add a client scope `mcp:resources` whose type is `Optional`.
* Add to the client scope a new `Audience` mapper whose `Included Custom Audience` field is `https://example.com/mcp`.

Please not that the client scope's `Included Custom Audience` field needs to be the same as the authorization request's `resource` parameter value and the MCP server's URL.

With the configuration, if the MCP client send to keycloak an authorization request whose `resource` parameter value is `https://example.com/mcp` and `scope` parameter includes `mcp:resources`, `mcp:tools` and `mcp:prompts`, keycloak can issue the following access token:

```json
{
  ...
  "aud": "https://example.com/mcp",
  "scope": "mcp:resources mcp:tools mcp:prompts"
  ...
}
```

However, this workaround have a problem. By this workaround, keycloak cannot treat many MCP servers and these MCP servers have scope parameters of the same names. The reason why is that the client scope only specify one `Included Custom Audience` field and does not change its value depending on any conditions.

For example, the MCP servers whose URLs are `https://erst.example.com/v1/mcp` and `https:/zweit.example.com/v3/mcp` and both support the same scopes `mcp:tools`, `mcp:prompts` and  `mcp:resources`, Keycloak cannot do token audience binding in the same realm.

On the contrary, the tentative measures shown in this playground can deal such the situation.

## Warning

The providers in this playground are not Keycloak project's official ones. You can use the providers as examples of custom providers for the tentative measures described in the playground.

## Token Audience Binding

To gain security benefit, the MCP specification requires an access token to be bound with its audience. In order to do so, the MCP specification requires the following:

* An MCP client MUST include the `resource` parameter defined in [Resource Indicators for OAuth 2.0 (RFC 8707)](https://datatracker.ietf.org/doc/html/rfc8707) in an authorization request and token request. The parameter's value MUST identify an MCP server that the MCP client intends to use the token with.

* An MCP server MUST validate that tokens presented to them were specifically issued for their use.

The MCP specification does not explicitly describe that an authorization server needs to support RFC 8707. But in practice, the authorization server needs to issue an access token that an MCP server can verify its audience, which is called Token Audience Binding.

The MCP specification does not describe how to do this binding. One method for the binding is to set a value of `resource` parameter to an `aud` claim in an access token. However, keycloak cannot recognize `resource` parameter.

### Custom provider

|Class name|Description|
|-|-|
|ResourceAudienceProtocolMapper|It is a custom mapper that sets the value of `resource` parameter to `aud` claim in tokens like an access token.|
|ResourceAudienceCheckExecutor|It is a custom client policy executor that check the value of `resource` parameter in a token request is the same as the one in an authorization server. If not, It returns an error.|
|ResourceAudienceCheckExecutorFactory|It is the factory class for `ResourceAudienceCheckExecutor`.|

To build the provider, run the following command under the root of this project:
```
mvn clean package
```
### Demo

### Environment

The demo uses the following:

|Entity|Description|
|-|-|
|Keycloak 26.4.7|It is the Keycloak including the custom provider.|
|Browser|It sends an authorization request, do user authentication and consent and receive its corresponding authorization response.|
|cURL|It sends a token request and receives its corresponding token response.|

### Scenarios

#### S1: normal case

##### Procedure

1. The browser sends an authorization request including the `resource` parameter and receive its corresponding successful authorization response.
2. The cURL sends an token request including the `resource` parameter whose value is the same as one in the authorization request.

##### Expectation

The cURL receives an access token including the `aud` claim whose value is the same as the value of `resource` parameter in both authorization request and token request.

#### S2: erroneous case

MCP spec says "MCP clients MUST include the `resource` parameter in both authorization requests
and token requests.". If both are different, keycloak would bind an access token with an unintended MCP server. This scenario demonstrates this consistency check.

##### Procedure

1. The browser sends an authorization request including the `resource` parameter and receive its corresponding successful authorization response.
2. The cURL sends an token request including the `resource` parameter whose value is **different** from the one in the authorization request.

##### Expectation

The cURL receives an error response indicating the values are different.

### Setup

For all the scenarios, the following realm setup is needed:

- Realm: mcp-test​
- Client: mcp-client​
  - Configuration: public client, redirect_uri: http://localhost:8080/mcp-client/cb , PKCE: S256​
- Client scope: mcp:tools, mcp:prompts, mcp:resources​
  - Configuration: optional, adding mapper: Resource Audience​
- User: alice​
- Client profile: mcp-profile​
  - executors:​
    - secure-redirect-uris-enforcer:​
      - Configuration: Allow IPv4 loopback address: ON, Allow http scheme: ON (for demonstration)
    - pkce-enforcer:
      - Configuration: Auto-configure: ON​
    - reject-implicit-grant:
      - Configuration: Auto-configure: ON​
    - reject-ropc-grant:
      - Configuration: Auto-configure : ON​
    - resource-audience-checker​
- Client policy: mcp-policy​
  - conditions:​
    - client-scopes:​
      - Configuration: Expected Scopes: mcp:tools, mcp:prompts, mcp:resources, Scope Type: Optional​
  - profiles: mcp-profile

Regarding RFC 7636 PKCE, the following valid `code_challenge` and `code_verifier` are used from [RFC 7636](https://datatracker.ietf.org/doc/html/rfc7636#appendix-B​):

```
code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM​
code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk
```

### S1: normal case with custom mapper

1. The browser sends an authorization request including the `resource` parameter whose value is `https://res.example.com/v2/mcp` and receive its corresponding successful authorization response.

The authorization endpoint is as follows:
- http://localhost:8080/realms/mcp-test/protocol/openid-connect/auth

The MCP server URL is as follows:
- https://res.example.com/v2/mcp

The query parameters of the authorization request are as follows:
- client_id=mcp-client​
- scope=mcp:tools+mcp:resources+mcp:prompts
- redirect_uri=http://localhost:8080/mcp-client/cb
- response_type=code​
- code_challenge_method=S256​
- code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM​
- resource=https://res.example.com/v2/mcp

The authorization request is the following:
```
http://localhost:8080/realms/mcp-test/protocol/openid-connect/auth?client_id=mcp-client&scope=mcp:tools+mcp:resources+mcp:prompts&redirect_uri=http://localhost:8080/mcp-client/cb&response_type=code&code_challenge_method=S256&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&resource=https://res.example.com/v2/mcp
```

The authorization response is the following:
```
http://localhost:8080/mcp-client/cb?session_state=<session state>&iss=http%3A%2F%2Flocalhost%3A8080%2Frealms%2Fmcp-test&code=<authorization code>
```

2. The cURL sends an token request including the `resource` parameter whose value is the same as one in the authorization request.

The token endpoint is as follows:
- http://localhost:8080/realms/mcp-test/protocol/openid-connect/

The MCP server URL is as follows:
- https://res.example.com/v2/mcp

The form parameters of the token request are as follows:
- code=\<authorization code\>​
- grant_type=authorization_code​
- client_id=mcp-client​
- redirect_uri=http://localhost:8080/mcp-client/cb​
- code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk​
- resource=https://res.example.com/v2/mcp

The token request is the following:
```
curl --location --request POST 'http://localhost:8080/realms/mcp-test/protocol/openid-connect/token' \​
--header 'Content-Type: application/x-www-form-urlencoded' \​
--data-urlencode 'code=<authorization code>' \​
--data-urlencode 'grant_type=authorization_code' \​
--data-urlencode 'client_id=mcp-client' \​
--data-urlencode 'redirect_uri=http://localhost:8080/mcp-client/cb' \​
--data-urlencode 'resource=https://res.example.com/v2/mcp' \​
--data-urlencode 'code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk'
```

The part of the decoded token response is as follows:
```
{
  "exp": 1765675960,
  "iat": 1765675660,
  "auth_time": 1765675031,
  "jti": "onrtac:3bda9df8-8cb6-f052-5a90-da9858af9702",
  "iss": "http://localhost:8080/realms/mcp-test",
  "aud": "https://res.example.com/v2/mcp",
  "sub": "2bc7d05d-c1d8-46e4-b61c-ce739461c9c8",
  "typ": "Bearer",
  "azp": "mcp-client",
  "scope": "mcp:resources mcp:tools mcp:prompts",
  ...
```

As we expected, the access token include the `aud` claim whose value is the MCP server URL, namely `https://res.example.com/v2/mcp`.

### S2: erroneous case

1. The browser sends an authorization request including the `resource` parameter whose value is `https://res.example.com/v2/mcp` and receive its corresponding successful authorization response.

The authorization endpoint is as follows:
- http://localhost:8080/realms/mcp-test/protocol/openid-connect/auth

The MCP server URL is as follows:
- https://res.example.com/v2/mcp

The query parameters of the authorization request are as follows:
- client_id=mcp-client​
- scope=mcp:tools+mcp:resources+mcp:prompts
- redirect_uri=http://localhost:8080/mcp-client/cb
- response_type=code​
- code_challenge_method=S256​
- code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM​
- resource=https://res.example.com/v2/mcp

The authorization request is the following:
```
http://localhost:8080/realms/mcp-test/protocol/openid-connect/auth?client_id=mcp-client&scope=mcp:tools+mcp:resources+mcp:prompts&redirect_uri=http://localhost:8080/mcp-client/cb&response_type=code&code_challenge_method=S256&code_challenge=E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM&resource=https://res.example.com/v2/mcp
```

The authorization response is the following:
```
http://localhost:8080/mcp-client/cb?session_state=<session state>&iss=http%3A%2F%2Flocalhost%3A8080%2Frealms%2Fmcp-test&code=<authorization code>
```

2. The cURL sends an token request including the `resource` parameter whose value is `https://mcp.example.com/v1/mcp`, namely **different** from the one in the authorization request.

The token endpoint is as follows:
- http://localhost:8080/realms/mcp-test/protocol/openid-connect/

The MCP server URL is as follows:
- https://mcp.example.com/v1/mcp
The value is different from the one in the authorization request (https://res.example.com/v2/mcp).

The form parameters of the token request are as follows:
- code=\<authorization code\>​
- grant_type=authorization_code​
- client_id=mcp-client​
- redirect_uri=http://localhost:8080/mcp-client/cb​
- code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk​
- resource=https://mcp.example.com/v1/mcp

The token request is the following:
```
curl --location --request POST 'http://localhost:8080/realms/mcp-test/protocol/openid-connect/token' \​
--header 'Content-Type: application/x-www-form-urlencoded' \​
--data-urlencode 'code=<authorization code>' \​
--data-urlencode 'grant_type=authorization_code' \​
--data-urlencode 'client_id=mcp-client' \​
--data-urlencode 'redirect_uri=http://localhost:8080/mcp-client/cb' \​
--data-urlencode 'resource=https://mcp.example.com/v1/mcp' \​
--data-urlencode 'code_verifier=dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk'
```

The error token response is the following:
```
{"error":"invalid_grant","error_description":"resource parameter value in token request does not match the one in authorization request."}
```

### Further customization

#### Token Audience Binding

To make the system in MCP more secure, the custom client policy executor can be customized to support allow/deny lists for `resource` parameter value, which can make keycloak control which MCP servers are accepted or denied.