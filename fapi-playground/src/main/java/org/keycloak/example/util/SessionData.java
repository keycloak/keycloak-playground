package org.keycloak.example.util;

import org.keycloak.example.Services;
import org.keycloak.example.oid4vci.OID4VCIContext;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.oidc.OIDCClientRepresentation;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.testsuite.util.oauth.PkceGenerator;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class SessionData {

    private ClientConfigContext clientConfigContext = new ClientConfigContext(null, "none", false);

    private OIDCClientRepresentation registeredClient;

    private KeysWrapper keys;

    private String authenticationRequestUrl;

    private WebRequestContext<AbstractHttpPostRequest, AccessTokenResponse> tokenRequestCtx;

    private OIDCFlowConfigContext oidcFlowConfigContext = new OIDCFlowConfigContext(false, false, false, false, false);

    private PkceGenerator pkceContext;

    private DPoPContext dpopContext;

    private OID4VCIContext oid4vciContext;

    // TODO: Cache the result...
    public OIDCConfigurationRepresentation getAuthServerInfo() {
        return Services.instance().getOauthClient().
                realm(MyConstants.REALM_NAME)
                .doWellKnownRequest();
    }

    public ClientConfigContext getClientConfigContext() {
        return clientConfigContext;
    }

    public void setClientConfigContext(ClientConfigContext clientConfigContext) {
        this.clientConfigContext = clientConfigContext;
    }

    public OIDCClientRepresentation getRegisteredClient() {
        return registeredClient;
    }

    public void setRegisteredClient(OIDCClientRepresentation registeredClient) {
        this.registeredClient = registeredClient;
    }

    public KeysWrapper getKeys() {
        return keys;
    }

    public void setKeys(KeysWrapper keys) {
        this.keys = keys;
    }

    public String getAuthenticationRequestUrl() {
        return authenticationRequestUrl;
    }

    public void setAuthenticationRequestUrl(String authenticationRequestUrl) {
        this.authenticationRequestUrl = authenticationRequestUrl;
    }

    public WebRequestContext<AbstractHttpPostRequest, AccessTokenResponse> getTokenRequestCtx() {
        return tokenRequestCtx;
    }

    public void setTokenRequestCtx(WebRequestContext<AbstractHttpPostRequest, AccessTokenResponse> tokenRequestCtx) {
        this.tokenRequestCtx = tokenRequestCtx;
    }

    public OIDCFlowConfigContext getOidcConfigContext() {
        return oidcFlowConfigContext;
    }

    public void setOidcFlowContext(OIDCFlowConfigContext oidcFlowConfigContext) {
        this.oidcFlowConfigContext = oidcFlowConfigContext;
    }

    public DPoPContext getOrCreateDpopContext() {
        if (dpopContext == null) {
            dpopContext = new DPoPContext();
        }
        return dpopContext;
    }

    public OID4VCIContext getOrCreateOID4VCIContext() {
        if (oid4vciContext == null) {
            oid4vciContext = new OID4VCIContext();
        }
        return oid4vciContext;
    }

    public PkceGenerator getPkceContext() {
        return pkceContext;
    }

    public void setPkceContext(PkceGenerator pkceContext) {
        this.pkceContext = pkceContext;
    }
}
