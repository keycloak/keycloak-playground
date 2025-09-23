package org.keycloak.example;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.common.util.Time;
import org.keycloak.example.bean.AuthorizationEndpointRequestObject;
import org.keycloak.example.bean.InfoBean;
import org.keycloak.example.bean.ServerInfoBean;
import org.keycloak.example.bean.UrlBean;
import org.keycloak.example.oauth.AbstractHttpPostRequest;
import org.keycloak.example.oauth.AccessTokenRequest;
import org.keycloak.example.oauth.AccessTokenResponse;
import org.keycloak.example.oauth.LoginUrlBuilder;
import org.keycloak.example.oauth.PkceGenerator;
import org.keycloak.example.oauth.RefreshRequest;
import org.keycloak.example.oauth.UserInfoRequest;
import org.keycloak.example.oauth.UserInfoResponse;
import org.keycloak.example.util.ClientConfigContext;
import org.keycloak.example.util.ClientRegistrationWrapper;
import org.keycloak.example.util.DPoPContext;
import org.keycloak.example.util.KeysWrapper;
import org.keycloak.example.util.MyConstants;
import org.keycloak.example.util.MyException;
import org.keycloak.example.util.OAuthClient;
import org.keycloak.example.util.OIDCFlowConfigContext;
import org.keycloak.example.util.OIDCLoginProtocol;
import org.keycloak.example.util.SessionData;
import org.keycloak.example.util.UUIDUtil;
import org.keycloak.example.util.WebRequestContext;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.dpop.DPoP;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.util.JWKSUtils;
import org.keycloak.util.JsonSerialization;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
@Path("/")
public class WebEndpoint {

    private static final Logger log = Logger.getLogger(WebEndpoint.class);

    @Context
    private HttpHeaders headers;

    @Context
    private HttpRequest request;

    Map<String, Object> fmAttributes = new HashMap<>();

    @GET
    @Produces("text/html")
    @NoCache
    public Response getWebPage() {
        fmAttributes.put("info", new InfoBean());
        return renderHtml();
    }

    private Response renderHtml() {
        fmAttributes.put("serverInfo", new ServerInfoBean());
        fmAttributes.put("url", new UrlBean());
        fmAttributes.put("clientConfigCtx", Services.instance().getSession().getClientConfigContext());
        fmAttributes.put("oidcConfigCtx", Services.instance().getSession().getOidcConfigContext());
        return Services.instance().getFreeMarker().processTemplate(fmAttributes, "index.ftl");
    }


    @GET
    @Produces("text/css")
    @NoCache // TODO: This could be cached...
    @Path("/styles.css")
    public Response staticResources() {
        try {
            InputStream is = getClass().getResourceAsStream("/static/styles.css");
            String css = StreamUtil.readString(is, StandardCharsets.UTF_8);

            Response.ResponseBuilder builder = Response.status(Response.Status.OK).type("text/css").entity(css);
            return builder.build();
        } catch (IOException ioe) {
            throw new NotFoundException("CSS not found", ioe);
        }
    }

