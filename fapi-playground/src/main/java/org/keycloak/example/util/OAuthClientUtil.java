package org.keycloak.example.util;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.keycloak.testsuite.util.oauth.AbstractHttpGetRequest;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;

/**
 * // TODO: Put this to Keycloak directly (likely to AbstractHttpPost and AbstractHttpGet) and skip using those methods to avoid reflection...
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuthClientUtil {

    public static Map<String, Object> getRequestInfo(AbstractHttpPostRequest postRequest) {
        Map<String, Object> request = new HashMap<>();
        // String endpoint = postRequest.getEndpoint();
        String endpoint = invokeMethod(postRequest, AbstractHttpPostRequest.class, "getEndpoint");
        request.put("endpoint", endpoint);

        // String headers = postRequest.headers;
        Map<String, String> headers = (Map<String, String>) getField(postRequest, AbstractHttpPostRequest.class, "headers");

        request.put("Headers", headers);

        // List<NameValuePair> parameters = postRequest.parameters;
        List<NameValuePair> parameters = (List<NameValuePair>) getField(postRequest, AbstractHttpPostRequest.class, "parameters");

        request.put("Params", parameters.stream().collect(Collectors.toMap(nvp -> nvp.getName(), nvp -> nvp.getValue())));
        return request;
    }

    public static Map<String, Object> getRequestInfo(AbstractHttpGetRequest getRequest) {
        Map<String, Object> request = new HashMap<>();

        // String endpoint = getRequest.getEndpoint();
        String endpoint = invokeMethod(getRequest, AbstractHttpGetRequest.class, "getEndpoint");

        request.put("endpoint", endpoint);

        HttpGet apacheGet = (HttpGet) getField(getRequest, AbstractHttpGetRequest.class, "get");
        Map<String, String> headers = Stream.of(apacheGet.getAllHeaders())
                        .collect(Collectors.toMap(header -> header.getName(), header -> header.getValue()));
        request.put("Headers", headers);

        //    request.put("Params", this.parameters.stream().collect(Collectors.toMap(nvp -> nvp.getName(), nvp -> nvp.getValue()))); // TODO: if needed
        return request;
    }

    private static String invokeMethod(Object o, Class clazz, String methodName) {
        try {
            Method m = clazz.getDeclaredMethod(methodName);
            m.setAccessible(true);
            return (String) m.invoke(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static Object getField(Object o, Class clazz, String fieldName) {
        try {
            Field f = clazz.getDeclaredField(fieldName);
            f.setAccessible(true);
            return f.get(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
