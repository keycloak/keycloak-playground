package org.keycloak.example.util;

import jakarta.ws.rs.core.UriInfo;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.common.util.Time;
import org.keycloak.example.Services;
import org.keycloak.example.bean.AuthorizationEndpointRequestObject;
import org.keycloak.example.bean.UrlBean;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testsuite.util.oauth.LoginUrlBuilder;
import org.keycloak.testsuite.util.oauth.PkceGenerator;

import java.util.function.Consumer;

public class LoginUtil {

    public static String getAuthorizationRequestUrl(OIDCFlowConfigContext oidcFlowCtx, UriInfo uriInfo, String scope) {
        OAuthClient oauthClient = Services.instance().getOauthClient();
        SessionData session = Services.instance().getSession();

        oauthClient.scope(scope);

        OIDCClientRepresentation oidcClient = session.getRegisteredClient();
        if (oidcClient == null) {
            throw new MyException("Not client registered. Please register client first");
        }

        String dpopJkt = oidcFlowCtx.isUseDPoPAuthzCodeBinding() ? session.getOrCreateDpopContext().generateKeyThumbprint() : null;

        if (oidcFlowCtx.isUseRequestObject()) {
            AuthorizationEndpointRequestObject requestObject = createValidRequestObjectForSecureRequestObjectExecutor(oidcClient.getClientId(), oidcFlowCtx.isUseNonce(), uriInfo);
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

    private static void setPkceInAuthenticationRequest(SessionData session, OIDCFlowConfigContext oidcFlowCtx, Consumer<PkceGenerator> pkceConsumer1, Runnable pkceCleaner) {
        if (oidcFlowCtx.isUsePkce()) {
            PkceGenerator pkce = PkceGenerator.s256();
            session.setPkceContext(pkce);
            pkceConsumer1.accept(pkce);
        } else {
            session.setPkceContext(null);
            pkceCleaner.run();
        }
    }

    private static AuthorizationEndpointRequestObject createValidRequestObjectForSecureRequestObjectExecutor(String clientId, boolean nonce, UriInfo uriInfo) {
        SessionData session = Services.instance().getSession();

        AuthorizationEndpointRequestObject requestObject = new AuthorizationEndpointRequestObject();
        requestObject.id(UUIDUtil.generateId());
        requestObject.iat(Long.valueOf(Time.currentTime()));
        requestObject.exp(requestObject.getIat() + Long.valueOf(300));
        requestObject.nbf(requestObject.getIat());
        requestObject.setClientId(clientId);
        requestObject.setResponseType("code id_token");
        requestObject.setRedirectUriParam(new UrlBean(uriInfo).getClientRedirectUri());
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
}
