package org.keycloak.example;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.vertx.core.http.HttpServerRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HttpMethod;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.reactive.server.multipart.MultipartFormDataInput;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.example.bean.InfoBean;
import org.keycloak.example.bean.ServerInfoBean;
import org.keycloak.example.bean.UrlBean;
import org.keycloak.example.handlers.ActionHandlerContext;
import org.keycloak.example.handlers.ActionHandlerManager;
import org.keycloak.example.util.*;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.RefreshToken;
import org.keycloak.representations.dpop.DPoP;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AccessTokenRequest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.RefreshRequest;
import org.keycloak.testsuite.util.oauth.UserInfoRequest;
import org.keycloak.testsuite.util.oauth.UserInfoResponse;
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
    private UriInfo uriInfo;

    @Context
    HttpServerRequest request;

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
        fmAttributes.put("url", new UrlBean(uriInfo));
        fmAttributes.put("clientConfigCtx", Services.instance().getSession().getClientConfigContext());
        fmAttributes.put("oidcConfigCtx", Services.instance().getSession().getOidcConfigContext());
        fmAttributes.put("oid4vciCtx", Services.instance().getSession().getOrCreateOID4VCIContext());
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
    public Response processAction(MultipartFormDataInput formData) {
        Map<String, String> params = formData.getValues().entrySet()
                .stream()
                .collect(Collectors.toMap(value -> value.getKey(), value -> value.getValue().iterator().next().getValue()));
        String action = params.get("my-action");
        SessionData session = Services.instance().getSession();
        WebRequestContext<AbstractHttpPostRequest, AccessTokenResponse> lastTokenResponse = session.getTokenRequestCtx();

        Map<String, Function<ActionHandlerContext, InfoBean>> handlerActions = new ActionHandlerManager().getAllHandlerActions();

        try {
            // TODO: Possibly replace this switch with action handlers as well...
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

                    String authRequestUrl = LoginUtil.getAuthorizationRequestUrl(oidcFlowCtx, uriInfo, null);
                    fmAttributes.put("info", new InfoBean("OIDC Authentication Request URL", authRequestUrl));
                    fmAttributes.put(Constants.AUTH_REQUEST_URL, authRequestUrl);
                    session.setAuthenticationRequestUrl(authRequestUrl);
                    break;
                case "process-fragment":
                    String authzResponseUrl = params.get("authz-response-url");
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

                                Map<String, Object> requestInfo = OAuthClientUtil.getRequestInfo(refreshedTokenResponse.getRequest());
                                info.addOutput("Refresh token request", JsonSerialization.writeValueAsPrettyString(requestInfo))
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

                                Map<String, Object> reqInfo = OAuthClientUtil.getRequestInfo(userInfo.getRequest());
                                info.addOutput("User Info request", JsonSerialization.writeValueAsPrettyString(reqInfo))
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
                    Function<ActionHandlerContext, InfoBean> actionImpl = handlerActions.get(action);
                    if (actionImpl != null) {
                        ActionHandlerContext actionCtx = new ActionHandlerContext(params, action, session, lastTokenResponse, uriInfo, fmAttributes);
                        InfoBean info = actionImpl.apply(actionCtx);
                        fmAttributes.put("info", info);
                    } else {
                        throw new MyException("Illegal action: " + action);
                    }
            }
        } catch (MyException me) {
            fmAttributes.put("info", new InfoBean("Error!", "Error when performing action. See server log for details"));
            log.error(me.getMessage(), me);
        }

        return renderHtml();
    }

    private ClientConfigContext collectClientConfigParams(Map<String, String> params, SessionData session) {
        String initToken = params.get("init-token");
        String clientAuthMethod = params.get("client-auth-method");
        boolean generateJwks = params.get("jwks") != null;
        ClientConfigContext clientCtx = new ClientConfigContext(initToken, clientAuthMethod, generateJwks);
        session.setClientConfigContext(clientCtx);
        return clientCtx;
    }

    private OIDCFlowConfigContext collectOIDCFlowConfigParams(Map<String, String> params, SessionData session) {
        boolean pkce = params.get("pkce") != null;
        boolean nonce = params.get("nonce") != null;
        boolean requestObject = params.get("request-object") != null;
        boolean useDPoP = params.get("dpop") != null;
        boolean useDPoPJKT = params.get("dpop-authz-code-binding") != null;
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
            fmAttributes.put("url", new UrlBean(uriInfo));
            return Services.instance().getFreeMarker().processTemplate(fmAttributes, "code-parser.ftl");
        }
        return handleLoginCallback(code, error, errorDescription, uriInfo.getRequestUri().toString());
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

                new ActionHandlerManager().afterTokenResponseSuccessCallback(session, tokenResponse);

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
        Map<String, Object> requestInfo = OAuthClientUtil.getRequestInfo(tokenRequest);
        info.addOutput("Token request", JsonSerialization.writeValueAsPrettyString(requestInfo))
                .addOutput("Token response", JsonSerialization.writeValueAsPrettyString(tokenResponse));
    }


    private OIDCClientRepresentation createClientToRegister(String clientAuthMethod, boolean generateJwks) {
        OIDCClientRepresentation client = new OIDCClientRepresentation();
        client.setClientName("my fapi client");
        UrlBean urls = new UrlBean(uriInfo);
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
