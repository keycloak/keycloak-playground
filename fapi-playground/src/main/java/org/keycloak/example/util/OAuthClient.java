package org.keycloak.example.util;

import org.apache.http.impl.client.CloseableHttpClient;
import org.keycloak.OAuth2Constants;
import org.keycloak.example.oauth.AbstractOAuthClient;
import org.keycloak.example.oauth.OAuthClientConfig;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OAuthClient extends AbstractOAuthClient<OAuthClient> {

    public OAuthClient(String baseUrl, CloseableHttpClient httpClient) {
        super(baseUrl, httpClient);

        config = new OAuthClientConfig()
                .responseType(OAuth2Constants.CODE);
    }

    @Override
    public void fillLoginForm(String username, String password) {
        throw new UnsupportedOperationException("Not supported");
//        LoginPage loginPage = new LoginPage(driver);
//        PageFactory.initElements(driver, loginPage);
//        loginPage.fillLogin(username, password);
//        loginPage.submit();
    }

    public void close() {
    }

}
