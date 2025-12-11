package org.keycloak.example.handlers;

import org.keycloak.example.bean.InfoBean;
import org.keycloak.example.util.SessionData;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;

import java.util.Map;
import java.util.function.Function;

/**
 * Ability to handle some actions from the UI
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ActionHandler {

    Map<String, Function<ActionHandlerContext, InfoBean>> getActions();

    /**
     * Called once user authenticated and being redirected from KC to the app
     *
     */
    void onAuthenticationCallback(SessionData session, AccessTokenResponse accessTokenResponse);

    /**
     * Called before logout (right before user is redirected to the logout URL). Opportunity to cleanup some context data etc.
     *
     */
    void onLogoutCallback(SessionData session);
}
