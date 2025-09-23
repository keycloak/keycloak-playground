package org.keycloak.example.util;

import org.keycloak.OAuth2Constants;

/**
 * Temporary class just for constants
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCLoginProtocol {

    public static final String LOGIN_PROTOCOL = "openid-connect";
    public static final String STATE_PARAM = "state";
    public static final String SCOPE_PARAM = "scope";
    public static final String AUTHORIZATION_DETAILS_PARAM = "authorization_details";
    public static final String CODE_PARAM = "code";
    public static final String RESPONSE_TYPE_PARAM = "response_type";
    public static final String GRANT_TYPE_PARAM = "grant_type";
    public static final String REDIRECT_URI_PARAM = "redirect_uri";
    public static final String POST_LOGOUT_REDIRECT_URI_PARAM = "post_logout_redirect_uri";
    public static final String CLIENT_ID_PARAM = "client_id";
    public static final String NONCE_PARAM = "nonce";
    public static final String MAX_AGE_PARAM = OAuth2Constants.MAX_AGE;
    public static final String PROMPT_PARAM = OAuth2Constants.PROMPT;
    public static final String LOGIN_HINT_PARAM = "login_hint";
    public static final String REQUEST_PARAM = "request";
    public static final String REQUEST_URI_PARAM = "request_uri";
    public static final String UI_LOCALES_PARAM = OAuth2Constants.UI_LOCALES_PARAM;
    public static final String CLAIMS_PARAM = "claims";
    public static final String ACR_PARAM = "acr_values";
    public static final String ID_TOKEN_HINT = "id_token_hint";

    public static final String LOGOUT_STATE_PARAM = "OIDC_LOGOUT_STATE_PARAM";
    public static final String LOGOUT_REDIRECT_URI = "OIDC_LOGOUT_REDIRECT_URI";
    public static final String LOGOUT_VALIDATED_ID_TOKEN_SESSION_STATE = "OIDC_LOGOUT_VALIDATED_ID_TOKEN_SESSION_STATE";
    public static final String LOGOUT_VALIDATED_ID_TOKEN_ISSUED_AT = "OIDC_LOGOUT_VALIDATED_ID_TOKEN_ISSUED_AT";

    public static final String ISSUER = "iss";

    public static final String RESPONSE_MODE_PARAM = "response_mode";

    public static final String PROMPT_VALUE_NONE = "none";
    public static final String PROMPT_VALUE_LOGIN = "login";
    public static final String PROMPT_VALUE_CONSENT = "consent";
    public static final String PROMPT_VALUE_CREATE = "create";
    public static final String PROMPT_VALUE_SELECT_ACCOUNT = "select_account";

    // Client authentication methods
    public static final String CLIENT_SECRET_BASIC = "client_secret_basic";
    public static final String CLIENT_SECRET_POST = "client_secret_post";
    public static final String CLIENT_SECRET_JWT = "client_secret_jwt";
    public static final String PRIVATE_KEY_JWT = "private_key_jwt";
    public static final String TLS_CLIENT_AUTH = "tls_client_auth";

    // https://tools.ietf.org/html/rfc7636#section-4.3
    public static final String CODE_CHALLENGE_PARAM = "code_challenge";
    public static final String CODE_CHALLENGE_METHOD_PARAM = "code_challenge_method";

    // https://tools.ietf.org/html/rfc7636#section-4.2
    public static final int PKCE_CODE_CHALLENGE_MIN_LENGTH = 43;
    public static final int PKCE_CODE_CHALLENGE_MAX_LENGTH = 128;

    // https://tools.ietf.org/html/rfc7636#section-4.1
    public static final int PKCE_CODE_VERIFIER_MIN_LENGTH = 43;
    public static final int PKCE_CODE_VERIFIER_MAX_LENGTH = 128;

    // https://tools.ietf.org/html/rfc7636#section-6.2.2
    public static final String PKCE_METHOD_PLAIN = "plain";
    public static final String PKCE_METHOD_S256 = "S256";

    // https://datatracker.ietf.org/doc/html/rfc9449#section-12.3
    public static final String DPOP_JKT = "dpop_jkt";
}
