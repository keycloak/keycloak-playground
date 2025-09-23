package org.keycloak.example.oauth;

import org.keycloak.OAuth2Constants;
import org.keycloak.example.util.Constants;
import org.keycloak.representations.ClaimsRepresentation;

public class LoginUrlBuilder extends AbstractUrlBuilder {

    public LoginUrlBuilder(AbstractOAuthClient<?> client) {
        super(client);
    }

    @Override
    public String getEndpoint() {
        return client.getEndpoints().getAuthorization();
    }

    public LoginUrlBuilder param(String name, String value) {
        parameter(name, value);
        return this;
    }

    public LoginUrlBuilder state(String state) {
        parameter(OAuth2Constants.STATE, state);
        return this;
    }

    public LoginUrlBuilder nonce(String nonce) {
        parameter("nonce", nonce);
        return this;
    }

    public LoginUrlBuilder prompt(String prompt) {
        parameter(OAuth2Constants.PROMPT, prompt);
        return this;
    }

    public LoginUrlBuilder loginHint(String loginHint) {
        parameter("login_hint", loginHint);
        return this;
    }

    public LoginUrlBuilder uiLocales(String uiLocales) {
        parameter(OAuth2Constants.UI_LOCALES_PARAM, uiLocales);
        return this;
    }

    public LoginUrlBuilder maxAge(int maxAge) {
        parameter(OAuth2Constants.MAX_AGE, Integer.toString(maxAge));
        return this;
    }

    public LoginUrlBuilder kcAction(String kcAction) {
        parameter(Constants.KC_ACTION, kcAction);
        return this;
    }

    public LoginUrlBuilder codeChallenge(PkceGenerator pkceGenerator) {
        if (pkceGenerator != null) {
            codeChallenge(pkceGenerator.getCodeChallenge(), pkceGenerator.getCodeChallengeMethod());
        }
        return this;
    }

    public LoginUrlBuilder codeChallenge(String codeChallenge, String codeChallengeMethod) {
        parameter(OAuth2Constants.CODE_CHALLENGE, codeChallenge);
        parameter(OAuth2Constants.CODE_CHALLENGE_METHOD, codeChallengeMethod);
        return this;
    }

    public LoginUrlBuilder dpopJkt(String dpopJkt) {
        if (dpopJkt != null) {
            parameter(Constants.DPOP_JKT, dpopJkt);
        }
        return this;
    }

    public LoginUrlBuilder claims(ClaimsRepresentation claims) {
        parameter(Constants.CLAIMS, claims);
        return this;
    }

    public LoginUrlBuilder request(String request) {
        parameter(Constants.REQUEST, request);
        return this;
    }

    public LoginUrlBuilder requestUri(String requestUri) {
        parameter(Constants.REQUEST_URI, requestUri);
        return this;
    }

    @Override
    protected void initRequest() {
        parameter(OAuth2Constants.RESPONSE_TYPE, client.config().getResponseType());
        parameter(Constants.RESPONSE_MODE, client.config().getResponseMode());
        parameter(OAuth2Constants.CLIENT_ID, client.config().getClientId());
        parameter(OAuth2Constants.REDIRECT_URI, client.config().getRedirectUri());

        parameter(OAuth2Constants.SCOPE, client.config().getScope());
    }

//    public AuthorizationEndpointResponse doLogin(String username, String password) {
//        open();
//        client.fillLoginForm(username, password);
//        return client.parseLoginResponse();
//    }

}
