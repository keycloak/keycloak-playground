package org.keycloak.example.util;

import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

import org.apache.http.client.RedirectStrategy;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClientBuilder;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeystoreUtil;

/**
 * Utilities for Holder of key mechanism and other Mutual TLS tests.
 *
 * @see https://issues.jboss.org/browse/KEYCLOAK-6771
 * @see https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-3
 */
public class MutualTLSUtils {

    public static final String DEFAULT_KEYSTOREPATH = System.getProperty("jboss.server.config.dir") + "/client.jks";
    public static final String DEFAULT_KEYSTOREPASSWORD = "secret";
    public static final String DEFAULT_TRUSTSTOREPATH = System.getProperty("jboss.server.config.dir") + "/keycloak.truststore";
    public static final String DEFAULT_TRUSTSTOREPASSWORD = "secret";

    public static final String OTHER_KEYSTOREPATH = System.getProperty("hok.client.certificate.keystore");
    public static final String OTHER_KEYSTOREPASSWORD = System.getProperty("hok.client.certificate.keystore.passphrase");

    // Client certificate with tricky Subject, which contains RDNs with  non-very known OID names
    // like "jurisdictionCountryName", "businessCategory", "serialNumber" . These OIDs are used by OpenBanking Brasil
    public static final String OBB_KEYSTOREPATH = System.getProperty("obb.client.certificate.keystore");
    public static final String OBB_KEYSTOREPASSWORD = System.getProperty("obb.client.certificate.keystore.passphrase");

    public static CloseableHttpClient newCloseableHttpClientWithDefaultKeyStoreAndTrustStore() {
        return newCloseableHttpClient(DEFAULT_KEYSTOREPATH, DEFAULT_KEYSTOREPASSWORD, DEFAULT_TRUSTSTOREPATH, DEFAULT_TRUSTSTOREPASSWORD);
    }

    public static CloseableHttpClient newCloseableHttpClientWithOtherKeyStoreAndTrustStore() {
        return newCloseableHttpClient(OTHER_KEYSTOREPATH, OTHER_KEYSTOREPASSWORD, DEFAULT_TRUSTSTOREPATH, DEFAULT_TRUSTSTOREPASSWORD);
    }

    public static CloseableHttpClient newCloseableHttpClientWithOBBKeyStoreAndTrustStore() {
        return newCloseableHttpClient(OBB_KEYSTOREPATH, OBB_KEYSTOREPASSWORD, DEFAULT_TRUSTSTOREPATH, DEFAULT_TRUSTSTOREPASSWORD);
    }

    public static CloseableHttpClient newCloseableHttpClientWithoutKeyStoreAndTrustStore() {
        return newCloseableHttpClient(null, null, null, null);
    }

    public static CloseableHttpClient newCloseableHttpClient(String keyStorePath, String keyStorePassword, String trustStorePath, String trustStorePassword) {

        KeyStore keystore = null;
        // Load the keystore file
        if (keyStorePath != null) {
            try {
                keystore = KeystoreUtil.loadKeyStore(keyStorePath, keyStorePassword);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // load the trustore
        KeyStore truststore = null;
        if (trustStorePath != null) {
            try {
                truststore = KeystoreUtil.loadKeyStore(trustStorePath, trustStorePassword);
            } catch(Exception e) {
                e.printStackTrace();
            }
        }

        if (keystore != null || truststore != null)
            return newCloseableHttpClientSSL(keystore, "secret", truststore, DefaultRedirectStrategy.INSTANCE);

        return HttpClientBuilder.create().build();
    }

    public static CloseableHttpClient newCloseableHttpClientSSL(KeyStore keystore, String keyStorePassword, KeyStore truststore, RedirectStrategy redirectStrategy) {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            KeyManagerFactory kmfactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmfactory.init(keystore, keyStorePassword.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(truststore);
            sslContext.init(kmfactory.getKeyManagers(), tmf.getTrustManagers(), null);
            SSLConnectionSocketFactory sf = new SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE);

            return HttpClientBuilder.create().setSSLSocketFactory(sf).setRedirectStrategy(redirectStrategy).build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException | UnrecoverableKeyException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getThumbprintFromDefaultClientCert() throws KeyStoreException, CertificateEncodingException {
        return getThumbprintFromClientCert(DEFAULT_KEYSTOREPATH, DEFAULT_KEYSTOREPASSWORD);
    }

    public static String getThumbprintFromOtherClientCert() throws KeyStoreException, CertificateEncodingException {
        return getThumbprintFromClientCert(OTHER_KEYSTOREPATH, OTHER_KEYSTOREPASSWORD);
    }

    public static String getThumbprintFromClientCert(String keyStorePath, String keyStorePassword) throws KeyStoreException, CertificateEncodingException {
        KeyStore keystore = null;
        // load the keystore containing the client certificate - keystore type is probably jks or pkcs12
        try {
            keystore = KeystoreUtil.loadKeyStore(keyStorePath, keyStorePassword);
        } catch (Exception e) {
            e.printStackTrace();
        }
        Enumeration<String> es = keystore.aliases();
        String alias = null;
        while(es.hasMoreElements()) {
            alias = es.nextElement();
        }
        X509Certificate cert = (X509Certificate) keystore.getCertificate(alias);
        String digestAlg = "SHA-256";
        byte[] DERX509Hash = cert.getEncoded();
        String DERX509Base64UrlEncoded = null;
        try {
            MessageDigest md = MessageDigest.getInstance(digestAlg);
            md.update(DERX509Hash);
            DERX509Base64UrlEncoded = Base64Url.encode(md.digest());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return DERX509Base64UrlEncoded;
    }

//    public static Response executeUserInfoRequestInGetMethod(String accessToken, boolean isKeystoreUsed, String keystorePath, String keystorePassward) {
//        ClientBuilder clientBuilder = ClientBuilder.newBuilder();
//        KeyStore keystore = null;
//        // Load the keystore file
//        if(isKeystoreUsed) {
//            try {
//                if (keystorePath != null) {
//                    keystore = KeystoreUtil.loadKeyStore(keystorePath, keystorePassward);
//                    clientBuilder.keyStore(keystore, keystorePassward);
//                } else {
//                    keystore = KeystoreUtil.loadKeyStore(DEFAULT_KEYSTOREPATH, DEFAULT_KEYSTOREPASSWORD);
//                    clientBuilder.keyStore(keystore, DEFAULT_KEYSTOREPASSWORD);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }
//        Client client = clientBuilder.build();
//        WebTarget userInfoTarget = null;
//        try {
//            userInfoTarget = UserInfoClientUtil.getUserInfoWebTarget(client);
//        } finally {
//            client.close();
//        }
//        return userInfoTarget.request().header(HttpHeaders.AUTHORIZATION, "bearer " + accessToken).get();
//    }
}