    @POST
    @Produces("text/html")
    @NoCache
    @Path("/action")
    public Response processAction() {
        MultivaluedMap<String, String> params = request.getDecodedFormParameters();
        String action = params.getFirst("my-action");
        SessionData session = Services.instance().getSession();
        WebRequestContext<AbstractHttpPostRequest, AccessTokenResponse> lastTokenResponse = session.getTokenRequestCtx();
        try {
            switch (action) {
                case "wellknown-endpoint":
                    OIDCConfigurationRepresentation cfg = Services.instance().getSession().getAuthServerInfo();
                    try {
                        fmAttributes.put("info", new InfoBean("OIDC well-known response", JsonSerialization.writeValueAsPrettyString(cfg)));
                    } catch (IOException ioe) {
                        throw new MyException("Error when trying to deserialize OIDC well-known response to string", ioe);
                    }
                    break;

                case "register-client":
                    ClientConfigContext clientCtx = collectClientConfigParams(params, session);
                    String initToken = clientCtx.getInitialAccessToken();
                    if (initToken == null || initToken.trim().isEmpty()) {
                        throw new MyException("Init token is missing. It is required when registering client. Please obtain init token from Keycloak admin console and try again");
                    }

                    ClientRegistrationWrapper clientReg = ClientRegistrationWrapper.create();
                    clientReg.setInitToken(initToken);

                    OIDCClientRepresentation oidcClient = createClientToRegister(clientCtx.getClientAuthMethod(), clientCtx.isGenerateJwks());
                    try {
                        WebRequestContext<OIDCClientRepresentation, OIDCClientRepresentation> res = clientReg.registerClient(oidcClient);
                        session.setRegisteredClient(res.getResponse());

                        OAuthClient oauthClient = Services.instance().getOauthClient();
                        if (OIDCLoginProtocol.CLIENT_SECRET_BASIC.equals(session.getClientConfigContext().getClientAuthMethod())) {
                            oauthClient.client(res.getResponse().getClientId(), res.getResponse().getClientSecret());
                        } else {
                            oauthClient.client(res.getResponse().getClientId());
                        }

                        fmAttributes.put("info", new InfoBean("Client Registration Request", JsonSerialization.writeValueAsPrettyString(res.getRequest()),
                                "Client Registration Response", JsonSerialization.writeValueAsPrettyString(res.getResponse())));
                    } catch (IOException ioe) {
                        throw new MyException("Error when trying to deserialize OIDC client registration", ioe);
                    } finally {
                        clientReg.close();
                    }
                    break;

                case "show-registered-client":
                    OIDCClientRepresentation client = session.getRegisteredClient();
                    if (client == null) {
                        fmAttributes.put("info", new InfoBean("No Registered client", "No client registered"));
                    } else {
                        try {
                            fmAttributes.put("info", new InfoBean("Last Registered client", JsonSerialization.writeValueAsPrettyString(client)));
                        } catch (IOException ioe) {
                            throw new MyException("Error when trying to deserialize OIDC registered client", ioe);
                        }
                    }

                    break;
                case "create-login-url":
                    OIDCFlowConfigContext oidcFlowCtx = collectOIDCFlowConfigParams(params, session);

                    String authRequestUrl = getAuthorizationRequestUrl(oidcFlowCtx);
                    fmAttributes.put("info", new InfoBean("OIDC Authentication Request URL", authRequestUrl));
                    fmAttributes.put("authRequestUrl", authRequestUrl);
                    session.setAuthenticationRequestUrl(authRequestUrl);
                    break;
                case "process-fragment":
                    String authzResponseUrl = params.getFirst("authz-response-url");
                    int fragmentIndex = authzResponseUrl.indexOf('#');
                    if (fragmentIndex == -1) {
                        throw new MyException("Fragment did not found in the URL " + authzResponseUrl);
                    }
                    String fragment = authzResponseUrl.substring(fragmentIndex + 1);
                    Map<String, String> parsedParams = Stream.of(fragment.split("&")).collect(Collectors.toMap(
                            param -> param.substring(0, param.indexOf('=')),
                            param -> param.substring(param.indexOf('=') + 1)));
                    return handleLoginCallback(parsedParams.get(OAuth2Constants.CODE), parsedParams.get(OAuth2Constants.ERROR), parsedParams.get(OAuth2Constants.ERROR_DESCRIPTION), authzResponseUrl);
                case "show-last-token-response":
                    if (lastTokenResponse == null) {
                        fmAttributes.put("info", new InfoBean("No Token Response", "No token response yet. Please login first."));
                    } else {
                        try {
                            InfoBean info = new InfoBean();
                            fmAttributes.put("info", info);

                            infoTokenRequestAndResponse(info, lastTokenResponse.getRequest(), lastTokenResponse.getResponse());
                        } catch (IOException ioe) {
                            throw new MyException("Error when trying to deserialize OIDC registered client", ioe);
                        }
                    }
                    break;
                case "show-last-tokens":
                    if (lastTokenResponse == null) {
                        fmAttributes.put("info", new InfoBean("No Token Response", "No token response yet. Please login first."));
                    } else {
                        try {
                            AccessTokenResponse atr = lastTokenResponse.getResponse();
                            if (atr.getAccessToken() == null || atr.getRefreshToken() == null) {
                                fmAttributes.put("info", new InfoBean("No Tokens", "No tokens. Please login first."));
                            } else {
                                IDToken idToken = new JWSInput(atr.getIdToken()).readJsonContent(IDToken.class);
                                AccessToken accessToken = new JWSInput(atr.getAccessToken()).readJsonContent(AccessToken.class);
                                RefreshToken refreshToken = new JWSInput(atr.getRefreshToken()).readJsonContent(RefreshToken.class);
                                fmAttributes.put("info", new InfoBean(
                                        "Last ID Token", JsonSerialization.writeValueAsPrettyString(idToken),
                                        "Last Access Token", JsonSerialization.writeValueAsPrettyString(accessToken),
                                        "Last Refresh Token", JsonSerialization.writeValueAsPrettyString(refreshToken)));
                            }
                        } catch (IOException | JWSInputException ioe) {
                            throw new MyException("Error when trying to deserialize tokens from token response", ioe);
                        }
                    }
                    break;
                case "show-last-dpop-proof":
                    String lastDPoP = session.getOrCreateDpopContext().getLastDpopProof();
                    if (lastDPoP == null) {
                        fmAttributes.put("info", new InfoBean("No DPoP", "No dpop JWT present. Please login first with 'Use DPoP' enabled."));
                    } else {
                        try {
                            JWSInput jws = new JWSInput(lastDPoP);
                            JWSHeader header = jws.getHeader();
                            DPoP dpop = jws.readJsonContent(DPoP.class);

                            fmAttributes.put("info", new InfoBean(
                                    "Last DPoP header", JsonSerialization.writeValueAsPrettyString(header),
                                    "Last DPoP", JsonSerialization.writeValueAsPrettyString(dpop),
                                    "Last thumbprint of JWK key", JWKSUtils.computeThumbprint(header.getKey())));
                        } catch (IOException | JWSInputException ioe) {
                            throw new MyException("Error when trying to deserialize DPoP JWT", ioe);
                        }
                    }
                    break;
                case "refresh-token":
                    collectOIDCFlowConfigParams(params, session);
                    if (lastTokenResponse == null) {
                        fmAttributes.put("info", new InfoBean("No Token Response", "No token response yet. Please login first."));
                    } else {
                        try {
                            if (lastTokenResponse.getResponse().getRefreshToken() == null) {
                                fmAttributes.put("info", new InfoBean("No Refresh token", "No refresh token. Please login first."));
                            } else {
                                InfoBean info = new InfoBean();
                                fmAttributes.put("info", info);

                                WebRequestContext<AbstractHttpPostRequest, AccessTokenResponse> refreshedTokenResponse = sendTokenRefresh(session);
                                session.setTokenRequestCtx(new WebRequestContext<>(refreshedTokenResponse.getRequest(), refreshedTokenResponse.getResponse()));

                                info.addOutput("Refresh token request", JsonSerialization.writeValueAsPrettyString(refreshedTokenResponse.getRequest().getRequestInfo()))
                                        .addOutput("Refresh token response", JsonSerialization.writeValueAsPrettyString(refreshedTokenResponse.getResponse()));
                            }
                        } catch (IOException ioe) {
                            throw new MyException("Error when trying to refresh token", ioe);
                        }
                    }
                    break;
                case "send-user-info":
                    collectOIDCFlowConfigParams(params, session);
                    if (lastTokenResponse == null) {
                        fmAttributes.put("info", new InfoBean("No Token Response", "No token response yet. Please login first."));
                    } else {
                        try {
                            if (lastTokenResponse.getResponse().getAccessToken() == null) {
                                fmAttributes.put("info", new InfoBean("No access token", "No access token. Please login first."));
                            } else {
                                InfoBean info = new InfoBean();
                                fmAttributes.put("info", info);

                                WebRequestContext<UserInfoRequest, UserInfoResponse> userInfo = sendUserInfo(session);

                                info.addOutput("User Info request", JsonSerialization.writeValueAsPrettyString(userInfo.getRequest().getRequestInfo()))
                                        .addOutput("User Info response", JsonSerialization.writeValueAsPrettyString(userInfo.getResponse()));
                            }
                        } catch (IOException ioe) {
                            throw new MyException("Error when trying to send user info", ioe);
                        }
                    }
                    break;
                case "rotate-dpop-keys":
                    collectOIDCFlowConfigParams(params, session);
                    DPoPContext ctx = session.getOrCreateDpopContext();
                    ctx.rotateKeys();
                    fmAttributes.put("info", new InfoBean("DPoP Keys rotated", "New thumbprint: " + ctx.generateKeyThumbprint()));
                    break;
                default:
                    throw new MyException("Illegal action");
            }
        } catch (MyException me) {
            fmAttributes.put("info", new InfoBean("Error!", "Error when performing action. See server log for details"));
            log.error(me.getMessage(), me);
        }

        return renderHtml();
    }

