package org.keycloak.workaround.mcp.services.clientpolicy.executor;

import org.keycloak.OAuthErrorException;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.endpoints.AuthorizationEndpoint;
import org.keycloak.representations.idm.ClientPolicyExecutorConfigurationRepresentation;
import org.keycloak.services.clientpolicy.ClientPolicyContext;
import org.keycloak.services.clientpolicy.ClientPolicyException;
import org.keycloak.services.clientpolicy.context.TokenRequestContext;
import org.keycloak.services.clientpolicy.executor.ClientPolicyExecutorProvider;

public class ResourceAudienceCheckExecutor implements ClientPolicyExecutorProvider<ClientPolicyExecutorConfigurationRepresentation> {

    protected final KeycloakSession session;

    public ResourceAudienceCheckExecutor(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public String getProviderId() {
        return ResourceAudienceCheckExecutorFactory.PROVIDER_ID;
    }

    public void executeOnEvent(ClientPolicyContext context) throws ClientPolicyException {
        switch (context.getEvent()) {
            case TOKEN_REQUEST:
                checkResourceParameterValue((TokenRequestContext) context);
                return;
            default:
                return;
        }
    }

    private void checkResourceParameterValue(TokenRequestContext context) throws ClientPolicyException {
        String resourceInTokenRequest = context.getParams().getFirst("resource");
        AuthenticatedClientSessionModel clientSession = context.getParseResult().getClientSession();
        if (clientSession == null) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_CLIENT, "client session is null");
        }
        String resourceInAuthorizationRequest = clientSession.getNote(AuthorizationEndpoint.LOGIN_SESSION_NOTE_ADDITIONAL_REQ_PARAMS_PREFIX + "resource");
        if (resourceInTokenRequest == null || !resourceInTokenRequest.equals(resourceInAuthorizationRequest)) {
            throw new ClientPolicyException(OAuthErrorException.INVALID_REQUEST, "resource parameter value in token request does not match the one in authorization request.");
        }
    }
}
