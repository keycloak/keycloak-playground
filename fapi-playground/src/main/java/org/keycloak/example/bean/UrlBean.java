package org.keycloak.example.bean;

import jakarta.ws.rs.core.UriInfo;
import org.keycloak.example.util.MyConstants;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UrlBean {

    private final String baseUrl;

    public UrlBean(UriInfo uriInfo) {
        String baseUrl = uriInfo.getBaseUri().toString();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        this.baseUrl = baseUrl;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getAction() {
        return baseUrl + "/action";
    }

    public String getClientRedirectUri() {
        return baseUrl + "/login-callback";
    }

    public String getCssUrl() {
        return baseUrl + "/styles.css";
    }

    public String getAccountConsoleUrl() {
        return MyConstants.SERVER_ROOT + "/realms/" + MyConstants.REALM_NAME + "/account?referrer=" + baseUrl;
    }
}