    private ClientConfigContext collectClientConfigParams(MultivaluedMap<String, String> params, SessionData session) {
        String initToken = params.getFirst("init-token");
        String clientAuthMethod = params.getFirst("client-auth-method");
        boolean generateJwks = params.getFirst("jwks") != null;
        ClientConfigContext clientCtx = new ClientConfigContext(initToken, clientAuthMethod, generateJwks);
        session.setClientConfigContext(clientCtx);
        return clientCtx;
    }

    private OIDCFlowConfigContext collectOIDCFlowConfigParams(MultivaluedMap<String, String> params, SessionData session) {
        boolean pkce = params.getFirst("pkce") != null;
        boolean nonce = params.getFirst("nonce") != null;
        boolean requestObject = params.getFirst("request-object") != null;
        boolean useDPoP = params.getFirst("dpop") != null;
        boolean useDPoPJKT = params.getFirst("dpop-authz-code-binding") != null;
//        if (useDPoPJKT && !useDPoP) {
//            throw new MyException("Incorrect to disable 'Use DPoP' and enable 'Use DPoP Authorization Code Binding' at the same time");
//        }
        OIDCFlowConfigContext ctx = new OIDCFlowConfigContext(pkce, nonce, requestObject, useDPoP, useDPoPJKT);
        session.setOidcFlowContext(ctx);
        return ctx;
    }

