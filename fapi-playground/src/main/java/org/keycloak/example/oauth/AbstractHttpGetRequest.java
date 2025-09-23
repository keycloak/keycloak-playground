package org.keycloak.example.oauth;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.keycloak.example.util.MediaType;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class AbstractHttpGetRequest<R> {

    protected final AbstractOAuthClient<?> client;

    private HttpGet get;

    private Map<String, String> headers = new HashMap<>(); // Just for logging purpose

    public AbstractHttpGetRequest(AbstractOAuthClient<?> client) {
        this.client = client;
    }

    protected abstract String getEndpoint();

    protected abstract void initRequest();

    public R send() {
        get = new HttpGet(getEndpoint());
        header("Accept", MediaType.APPLICATION_JSON);

        initRequest();
        try {
            return toResponse(client.httpClient().get().execute(get));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void header(String name, String value) {
        if (value != null) {
            get.addHeader(name, value);
            headers.put(name, value);
        }
    }

    protected abstract R toResponse(CloseableHttpResponse response) throws IOException;

    public Map<String, Object> getRequestInfo() {
        Map<String, Object> request = new HashMap<>();
        request.put("endpoint", getEndpoint());
        request.put("Headers", this.headers);
     //    request.put("Params", this.parameters.stream().collect(Collectors.toMap(nvp -> nvp.getName(), nvp -> nvp.getValue()))); // TODO: if needed
        return request;
    }

}
