package org.keycloak.example.oauth;

// TODO:mposolda those should not be hardcoded, but ideally downloaded from OIDC welll-known endpoint
public class Endpoints {

    private final String baseUrl;
    private final String realm;

    public Endpoints(String baseUrl, String realm) {
        this.baseUrl = baseUrl;
        this.realm = realm;
    }

    public String getOpenIDConfiguration() {
        return getRealmUrl() + "/.well-known/openid-configuration";
    }

    public String getIssuer() {
        return getRealmUrl();
    }

    private String getProtocolUrl() {
        return getRealmUrl() + "/protocol/openid-connect";
    }

    public String getAuthorization() {
        return getProtocolUrl() + "/auth";
    }

//    public String getRegistration() {
//        return asString(OIDCLoginProtocolService.registrationsUrl(getBase()));
//    }

    public String getToken() {
        return getProtocolUrl() + "/token";
    }

    public String getIntrospection() {
        return getToken() + "/introspect";
    }

    public String getRevocation() {
        return getProtocolUrl() + "/revoke";
    }

    public String getUserInfo() {
        return getProtocolUrl() + "/userinfo";
    }

    public String getJwks() {
        return getProtocolUrl() + "/certs";
    }
//
//    public String getDeviceAuthorization() {
//        return asString(DeviceGrantType.oauth2DeviceAuthUrl(getBase()));
//    }

    public String getPushedAuthorizationRequest() {
        return getProtocolUrl() + "/ext/par/request";
    }

    public String getLogout() {
        return getProtocolUrl() + "/logout";
    }

//    public String getBackChannelLogout() {
//        return asString(OIDCLoginProtocolService.logoutUrl(getBase()).path("/backchannel-logout"));
//    }
//
//    public String getBackchannelAuthentication() {
//        return asString(CibaGrantType.authorizationUrl(getBase()));
//    }
//
//    public String getBackchannelAuthenticationCallback() {
//        return asString(CibaGrantType.authenticationUrl(getBase()));
//    }

    String getBase() {
        return baseUrl;
    }

    String getRealmUrl() {
        return baseUrl + "/realms/" + realm;
    }

}
