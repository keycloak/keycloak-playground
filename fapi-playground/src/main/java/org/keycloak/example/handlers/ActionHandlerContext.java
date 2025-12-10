package org.keycloak.example.handlers;

import jakarta.ws.rs.core.UriInfo;
import org.keycloak.example.util.SessionData;
import org.keycloak.example.util.WebRequestContext;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import java.util.Map;

public class ActionHandlerContext {

    private final Map<String, String> params;
    private final String action;
    private final SessionData session;
    private final WebRequestContext<AbstractHttpPostRequest, AccessTokenResponse> lastTokenResponse;
    private final UriInfo uriInfo;
    private final Map<String, Object> fmAttributes;

    public ActionHandlerContext(Map<String, String> params, String action, SessionData session, WebRequestContext<AbstractHttpPostRequest, AccessTokenResponse> lastTokenResponse,
                                UriInfo uriInfo, Map<String, Object> fmAttributes) {
        this.params = params;
        this.action = action;
        this.session = session;
        this.lastTokenResponse = lastTokenResponse;
        this.uriInfo = uriInfo;
        this.fmAttributes = fmAttributes;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getAction() {
        return action;
    }

    public SessionData getSession() {
        return session;
    }

    public WebRequestContext<AbstractHttpPostRequest, AccessTokenResponse> getLastTokenResponse() {
        return lastTokenResponse;
    }

    public UriInfo getUriInfo() {
        return uriInfo;
    }

    public Map<String, Object> getFmAttributes() {
        return fmAttributes;
    }
}
