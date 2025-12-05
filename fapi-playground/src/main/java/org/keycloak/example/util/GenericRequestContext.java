package org.keycloak.example.util;

import java.util.Map;

public class GenericRequestContext {

    private final String endpoint;
    private final Map<String, String> headers;
    private final Map<String, String> params;
    private final String body;

    public GenericRequestContext(String endpoint, Map<String, String> headers, Map<String, String> params) {
        this.endpoint = endpoint;
        this.headers = headers;
        this.params = params;
        this.body = null;
    }

    public GenericRequestContext(String endpoint, Map<String, String> headers, String body) {
        this.endpoint = endpoint;
        this.headers = headers;
        this.params = null;
        this.body = body;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public String getBody() {
        return body;
    }
}
