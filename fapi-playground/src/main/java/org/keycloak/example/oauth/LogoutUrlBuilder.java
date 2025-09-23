package org.keycloak.example.oauth;

import org.keycloak.OAuth2Constants;
import org.keycloak.example.util.OIDCLoginProtocol;

public class LogoutUrlBuilder extends AbstractUrlBuilder {

    LogoutUrlBuilder(AbstractOAuthClient<?> client) {
        super(client);
    }

    @Override
    public String getEndpoint() {
        return client.getEndpoints().getLogout();
    }

    public LogoutUrlBuilder param(String name, String value) {
        parameter(name, value);
        return this;
    }

    public LogoutUrlBuilder idTokenHint(String idTokenHint) {
        parameter(OAuth2Constants.ID_TOKEN_HINT, idTokenHint);
        return this;
    }

    public LogoutUrlBuilder postLogoutRedirectUri(String redirectUri) {
        parameter(OIDCLoginProtocol.POST_LOGOUT_REDIRECT_URI_PARAM, redirectUri);
        return this;
    }

    public LogoutUrlBuilder state(String state) {
        parameter(OAuth2Constants.STATE, state);
        return this;
    }

    public LogoutUrlBuilder uiLocales(String uiLocales) {
        parameter(OAuth2Constants.UI_LOCALES_PARAM, uiLocales);
        return this;
    }

//    public LogoutUrlBuilder initiatingIdp(String initiatingIdp) {
//        parameter(AuthenticationManager.INITIATING_IDP_PARAM, initiatingIdp);
//        return this;
//    }

    public LogoutUrlBuilder withClientId() {
        parameter(OAuth2Constants.CLIENT_ID, client.config().getClientId());
        return this;
    }

    public LogoutUrlBuilder withRedirect() {
        postLogoutRedirectUri(client.config().getPostLogoutRedirectUri());
        return this;
    }

    @Override
    protected void initRequest() {
    }

}
