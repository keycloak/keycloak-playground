package org.keycloak.example.util;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.QueryParam;
import org.keycloak.common.util.KeyUtils;
import org.keycloak.common.util.PemUtils;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.AsymmetricSignatureSignerContext;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.example.bean.AuthorizationEndpointRequestObject;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.JWSBuilder;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class KeysWrapper {

    public static final String PRIVATE_KEY = "privateKey";
    public static final String PUBLIC_KEY = "publicKey";

    private final OIDCClientData clientData = new OIDCClientData();

    public Map<String, String> generateKeys(@QueryParam("jwaAlgorithm") String jwaAlgorithm,
                                            @QueryParam("advertiseJWKAlgorithm") Boolean advertiseJWKAlgorithm) {
        try {
            KeyPair keyPair = null;
            KeyUse keyUse = KeyUse.SIG;
            if (jwaAlgorithm == null) jwaAlgorithm = Algorithm.RS256;
            String keyType = null;

            switch (jwaAlgorithm) {
                case Algorithm.RS256:
                case Algorithm.RS384:
                case Algorithm.RS512:
                case Algorithm.PS256:
                case Algorithm.PS384:
                case Algorithm.PS512:
                    keyType = KeyType.RSA;
                    keyPair = KeyUtils.generateRsaKeyPair(2048);
                    break;
//                case Algorithm.ES256:
//                    keyType = KeyType.EC;
//                    keyPair = generateEcdsaKey("secp256r1");
//                    break;
//                case Algorithm.ES384:
//                    keyType = KeyType.EC;
//                    keyPair = generateEcdsaKey("secp384r1");
//                    break;
//                case Algorithm.ES512:
//                    keyType = KeyType.EC;
//                    keyPair = generateEcdsaKey("secp521r1");
//                    break;
//                case JWEConstants.RSA1_5:
//                case JWEConstants.RSA_OAEP:
//                case JWEConstants.RSA_OAEP_256:
//                    // for JWE KEK Key Encryption
//                    keyType = KeyType.RSA;
//                    keyUse = KeyUse.ENC;
//                    keyPair = KeyUtils.generateRsaKeyPair(2048);
//                    break;
                default :
                    throw new RuntimeException("Unsupported signature algorithm");
            }

            clientData.setKeyPair(keyPair);
            clientData.setKeyType(keyType);
            if (advertiseJWKAlgorithm == null || Boolean.TRUE.equals(advertiseJWKAlgorithm)) {
                clientData.setKeyAlgorithm(jwaAlgorithm);
            } else {
                clientData.setKeyAlgorithm(null);
            }
            clientData.setKeyUse(keyUse);
        } catch (Exception e) {
            throw new BadRequestException("Error generating signing keypair", e);
        }
        return getKeysAsPem();
    }

    private Map<String, String> getKeysAsPem() {
        String privateKeyPem = PemUtils.encodeKey(clientData.getSigningKeyPair().getPrivate());
        String publicKeyPem = PemUtils.encodeKey(clientData.getSigningKeyPair().getPublic());

        Map<String, String> res = new HashMap<>();
        res.put(PRIVATE_KEY, privateKeyPem);
        res.put(PUBLIC_KEY, publicKeyPem);
        return res;
    }

    public JSONWebKeySet getJwks() {
        JSONWebKeySet keySet = new JSONWebKeySet();
        KeyPair keyPair = clientData.getKeyPair();
        String keyAlgorithm = clientData.getKeyAlgorithm();
        String keyType = clientData.getKeyType();
        KeyUse keyUse = clientData.getKeyUse();

        if (keyPair == null) {
            keySet.setKeys(new JWK[] {});
        } else if (KeyType.RSA.equals(keyType)) {
            keySet.setKeys(new JWK[] { JWKBuilder.create().algorithm(keyAlgorithm).rsa(keyPair.getPublic(), keyUse) });
        } else if (KeyType.EC.equals(keyType)) {
            keySet.setKeys(new JWK[] { JWKBuilder.create().algorithm(keyAlgorithm).ec(keyPair.getPublic()) });
        } else {
            keySet.setKeys(new JWK[] {});
        }

        return keySet;

    }

    public String getOidcRequest(AuthorizationEndpointRequestObject oidcRequest, String jwaAlgorithm) {
        if ("none".equals(jwaAlgorithm)) {
            return new JWSBuilder().jsonContent(oidcRequest).none();
        } else if (clientData.getSigningKeyPair() == null) {
            throw new MyException("signing key not set");
        } else {
            PrivateKey privateKey = clientData.getSigningKeyPair().getPrivate();
            String kid = KeyUtils.createKeyId(clientData.getSigningKeyPair().getPublic());
            KeyWrapper keyWrapper = new KeyWrapper();
            keyWrapper.setAlgorithm(clientData.getSigningKeyAlgorithm());
            keyWrapper.setKid(kid);
            keyWrapper.setPrivateKey(privateKey);
            SignatureSignerContext signer;
            switch (clientData.getSigningKeyAlgorithm()) {
//                case Algorithm.ES256:
//                case Algorithm.ES384:
//                case Algorithm.ES512:
//                    signer = new ServerECDSASignatureSignerContext(keyWrapper);
//                    break;
                default:
                    signer = new AsymmetricSignatureSignerContext(keyWrapper);
            }
            return new JWSBuilder().kid(kid).jsonContent(oidcRequest).sign(signer);
        }
    }

    public static class OIDCClientData {

        private KeyPair keyPair;
        private String oidcRequest;
        private List<String> sectorIdentifierRedirectUris;
        private String keyType = KeyType.RSA;
        private String keyAlgorithm;
        private KeyUse keyUse = KeyUse.SIG;

        public KeyPair getSigningKeyPair() {
            return keyPair;
        }

        public void setSigningKeyPair(KeyPair signingKeyPair) {
            this.keyPair = signingKeyPair;
        }

        public String getOidcRequest() {
            return oidcRequest;
        }

        public void setOidcRequest(String oidcRequest) {
            this.oidcRequest = oidcRequest;
        }

        public List<String> getSectorIdentifierRedirectUris() {
            return sectorIdentifierRedirectUris;
        }

        public void setSectorIdentifierRedirectUris(List<String> sectorIdentifierRedirectUris) {
            this.sectorIdentifierRedirectUris = sectorIdentifierRedirectUris;
        }

        public String getSigningKeyType() {
            return keyType;
        }

        public void setSigningKeyType(String signingKeyType) {
            this.keyType = signingKeyType;
        }

        public String getSigningKeyAlgorithm() {
            return keyAlgorithm;
        }

        public void setSigningKeyAlgorithm(String signingKeyAlgorithm) {
            this.keyAlgorithm = signingKeyAlgorithm;
        }

        public KeyPair getKeyPair() {
            return keyPair;
        }

        public void setKeyPair(KeyPair keyPair) {
            this.keyPair = keyPair;
        }

        public String getKeyType() {
            return keyType;
        }

        public void setKeyType(String keyType) {
            this.keyType = keyType;
        }

        public String getKeyAlgorithm() {
            return keyAlgorithm;
        }

        public void setKeyAlgorithm(String keyAlgorithm) {
            this.keyAlgorithm = keyAlgorithm;
        }

        public KeyUse getKeyUse() {
            return keyUse;
        }

        public void setKeyUse(KeyUse keyUse) {
            this.keyUse = keyUse;
        }
    }
}
