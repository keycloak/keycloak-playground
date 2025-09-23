package org.keycloak.example.util;

import java.security.KeyPair;

import org.jboss.logging.Logger;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.crypto.KeyUse;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.util.DPoPGenerator;
import org.keycloak.util.JWKSUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class DPoPContext {

    private static final Logger log = Logger.getLogger(DPoPContext.class);

    private KeyPair keyPair;
    private String lastDpopProof;

    public String getLastDpopProof() {
        return lastDpopProof;
    }

    public String generateDPoP(String httpMethod, String endpointUrl, String accessToken) {
        if (keyPair == null) {
            generateKeys();
        }
        lastDpopProof = DPoPGenerator.generateRsaSignedDPoPProof(keyPair, httpMethod, endpointUrl, accessToken);
        return lastDpopProof;
    }

    public String generateKeyThumbprint() {
        if (keyPair == null) {
            generateKeys();
        }

        JWK jwk = JWKBuilder.create()
                .rsa(keyPair.getPublic(), KeyUse.SIG);
        return JWKSUtils.computeThumbprint(jwk);
    }

    public void rotateKeys() {
        generateKeys();
    }

    private void generateKeys() {
        keyPair = KeyUtils.generateRsaKeyPair(2048);
        log.info("New DPoP RSA keyPair generated.");
    }
}
