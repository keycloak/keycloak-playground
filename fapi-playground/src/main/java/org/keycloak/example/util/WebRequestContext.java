package org.keycloak.example.util;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class WebRequestContext<REQ, RES> {

    private final REQ request;
    private final RES response;

    public WebRequestContext(REQ request, RES response) {
        this.request = request;
        this.response = response;
    }

    public REQ getRequest() {
        return request;
    }

    public RES getResponse() {
        return response;
    }
}
