# FAPI Playground

This is the example application to demonstrate Keycloak FAPI 1 support and DPoP support. It requires to:
- Run and setup Keycloak server on your laptop
- Run Wildfly server on your laptop with this application deployed

## Warning

This application is for demonstration purposes with Keycloak integration. It is not proper implementation of FAPI relying party and
does not do all the verifications prescribed for the client application in the FAPI specifications:
- https://openid.net/specs/openid-financial-api-part-1-1_0.html#public-client
- https://openid.net/specs/openid-financial-api-part-2-1_0.html#confidential-client

For DPoP, see https://datatracker.ietf.org/doc/html/rfc9449 and Keycloak documentation in https://www.keycloak.org/docs/latest/server_admin/index.html#_oidc_clients .

## Pre-requisites

This demo assumes Keycloak running on `https://as.keycloak-fapi.org:8443` and application running on `https://app.keycloak-fapi.org:8543`.
In order to have both running on your laptop, you may need to ensure that these servers are bound to your host.

On linux, the easiest is to edit `/etc/hosts` file and add the host similar to this
```
127.0.0.1 as.keycloak-fapi.org app.keycloak-fapi.org
``` 

## Build this project

This project is tested with OpenJDK 21 and Maven 3.9.9

### Temp: Build Keycloak

It is temporarily needed to build Keycloak as project has dependency on Keycloak snapshot. You can either edit your `pom.xml` to allow downloading snapshots, but
maybe easier is to build Keycloak on your laptop to make sure that snapshot artifacts available in your local repository. Some possible steps to do it:

```
git clone git@github.com:keycloak/keycloak.git
cd keycloak
mvn clean install -DskipTests=true -Pdistribution
```

_TODO: Remove this requirement and update dependency on released 26.4.0 Keycloak once it is released. Also might allow to remove --features=dpop from the startup command below_

### Build project

From the root of this project, run:
```
mvn clean install
```

## Start and prepare keycloak

This was tested with OpenJDK 21 and Keycloak nightly distribution (before 26.4.0 release) 

1) Copy keystore + truststore to the Keycloak distribution:
```
cp keystores/keycloak.* $KEYCLOAK_HOME/bin
```

2) Start the server 
```
cd $KEYCLOAK_HOME/bin
./kc.sh start --hostname=as.keycloak-fapi.org --https-key-store-file=keycloak.jks --https-key-store-password=secret \
--https-trust-store-file=keycloak.truststore --https-trust-store-password=secret \
--https-client-auth=request --features=dpop
```


3) Create and configure new realm

3.a) Go to `https://as.keycloak-fapi.org:8443/`, create admin account, login to admin console

3.b) Create realm `test`

3.c) Create some user with password in this realm 

3.d) Under `Clients` -> `Initial Access Tokens` create new initial access token and copy it somewhere for the
later use in the demo. For demo purposes, use bigger number of clients (EG. 99).


## Start example app and deploy the example

1) Unzip Wildlfy 33.0.1 to some directory. Will be referred as `$APP_HOME`

2) Copy keystore and truststore:
```
cp keystores/keycloak.truststore $APP_HOME/standalone/configuration/
cp keystores/client.jks $APP_HOME/standalone/configuration/
```

3) Deploy the application:
```
cp target/fapi-demo.war $APP_HOME/standalone/deployments/
```

3) Start the wildfly server:
```
cd $APP_HOME/bin
./standalone.sh -b app.keycloak-fapi.org -Djboss.socket.binding.port-offset=100
```

## Demo

### FAPI 1 Demo

1) Go to `https://app.keycloak-fapi.org:8543/fapi-demo` 

__2) No FAPI yet__

2.a) In the `Client Registration` part, you can provide Initial access token from Keycloak (See above) and register some client. Can be for example
public client (Switch `Client authentication method` can be switched to `none`)

2.b) You can click `Create Login URL` and click `Login` . After user authentication, you can be redirected back to the application.
You should see 200 from token response. No FAPI is involved yet. You can see that tokens don't have `nonce` claim in it (Tokens can be seen by click on the button `Show Last tokens`) 

__3) Fapi Baseline test__

3.a) In the Keycloak admin console, in the tab `Realm Settings` -> `Client Policies`, you can create create client policy with `any-client` condition and
link with the built-in `fapi-1-baseline` profile.

3.b) Now in the application, you can register new client. You can doublecheck in the Keycloak admin console, that it has `Consent Required` switched to ON.
Note that you can doublecheck the client by looking at `Client_id` claim from the returned client registration response and then lookup this client by this client ID
in the Keycloak admin console `Clients` tab.

3.c) You can click `Create login URL` and login with new client. Note that to pass `fapi-1-baseline`, it is needed to check `Use Nonce param`
and `Use PKCE`. Otherwise, Keycloak won't allow login.

3.d) Authentication requires user to consent. After authentication, check that ID token has `nonce` claim (Ideally you should check that it matches with the
`nonce` sent in the initial request)

__4) Fapi advanced test__

4.a) Change client policy from above to use `fapi-1-advanced` instead of baseline.

4.b) Register new client. It must be checked the checkbox `Generate client keys` and `Client authentication method` should be set to `tls_client_auth` in case of "FAPI 1 Advanced"

4.c) Create login URL. It must be checked with both `Use nonce` and `Use Request Object` to send stuff in signed request object. Note that this also uses `response_type=code id_token`
, which is one of the allowed `response_type` values for FAPI advanced. The OIDC authentication response parameters are sent in the fragment (not query as other `response_type` are using).


