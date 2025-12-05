package org.keycloak.example.handlers;

import org.keycloak.example.bean.InfoBean;

import java.util.Map;
import java.util.function.Function;

/**
 * Ability to handle some actions from the UI
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public interface ActionHandler {

    Map<String, Function<ActionHandlerContext, InfoBean>> getActions();
}