    @GET
    @Produces("text/html")
    @NoCache
    @Path("/login-callback")
    public Response loginCallback(@QueryParam(OAuth2Constants.CODE) String code,
                                  @QueryParam(OAuth2Constants.STATE) String state,
                                  @QueryParam(OAuth2Constants.SESSION_STATE) String sessionState,
                                  @QueryParam(OAuth2Constants.ERROR) String error,
                                  @QueryParam(OAuth2Constants.ERROR_DESCRIPTION) String errorDescription) {
        if (code == null && error == null) {
            // Fragment response mode
            fmAttributes.put("serverInfo", new ServerInfoBean());
            fmAttributes.put("url", new UrlBean());
            return Services.instance().getFreeMarker().processTemplate(fmAttributes, "code-parser.ftl");
        }
        return handleLoginCallback(code, error, errorDescription, request.getUri().getRequestUri().toString());
    }

    private Response handleLoginCallback(String code, String error, String errorDescription, String origAuthzResponseUrl) {
        SessionData session = Services.instance().getSession();
        if (error != null) {
            fmAttributes.put("info", new InfoBean("OIDC Authentication request URL sent", session.getAuthenticationRequestUrl(), "Error!", "Error returned from Authentication response: " + error + ", Error description: " + errorDescription));
        } else {
            try {
                // WebResponse<List<NameValuePair>, OAuthClient.AccessTokenResponse> tokenResponse = Services.instance().getOauthClient().doAccessTokenRequest(code, null, MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore());
                OAuthClient oauthClient = Services.instance().getOauthClient();

                oauthClient.redirectUri(session.getRegisteredClient().getRedirectUris().get(0));

                AccessTokenRequest tokenRequest = oauthClient.accessTokenRequest(code);

                if (session.getOidcConfigContext().isUseDPoP()) {
                    String dpopProof = session.getOrCreateDpopContext().generateDPoP(HttpMethod.POST, session.getAuthServerInfo().getTokenEndpoint(), null);
                    tokenRequest.dpopProof(dpopProof);
                }
                if (session.getOidcConfigContext().isUsePkce()) {
                    tokenRequest.codeVerifier(session.getPkceContext());
                }
                AccessTokenResponse tokenResponse = tokenRequest.send();

                InfoBean info = new InfoBean("Authentication request URL", session.getAuthenticationRequestUrl())
                        .addOutput("Authentication response URL", origAuthzResponseUrl);

                infoTokenRequestAndResponse(info, tokenRequest, tokenResponse);

                fmAttributes.put("info", info);
                session.setTokenRequestCtx(new WebRequestContext<>(tokenRequest, tokenResponse));
            } catch (Exception me) {
                fmAttributes.put("info", new InfoBean("Error!", "Error when performing action. See server log for details"));
                log.error(me.getMessage(), me);
            }
        }
        return renderHtml();
    }

