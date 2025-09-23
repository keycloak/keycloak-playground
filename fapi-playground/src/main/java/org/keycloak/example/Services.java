package org.keycloak.example;

import org.keycloak.common.crypto.CryptoIntegration;
import org.keycloak.example.util.FreeMarkerUtil;
import org.keycloak.example.util.MutualTLSUtils;
import org.keycloak.example.util.MyConstants;
import org.keycloak.example.util.OAuthClient;
import org.keycloak.example.util.SessionData;

import static org.keycloak.example.util.MyConstants.SERVER_ROOT;

/**
 * Application-scoped stuff
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class Services {

    private static final Services instance = new Services();
    private Services() {
        CryptoIntegration.init(Services.class.getClassLoader());
    }



    public static Services instance() {
        return instance;
    }

    private final FreeMarkerUtil freeMarker = new FreeMarkerUtil();
    private volatile OAuthClient oauthClient;

    private final SessionData session = new SessionData(); // TODO: Make sure that this is really session data and not app-scoped stuff


    public FreeMarkerUtil getFreeMarker() {
        return freeMarker;
    }

    public OAuthClient getOauthClient() {
        if (oauthClient == null) {
            synchronized (this) {
                oauthClient = new OAuthClient(SERVER_ROOT, MutualTLSUtils.newCloseableHttpClientWithDefaultKeyStoreAndTrustStore())
                        .realm(MyConstants.REALM_NAME);
//                oauthClient.init();
            }
        }
        return oauthClient;
    }

    public SessionData getSession() {
        return session;
    }

//    public void setSession(SessionData session) {
//        this.session = session;
//    }
}
