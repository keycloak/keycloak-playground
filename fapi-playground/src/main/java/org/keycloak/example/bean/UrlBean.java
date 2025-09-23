package org.keycloak.example.bean;

import org.jboss.resteasy.spi.HttpRequest;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.example.Services;
import org.keycloak.example.util.MyConstants;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class UrlBean {

    private final String baseUrl;

    public UrlBean() {
        String baseUrl = ResteasyProviderFactory.getInstance().getContextData(HttpRequest.class).getUri().getBaseUri().toString();
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

    public String getAccountConsoleUrl() {
        return MyConstants.SERVER_ROOT + "/realms/" + MyConstants.REALM_NAME + "/account?referrer=" + baseUrl;
    }
}