    private void infoTokenRequestAndResponse(InfoBean info, AbstractHttpPostRequest tokenRequest, AccessTokenResponse tokenResponse) throws IOException {
        info.addOutput("Token request", JsonSerialization.writeValueAsPrettyString(tokenRequest.getRequestInfo()))
                .addOutput("Token response", JsonSerialization.writeValueAsPrettyString(tokenResponse));
    }


    private String getAuthorizationRequestUrl(OIDCFlowConfigContext oidcFlowCtx) {
        OAuthClient oauthClient = Services.instance().getOauthClient();
        SessionData session = Services.instance().getSession();
        OIDCClientRepresentation oidcClient = session.getRegisteredClient();
        if (oidcClient == null) {
            throw new MyException("Not client registered. Please register client first");
        }

        String dpopJkt = oidcFlowCtx.isUseDPoPAuthzCodeBinding() ? session.getOrCreateDpopContext().generateKeyThumbprint() : null;

        if (oidcFlowCtx.isUseRequestObject()) {
            AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor(oidcClient.getClientId(), oidcFlowCtx.isUseNonce());
            KeysWrapper keys = Services.instance().getSession().getKeys();
            if (keys == null) {
                throw new MyException("JWKS keys not set when generating request object. Keys need to be created during client registration");
            }
            String request = keys.getOidcRequest(requestObject, Services.instance().getSession().getRegisteredClient().getRequestObjectSigningAlg());
            // oauthClient.client(oidcClient.getClientId()); Already set after client registration
            return oauthClient.redirectUri(null)
                    .responseType("code id_token")
                    .loginForm()
                        .request(request)
                        .nonce(null)
                        .state(null)
                        .dpopJkt(dpopJkt)
//                        .responseType(null);
            // oauthClient.responseMode("query");
                        .build();
        } else {
            LoginUrlBuilder loginUrl = oauthClient//.client(oidcClient.getClientId()) - Already set after client registration
                .responseType(OAuth2Constants.CODE)
                .redirectUri(oidcClient.getRedirectUris().get(0))
                    .loginForm()
                        .state(SecretGenerator.getInstance().generateSecureID())
                        .request(null);

            setPkceInAuthenticationRequest(session, oidcFlowCtx,
                    pkce -> loginUrl.codeChallenge(pkce),
                    () -> loginUrl.codeChallenge(null, null));

            if (oidcFlowCtx.isUseNonce()) {
                loginUrl.nonce(SecretGenerator.getInstance().generateSecureID());
            } else {
                loginUrl.nonce(null);
            }
            loginUrl.dpopJkt(dpopJkt);
            return loginUrl.build();
        }
    }


    private void setPkceInAuthenticationRequest(SessionData session, OIDCFlowConfigContext oidcFlowCtx, Consumer<PkceGenerator> pkceConsumer1, Runnable pkceCleaner) {
        if (oidcFlowCtx.isUsePkce()) {
            PkceGenerator pkce = PkceGenerator.s256();
            session.setPkceContext(pkce);
            pkceConsumer1.accept(pkce);
        } else {
            session.setPkceContext(null);
            pkceCleaner.run();
        }
    }


