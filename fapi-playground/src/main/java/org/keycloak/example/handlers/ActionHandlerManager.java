package org.keycloak.example.handlers;

import org.keycloak.example.bean.InfoBean;
import org.keycloak.example.util.SessionData;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public class ActionHandlerManager {

    private static final List<ActionHandler> HANDLERS = List.of(
            new OID4VCIHandler() // TODO: Register all handlers here. Maybe replace with service-loader...
    );

    public Map<String, Function<ActionHandlerContext, InfoBean>> getAllHandlerActions() {
        Map<String, Function<ActionHandlerContext, InfoBean>> all = new HashMap<>();
        for (ActionHandler handler : HANDLERS) {
            for (String key : handler.getActions().keySet()) {
                if (all.containsKey(key)) {
                    throw new IllegalStateException("The key " + key + " already registered as an action. Please use different action key for the handler " + handler);
                }
            }
            all.putAll(handler.getActions());
        }
        return all;
    }

    public void afterTokenResponseSuccessCallback(SessionData session, AccessTokenResponse accessTokenResponse) {
        for (ActionHandler handler : HANDLERS) {
            handler.afterTokenResponseSuccessCallback(session, accessTokenResponse);
        }
    }
}