4.d) After authentication, you can check by `Show Last Tokens` that access token has hash of it's certificate, due Keycloak used `Sender Constrained access token`
required by the specs. This hash is based on the X.509 certificate used for client authentication (It is not DPoP based hash, which is described below).

### DPOP Demo

1) It is assumption you have realm `test`, some user in the realm and initial access token as described in the `FAPI 1 Demo` above. 
But it is recommended to disable the client policies set by `FAPI 1 Demo`

2) It is recommended to test DPoP with `Client authentication method` set either to `none` (public clients) or `client_auth_basic` (Normal confidential client with client-secret based authentication)

__3) Use DPoP__ - Switch `Use DPoP` in the FAPI playground demo will make sure that DPoP is used in the token-request (after user login and being redirected from Keycloak back to the application), refresh-token request and user-info requests.
Some example scenarios (you can come with more):

__3.a) Public client test__ - Try to login, Then refresh token (button `Refresh token`) or send User-info request with the obtained access token (Button `Send User Info`).
Check that both access-token and refresh-token has `cnf` thumbprint after authentication.

__3.b) Confidential client test__ - Test for confidential clients with `client_auth_basic` authentication. Check that only access-token has `cnf` claim, but refresh token does not have

__3.c) Rotating DPoP keys__ - After DPoP login, try to `Rotate DPoP keys`. This will make client application to rotate DPoP keys and hence made existing DPoP bound tokens not effectively usable by this
client application. You can notice that `Send user info` will not work. The `Refresh token` will not work for public clients,
but will work for confidential clients (as refresh token is not DPoP bound for confidential client)

__4) Binding to authorization code__ - Switch `Use DPoP Authorization Code Binding` will add parameter `dpop_jkt` to the OIDC authentication request.

4.a) Try to enable this switch and disable `Use DPoP`. Login will not work as `dpop_jkt` used in the OIDC authentication request is not used in the token request.
But when both `Use DPoP Authorization Code Binding` and `Use DPoP` are checked, login should work

__5) Client based switch__ - Try to enable switch `Require DPoP bound tokens` in the Keycloak admin console for your OIDC registered client. Switch can be seen in the section `Capability config` of
OIDC client in the Keycloak admin console (See Server administration guide for more details). You can see that after doing this, the switch `Use DPoP` in the FAPI playground
must be checked. Login without DPoP will not work and Token Request will return 400 HTTP error as DPoP is mandatory for the client with this switch enabled.

__6) DPoP Enforcer executor__ - In the Keycloak admin console, in the tab `Realm settings` -> tab `Client policies` -> tab `Profiles`, you can create new client profile called for example `dpop-profile` and 
add the client policy executor `dpop-enforcer-executor` to this profile. Configure executor according your preference. Then in the `Realm settings` -> `Client policies` -> `Policies`
you can create client policy `dpop-policy` with condition `any-client` and link to the `dpop-profile` client profile.

6.a) In the FAPI playground application, you can register new client. If `Auto configure` was enabled for the client policy executor created above, then new client will have
`"dpop_bound_access_tokens" : true` in the `Client Registration Response`. This means DPoP will be mandatory for this client.

6.b) If you checked `Enforce Authorization Code binding to DPoP key` for the DPoP client policy executor above, you can notice that plagroud will require `Use DPoP Authorization Code Binding`
for the successful login.



## Contributions

Anyone is welcome to use this demo according with the licence and feel free to use it in your own presentations for FAPI or OAuth2
Contributions are welcome. Please send PR to this repository with the possible contributions.

Possible contribution tips:

1) Automated tests (ideally with the use of Junit5 and Keycloak test framework - https://www.keycloak.org/2024/11/preview-keycloak-test-framework )

2) Deploy FAPI playground on Quarkus instead of WildFly

3) Make it working without a need to fork `OAuthClient` utilities from Keycloak codebase. The package `org.keycloak.example.oauth` contains lots of
classes copied from Keycloak module https://github.com/keycloak/keycloak/tree/main/test-framework/oauth . Instead of forking classes, it can be good to use
directly the Keycloak classes and have dependency on that Keycloak module. It failed for me due the `keycloak-test-framework-oauth` module has dependency on `keycloak-services`,
which has many other 3rd party dependencies and this caused some issues when this was deployed on WildFly.

How to possibly fix this:

3.a) Use Quarkus instead of WildFly (See step 2). But not sure if it helps... 

3.b) Make sure that Keycloak `keycloak-test-framework-oauth` module from test-framework does not have dependencies on `keycloak-services` (will require some changes in the Keycloak itself). There is
maybe not so much changes needed as `keycloak-test-framework-oauth` client has mostly dependencies on various constants and minor utilities from `keycloak-services`. 

4) Add some other FAPI/OAuth/OIDC related functionality to this demo (EG. OIDC4VCI or something else)

5) Cleanup. There are lots of TODOs in the codebase. Also maybe UI can be improved. The README instructions can be possibly improved and made more clear as well.
Feel free to create GH issue at least if you find the trouble, but PR with contribution is welcome even more!

(See above for potential contributions tips and also search for `TODO:` in the code :-) )

## Slides

See slides from devconf 2022 presentation in file [Keycloak FAPI slides](keycloak-fapi-devconf-2022-slides.pdf).