    private OIDCClientRepresentation createClientToRegister(String clientAuthMethod, boolean generateJwks) {
        OIDCClientRepresentation client = new OIDCClientRepresentation();
        client.setClientName("my fapi client");
        UrlBean urls = new UrlBean();
        client.setClientUri(urls.getBaseUrl());
        client.setRedirectUris(Collections.singletonList(urls.getClientRedirectUri()));
        client.setTokenEndpointAuthMethod(clientAuthMethod);
        if (OIDCLoginProtocol.TLS_CLIENT_AUTH.equals(clientAuthMethod)) {
            client.setTlsClientAuthSubjectDn(MyConstants.EXACT_CERTIFICATE_SUBJECT_DN);
            client.setResponseTypes(Arrays.asList("code", "code id_token")); // Indicates that we want fapi advanced. This should be done in a better way...
        }

        if (generateJwks) {
            KeysWrapper keys = new KeysWrapper();
            keys.generateKeys("PS256", true); // Hardcoded alg to be default for fapi-advanced. Should be improved...
            JSONWebKeySet jwks = keys.getJwks();
            client.setJwks(jwks);
            Services.instance().getSession().setKeys(keys);
        } else {
            Services.instance().getSession().setKeys(null);
        }
        return client;
    }

    protected AuthorizationEndpointRequestObject createValidRequestObjectForSecureRequestObjectExecutor(String clientId, boolean nonce) {
        SessionData session = Services.instance().getSession();

        AuthorizationEndpointRequestObject requestObject = new AuthorizationEndpointRequestObject();
        requestObject.id(UUIDUtil.generateId());
        requestObject.iat(Long.valueOf(Time.currentTime()));
        requestObject.exp(requestObject.getIat() + Long.valueOf(300));
        requestObject.nbf(requestObject.getIat());
        requestObject.setClientId(clientId);
        requestObject.setResponseType("code id_token");
        requestObject.setRedirectUriParam(new UrlBean().getClientRedirectUri());
        requestObject.setScope("openid");
        String state = UUIDUtil.generateId();
        requestObject.setState(state);
        requestObject.setMax_age(Integer.valueOf(600));
        requestObject.setOtherClaims("custom_claim_ein", "rot");
        if (session.getAuthServerInfo() == null) {
            throw new MyException("Please make sure that well-known info is executed before generating request object");
        }
        requestObject.audience(session.getAuthServerInfo().getIssuer(), "https://example.com");
        if (nonce) {
            requestObject.setNonce(UUIDUtil.generateId());
        }

        setPkceInAuthenticationRequest(session, session.getOidcConfigContext(),
                pkce -> {
                    requestObject.setCodeChallenge(pkce.getCodeChallenge());
                    requestObject.setCodeChallengeMethod(pkce.getCodeChallengeMethod());
                },
                () -> {
                    requestObject.setCodeChallenge(null);
                    requestObject.setCodeChallengeMethod(null);
                });

        return requestObject;
    }

    private WebRequestContext<AbstractHttpPostRequest, AccessTokenResponse> sendTokenRefresh(SessionData session) {
        OAuthClient oauthClient = Services.instance().getOauthClient();
        String refreshToken = session.getTokenRequestCtx().getResponse().getRefreshToken(); // Already checked that there is tokenRequestCtx
        RefreshRequest tokenRequest = oauthClient.refreshRequest(refreshToken);

        if (session.getOidcConfigContext().isUseDPoP()) {
            String dpopProof = session.getOrCreateDpopContext().generateDPoP(HttpMethod.POST, session.getAuthServerInfo().getTokenEndpoint(), refreshToken);
            tokenRequest.dpopProof(dpopProof);
        }
        AccessTokenResponse tokenResponse = tokenRequest.send();
        return new WebRequestContext<>(tokenRequest, tokenResponse);
    }

    private WebRequestContext<UserInfoRequest, UserInfoResponse> sendUserInfo(SessionData session) {
        OAuthClient oauthClient = Services.instance().getOauthClient();
        String accessToken = session.getTokenRequestCtx().getResponse().getAccessToken(); // Already checked that there is tokenRequestCtx
        UserInfoRequest userInfoRequest = oauthClient.userInfoRequest(accessToken);

        if (session.getOidcConfigContext().isUseDPoP()) {
            String dpopProof = session.getOrCreateDpopContext().generateDPoP(HttpMethod.GET, session.getAuthServerInfo().getUserinfoEndpoint(), accessToken);
            userInfoRequest.dpop(dpopProof);
        }
        UserInfoResponse tokenResponse = userInfoRequest.send();
        return new WebRequestContext<>(userInfoRequest, tokenResponse);
    }

}
