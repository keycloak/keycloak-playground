package org.keycloak.example.util;

import org.keycloak.client.registration.Auth;
import org.keycloak.client.registration.ClientRegistration;
import org.keycloak.client.registration.ClientRegistrationException;
import org.keycloak.representations.oidc.OIDCClientRepresentation;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class ClientRegistrationWrapper {

    private final ClientRegistration reg;

    private ClientRegistrationWrapper(ClientRegistration reg) {
        this.reg = reg;
    }

    public static ClientRegistrationWrapper create() {
        ClientRegistration reg = ClientRegistration.create()
                .url(MyConstants.SERVER_ROOT, MyConstants.REALM_NAME)
                .httpClient(MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore())
                .build();
        return new ClientRegistrationWrapper(reg);
    }

    public void close() {
        try {
            reg.close();
        } catch (ClientRegistrationException ex) {
            throw new MyException("Exception when closing client registration client", ex);
        }
    }

    public void setInitToken(String initToken) {
        reg.auth(Auth.token(initToken));
    }

    public WebRequestContext<OIDCClientRepresentation, OIDCClientRepresentation> registerClient(OIDCClientRepresentation client) {
        try {
            OIDCClientRepresentation response = reg.oidc().create(client);
            return new WebRequestContext<>(client, response);
        } catch (ClientRegistrationException cre) {
            throw new MyException("Failed to register client", cre);
        }
    }

}
