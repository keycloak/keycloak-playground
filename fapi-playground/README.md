# FAPI Playground

This is the example application to demonstrate Keycloak FAPI 1 support and DPoP support. It requires to:
- Run and setup Keycloak server on your laptop
- Run the application deployed on Quarkus

## Warning

This application is for demonstration purposes with Keycloak integration. It is not proper implementation of FAPI relying party and
does not do all the verifications prescribed for the client application in the FAPI specifications:
- https://openid.net/specs/openid-financial-api-part-1-1_0.html#public-client
- https://openid.net/specs/openid-financial-api-part-2-1_0.html#confidential-client

For DPoP, see https://datatracker.ietf.org/doc/html/rfc9449 and Keycloak documentation in https://www.keycloak.org/docs/latest/server_admin/index.html#_oidc_clients .

## Pre-requisites

This demo assumes Keycloak running on `https://as.keycloak-fapi.org:8443` and application running on `https://app.keycloak-fapi.org:8543`.
This is to mimic real servers.  In order to have both running on your laptop, you may need to ensure that these servers are bound to your host.

On linux, the easiest is to edit `/etc/hosts` file and add the host similar to this
```
127.0.0.1 as.keycloak-fapi.org app.keycloak-fapi.org
``` 

## Build this project

This project is tested with OpenJDK 21 and Maven 3.9.9 and Keycloak 26.4.0 release

### Build project

From the root of this project, run:
```
mvn clean install
```

## Start and prepare keycloak

1) Copy keystore + truststore to the Keycloak distribution:
```
cp keystores/keycloak.* $KEYCLOAK_HOME/bin
```

2) Start the server 
```
cd $KEYCLOAK_HOME/bin
./kc.sh start --hostname=as.keycloak-fapi.org --https-key-store-file=keycloak.jks --https-key-store-password=secret \
--https-trust-store-file=keycloak.truststore --https-trust-store-password=secret \
--https-client-auth=request --features=oid4vc-vci
```


3) Create and configure new realm

3.a) Go to `https://as.keycloak-fapi.org:8443/`, create admin account, login to admin console

3.b) Create realm `test`.

3.c) Create some user with password in this realm 

3.d) Under `Clients` -> `Initial Access Tokens` create new initial access token and copy it somewhere for the
later use in the demo. For demo purposes, use bigger number of clients (EG. 99).


## Start example app and deploy the example

1) Start application by running
```
mvn quarkus:run
```

For debugging, it is possible to use `mvn quarkus:dev` (However application is then running on https://localhost:8543 )

## Demo

There are few demos, which can be run independently on each other (EG. Running DPoP demo does not prescribe that you must run also steps in the FAPI demo).

### FAPI 1 Demo

1) Go to `https://app.keycloak-fapi.org:8543` 

__2) No FAPI yet__

2.a) In the `Client Registration` part, you can provide Initial access token from Keycloak (See above) and register some client. Can be for example
public client (Switch `Client authentication method` can be switched to `none`)

2.b) You can click `Create Login URL` and click `Login` . After user authentication, you will be redirected back to the application.
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
But it is recommended to disable the client policies set by `FAPI 1 Demo` (in case that you run the `FAPI 1 demo` beforehand).

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
add the client policy executor `dpop-bind-enforcer` executor to this profile. Configure executor according your preference. Then in the `Realm settings` -> `Client policies` -> `Policies`
you can create client policy `dpop-policy` with condition `any-client` and link to the `dpop-profile` client profile.

6.a) In the FAPI playground application, you can register new client. If `Auto configure` was enabled for the client policy executor created above, then new client will have
`"dpop_bound_access_tokens" : true` in the `Client Registration Response`. This means DPoP will be mandatory for this client.

6.b) If you checked `Enforce Authorization Code binding to DPoP key` for the DPoP client policy executor above, you can notice that plagroud will require `Use DPoP Authorization Code Binding`
for the successful login.

## OID4VCI Demo

NOTE: At the time of this writing, this is expected to be tested with latest Keycloak server nightly release (from 2025-12-05 or newer).

OID4VCI demo expects that server is started with the `--features=oid4vc-vci` feature enabled.

1) It is expected to import realm `test` from this directory with some pre-configured client scopes and stuff.
Please login to the admin console, delete realm `test` (if you have existing realm from previous demos) and import realm
from the file [oid4vci/singleFile-realm.json](oid4vci/singleFile-realm.json) .

