package org.keycloak.example.util;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientConfigContext {

    private final String initialAccessToken;
    private final String clientAuthMethod;
    private final boolean generateJwks;

    public ClientConfigContext(String initialAccessToken, String clientAuthMethod, boolean generateJwks) {
        this.initialAccessToken = initialAccessToken;
        this.clientAuthMethod = clientAuthMethod;
        this.generateJwks = generateJwks;
    }

    public String getInitialAccessToken() {
        return initialAccessToken;
    }

    public String getClientAuthMethod() {
        return clientAuthMethod;
    }

    public boolean isGenerateJwks() {
        return generateJwks;
    }
}
