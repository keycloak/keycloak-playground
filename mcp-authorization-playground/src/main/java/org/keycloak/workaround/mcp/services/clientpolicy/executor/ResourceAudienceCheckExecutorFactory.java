package org.keycloak.workaround.mcp.services.clientpolicy.executor;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProviderFactory;

import java.util.Collections;
import java.util.List;

public class ResourceAudienceCheckExecutorFactory implements ClientPolicyExecutorProviderFactory {

    public static final String PROVIDER_ID = "resource-audience-checker";

    @Override
    public ClientPolicyExecutorProvider create(KeycloakSession session) {
        return new ResourceAudienceCheckExecutor(session);
    }

    @Override
    public void init(Config.Scope config) {
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public String getHelpText() {
        return "On a token endpoint, this executor checks if the value of a resource parameter in a token request matches the one sent in its authorization request. If not, it denies its request.";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return Collections.emptyList();
    }
}