2) Go to http://localhost:8543 . Then obtain initial access token, register OIDC client (Tested with client authentication method `None`)

3) Go back to Keycloak admin console and lookup your newly registered client from the `test` realm.
Manually update the client to:
3.a) Enable OID4VCI switch for this client. It can be found in the `Advanced` tab of the client, and then in the section `OpenID for Verifiable Credentials`
at the bottom of the page
3.b) Assign some OID4VCI client scope to this client according to the credential you want to issue. For example you may assign
client scopes `education-certificate` and `oid4vc_natural_person` to the client. Make sure to assign it as `Optional` client scope to the client.

4) In the demo application, obtain OID4VCI metadata (first button in the OID4VCI section of the page) and make sure to
select OID4VCI credential related to the client scope you assigned to the client in the previous step.

5) Select some credential from `Credential Type` (ideally `Education Certificate`) and click `Credential issuance - Authorization code grant`
and make sure that OID4VCI credential is successfully issued. See how OIDC authentication request with `Authorization details` will look like.
And then click `Login` link at the bottom of the page

6) Login as `john` with `password` . It is needed to update `University` attribute in case
you selected `Education Certificate` in previous step. This is due the user-profile configuration
and due the fact that `education-certificate` scope was added as a request parameter to OIDC authentication request together
with authorization details

7) Now you can click `Credential request`, which should fail as user missing the mandatory attribute `education-certificate-number`.
   (NOTE: This requires fixing https://github.com/keycloak/keycloak/issues/44796 in current main).

8) In the other tab in the admin console, admin is able to manually update user `john` and fill the `Education certificate number` for him.
In reality, assumption is, that there should be some "business process" needed for this (EG. user `john` uploads his university
diploma somewhere to be able to share it with the administrator)

9) Go back to fapi-demo and click `Credential request` again. Now credential should be successfully issued for `john` .

10) See button `Show last verifiable credential` to see the parsed sd-jwt data.
 
11) Then fill `Claims to present (divided by comma):`
with some claims (EG. `university,firstName,lastName`) and click `Create presentation from last verifiable credential`.
You can see sd-jwt with only subset of the claims.

12) Change credential type to `oid4vc_natural_person` and fill username as `alice` . Now it is OK to
send `Credential issuance - pre-authorized code` grant. See requests to observe the credential-offer and pre-authorized token
obtained for `alice` .
**NOTE 1:** user `john` can obtain credential-offer for `alice` as he has role `credential-offer-create`).
**NOTE 2:** Not sure if this flow would still work in later Keycloak versions in a way shown in this demo. See discussion https://github.com/keycloak/keycloak/discussions/44764
for the details.

13) Repeat steps 9, 10, 11 and observe new `oid4vc_natural_person` VC for alice


## Test with latest Keycloak nightly 

1) Build Keycloak

It may be good to build Keycloak as project has dependency on Keycloak snapshot. You can either edit your `pom.xml` to allow downloading snapshots from last nightly build, but
maybe easier is to build Keycloak on your laptop to make sure that snapshot artifacts available in your local repository. Some possible steps to do it:

```
git clone git@github.com:keycloak/keycloak.git
cd keycloak
mvn clean install -DskipTests=true -Pdistribution
```

2) Update in `pom.xml` and set `keycloak.version` property to `999.0.0-SNAPSHOT` .

## Contributions

Anyone is welcome to use this demo according with the licence and feel free to use it in your own presentations for FAPI, OAuth2, OIDC, DPoP or anything else.
Contributions are welcome. Please send PR to this repository with the possible contributions.

Possible contribution tips:

1) Automated tests (ideally with the use of Junit5 and Keycloak test framework - https://www.keycloak.org/2024/11/preview-keycloak-test-framework )

2) Update to newer version of Keycloak (might need to update dependencies in `pom.xml`. Maybe code as well if something changed)

3) Add some other FAPI/OAuth/OIDC related functionality to this demo (EG. OIDC4VCI or something else)

4) Cleanup. There are lots of TODOs in the codebase. Also maybe UI can be improved. The README instructions can be possibly improved and made more clear as well.
Feel free to create GH issue at least if you find the trouble, but PR with contribution is welcome even more!

(See above for potential contributions tips and also search for `TODO:` in the code :-) )

## Slides

See slides from devconf 2022 presentation in file [Keycloak FAPI slides](keycloak-fapi-devconf-2022-slides.pdf).
