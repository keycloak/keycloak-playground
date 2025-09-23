package org.keycloak.example.util;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.SecretGenerator;
import org.keycloak.crypto.HashException;
import org.keycloak.jose.jws.crypto.HashUtils;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class PkceUtils {

    private static final Pattern VALID_CODE_VERIFIER_PATTERN = Pattern.compile("^[0-9a-zA-Z\\-\\.~_]+$");

    public static String generateCodeVerifier() {
        return Base64Url.encode(SecretGenerator.getInstance().randomBytes(64));
    }

    public static String encodeCodeChallenge(String codeVerifier, String codeChallengeMethod) {
        try {
            switch (codeChallengeMethod) {
                case OAuth2Constants.PKCE_METHOD_S256:
                    return generateS256CodeChallenge(codeVerifier);
                case OAuth2Constants.PKCE_METHOD_PLAIN:
                    // fall-trhough
                default:
                    return codeVerifier;
            }
        } catch(Exception ex) {
            return null;
        }
    }

    // https://tools.ietf.org/html/rfc7636#section-4.6
    public static String generateS256CodeChallenge(String codeVerifier) throws HashException {
        return HashUtils.sha256UrlEncodedHash(codeVerifier, StandardCharsets.ISO_8859_1);
    }
}
