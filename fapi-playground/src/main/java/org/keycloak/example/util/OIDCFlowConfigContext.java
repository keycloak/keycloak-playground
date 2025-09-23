package org.keycloak.example.util;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCFlowConfigContext {

    private final boolean usePkce;
    private final boolean useNonce;
    private final boolean useRequestObject;
    private final boolean useDPoP;
    private final boolean useDPoPAuthzCodeBinding;

    public OIDCFlowConfigContext(boolean usePkce, boolean useNonce, boolean useRequestObject, boolean useDPoP, boolean useDPoPAuthzCodeBinding) {
        this.usePkce = usePkce;
        this.useNonce = useNonce;
        this.useRequestObject = useRequestObject;
        this.useDPoP = useDPoP;
        this.useDPoPAuthzCodeBinding = useDPoPAuthzCodeBinding;
    }


    public boolean isUsePkce() {
        return usePkce;
    }

    public boolean isUseNonce() {
        return useNonce;
    }

    public boolean isUseRequestObject() {
        return useRequestObject;
    }

    public boolean isUseDPoP() {
        return useDPoP;
    }

    public boolean isUseDPoPAuthzCodeBinding() {
        return useDPoPAuthzCodeBinding;
    }
}
