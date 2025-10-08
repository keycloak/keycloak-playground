package org.keycloak.example.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.keycloak.OAuth2Constants;
import org.keycloak.constants.AdapterConstants;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.grants.ciba.CibaGrantType;
import org.keycloak.representations.JsonWebToken;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class AuthorizationEndpointRequestObject extends JsonWebToken {

    @JsonProperty(OIDCLoginProtocol.CLIENT_ID_PARAM)
    String clientId;

    @JsonProperty(OIDCLoginProtocol.RESPONSE_TYPE_PARAM)
    String responseType;

    @JsonProperty(OIDCLoginProtocol.RESPONSE_MODE_PARAM)
    String responseMode;

    @JsonProperty(OIDCLoginProtocol.REDIRECT_URI_PARAM)
    String redirectUriParam;

    @JsonProperty(OIDCLoginProtocol.STATE_PARAM)
    String state;

    @JsonProperty(OIDCLoginProtocol.SCOPE_PARAM)
    String scope;

    @JsonProperty(OIDCLoginProtocol.LOGIN_HINT_PARAM)
    String loginHint;

    @JsonProperty(OIDCLoginProtocol.PROMPT_PARAM)
    String prompt;

    @JsonProperty(OIDCLoginProtocol.NONCE_PARAM)
    String nonce;

    Integer max_age;

    @JsonProperty(OIDCLoginProtocol.UI_LOCALES_PARAM)
    String uiLocales;

    @JsonProperty(OIDCLoginProtocol.ACR_PARAM)
    String acr;

    @JsonProperty(OAuth2Constants.DISPLAY)
    String display;

    @JsonProperty(OIDCLoginProtocol.CODE_CHALLENGE_PARAM)
    String codeChallenge;

    @JsonProperty(OIDCLoginProtocol.CODE_CHALLENGE_METHOD_PARAM)
    String codeChallengeMethod;

    @JsonProperty(AdapterConstants.KC_IDP_HINT)
    String idpHint;

    @JsonProperty(Constants.KC_ACTION)
    String action;

    // CIBA

    @JsonProperty(CibaGrantType.CLIENT_NOTIFICATION_TOKEN)
    String clientNotificationToken;

    @JsonProperty(CibaGrantType.LOGIN_HINT_TOKEN)
    String loginHintToken;

    @JsonProperty(OIDCLoginProtocol.ID_TOKEN_HINT)
    String idTokenHint;

    @JsonProperty(CibaGrantType.USER_CODE)
    String userCode;

    @JsonProperty(CibaGrantType.BINDING_MESSAGE)
    String bindingMessage;

    Integer requested_expiry;

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId =  clientId;
    }

    public String getResponseType() {
        return responseType;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public void setResponseMode(String responseMode) {
        this.responseMode = responseMode;
    }

    public String getRedirectUriParam() {
        return redirectUriParam;
    }

    public void setRedirectUriParam(String redirectUriParam) {
        this.redirectUriParam = redirectUriParam;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getLoginHint() {
        return loginHint;
    }

    public void setLoginHint(String loginHint) {
        this.loginHint = loginHint;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public Integer getMax_age() {
        return max_age;
    }

    public void setMax_age(Integer max_age) {
        this.max_age = max_age;
    }

    public String getUiLocales() {
        return uiLocales;
    }

    public void setUiLocales(String uiLocales) {
        this.uiLocales = uiLocales;
    }

    public String getAcr() {
        return acr;
    }

    public void setAcr(String acr) {
        this.acr = acr;
    }

    public String getCodeChallenge() {
        return codeChallenge;
    }

    public void setCodeChallenge(String codeChallenge) {
        this.codeChallenge = codeChallenge;
    }

    public String getCodeChallengeMethod() {
        return codeChallengeMethod;
    }

    public void setCodeChallengeMethod(String codeChallengeMethod) {
        this.codeChallengeMethod = codeChallengeMethod;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getIdpHint() {
        return idpHint;
    }

    public void setIdpHint(String idpHint) {
        this.idpHint = idpHint;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getClientNotificationToken() {
        return clientNotificationToken;
    }

    public void setClientNotificationToken(String clientNotificationToken) {
        this.clientNotificationToken = clientNotificationToken;
    }

    public String getLoginHintToken() {
        return loginHintToken;
    }

    public void setLoginHintToken(String loginHintToken) {
        this.loginHintToken = loginHintToken;
    }

    public String getIdTokenHint() {
        return idTokenHint;
    }

    public void setIdTokenHint(String idTokenHint) {
        this.idTokenHint = idTokenHint;
    }

    public String getBindingMessage() {
        return bindingMessage;
    }

    public void setBindingMessage(String bindingMessage) {
        this.bindingMessage = bindingMessage;
    }

    public String getUserCode() {
        return userCode;
    }

    public void setUserCode(String userCode) {
        this.userCode = userCode;
    }

    public Integer getRequested_expiry() {
        return requested_expiry;
    }

    public void setRequested_expiry(Integer requested_expiry) {
        this.requested_expiry = requested_expiry;
    }

}
