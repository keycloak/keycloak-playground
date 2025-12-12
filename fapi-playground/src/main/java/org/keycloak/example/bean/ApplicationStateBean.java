package org.keycloak.example.bean;

import org.keycloak.example.util.SessionData;

public class ApplicationStateBean {

    private final SessionData session;

    public ApplicationStateBean(SessionData session) {
        this.session = session;
    }

    public boolean isClientRegistered() {
        return session.getRegisteredClient() != null;
    }


    public boolean isAuthenticated() {
        return session.getTokenRequestCtx() != null;
    }

    public boolean isDpopProofAvailable() {
        return session.getOrCreateDpopContext().getLastDpopProof() != null;
    }
}
