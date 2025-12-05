package org.keycloak.example.handlers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.http.impl.client.CloseableHttpClient;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.example.Services;
import org.keycloak.example.bean.InfoBean;
import org.keycloak.example.util.*;
import org.keycloak.jose.jws.JWSHeader;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailsResponse;
import org.keycloak.protocol.oid4vc.model.*;
import org.keycloak.protocol.oidc.grants.PreAuthorizedCodeGrantTypeFactory;
import org.keycloak.representations.IDToken;
import org.keycloak.sdjwt.vp.SdJwtVP;
import org.keycloak.testsuite.util.oauth.AbstractHttpPostRequest;
import org.keycloak.testsuite.util.oauth.AccessTokenResponse;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OID4VCIHandler implements ActionHandler {

    private static final Logger log = Logger.getLogger(OID4VCIHandler.class);

    @Override
    public Map<String, Function<ActionHandlerContext, InfoBean>> getActions() {
        return Map.of(
                "oid4vci-wellknown-endpoint", this::handleOID4VCIWellKnownEndpointAction,
                "create-credential-flow", this::handleCreateCredentialOfferAction,
                "last-credential-response", this::getLastCredentialResponse
        );
    }

    private InfoBean handleOID4VCIWellKnownEndpointAction(ActionHandlerContext actionContext) {
        CredentialIssuer credentialIssuer = invokeOID4VCIWellKnownEndpoint();

        OID4VCIContext oid4VCIContext = actionContext.getSession().getOrCreateOID4VCIContext();
        oid4VCIContext.setCredentialIssuerMetadata(credentialIssuer);

        List<OID4VCIContext.OID4VCCredential> availableCreds = getAvailableCredentials(credentialIssuer);
        log.infof("Available OID4VC credentials: %s", availableCreds);
        oid4VCIContext.setAvailableCredentials(availableCreds);

        try {
            return new InfoBean("OID4VCI well-known response", JsonSerialization.writeValueAsPrettyString(credentialIssuer));
        } catch (IOException ioe) {
            throw new MyException("Error when trying to deserialize OID4VCI well-known response to string", ioe);
        }
    }

    private InfoBean handleCreateCredentialOfferAction(ActionHandlerContext actionContext) {
        WebRequestContext<AbstractHttpPostRequest, AccessTokenResponse> lastTokenResponse = actionContext.getLastTokenResponse();
        SessionData session = actionContext.getSession();

        if (lastTokenResponse == null || lastTokenResponse.getResponse().getAccessToken() == null) {
            return new InfoBean("No Token Response", "No token response yet. Please login first.");
        } else {
            try {
                OID4VCIContext oid4VCIContext = session.getOrCreateOID4VCIContext();
                collectOID4VCIConfigParams(actionContext.getParams(), oid4VCIContext);

                if (oid4VCIContext.getCredentialIssuerMetadata() == null) {
                    return new InfoBean("No credential issuer metadata", "Please first obtain OID4VCI credential issuer metadata from OID4VCI well-known endpoint");
                }

                if (oid4VCIContext.getSelectedCredentialId() == null && oid4VCIContext.getSelectedCredentialId().isBlank()) {
                    return new InfoBean("No selected", "Please select OID4VCI credential to be used from available credentials");
                }

                // Step 1: Invoke credential-offer creation to Keycloak
                WebRequestContext<GenericRequestContext, CredentialOfferURI> credentialOfferCreation = invokeCredentialOfferCreation(lastTokenResponse.getResponse(), oid4VCIContext.getSelectedCredentialId());
                oid4VCIContext.setCredentialOfferURI(credentialOfferCreation.getResponse());

                // Step 2: Invoke credential-offer URI
                WebRequestContext<String, CredentialsOffer> credentialOffer = invokeCredentialOfferURI(oid4VCIContext.getCredentialOfferURI());
                oid4VCIContext.setCredentialsOffer(credentialOffer.getResponse());
                oid4VCIContext.setCredentialOfferURI(credentialOfferCreation.getResponse());

                // Step 3: Token-request with pre-authorized code
                WebRequestContext<GenericRequestContext, String> tokenResponse = triggerTokenRequestOfPreAuthorizationGrant(session.getAuthServerInfo().getTokenEndpoint(), credentialOffer.getResponse(), session);

                // Step 4: Credential request
                WebRequestContext<GenericRequestContext, CredentialResponse> credentialResponse = triggerCredentialRequest(oid4VCIContext, tokenResponse.getResponse());
                oid4VCIContext.setCredentialResponse(credentialResponse.getResponse());

                return new InfoBean(
                        "Request 1: Credential offer creation request", JsonSerialization.writeValueAsPrettyString(credentialOfferCreation.getRequest()),
                        "Response 1: Credential offer creation response", JsonSerialization.writeValueAsPrettyString(credentialOfferCreation.getResponse()),
                        "Request 1: Credential Offer request", credentialOffer.getRequest(),
                        "Response 2: Credential Offer response", JsonSerialization.writeValueAsPrettyString(credentialOffer.getResponse()),
                        "Request 3: Token request", JsonSerialization.writeValueAsPrettyString(tokenResponse.getRequest()),
                        "Response 3: Token response", tokenResponse.getResponse(),
                        "Request 4: Credential request", JsonSerialization.writeValueAsPrettyString(credentialResponse.getRequest()),
                        "Response 4: Credential response", JsonSerialization.writeValueAsPrettyString(credentialResponse.getResponse()));

            } catch (IOException ioe) {
                throw new MyException("Error when trying to deserialize OID4VCI well-known response to string", ioe);
            }
        }
    }

    // TODO: Use OAuthClient to invoke OID4VCI?
    private static CredentialIssuer invokeOID4VCIWellKnownEndpoint() {
        CloseableHttpClient httpClient = Services.instance().getHttpClient();
        String oid4vciWellKnownUrl = MyConstants.SERVER_ROOT + "/.well-known/openid-credential-issuer/realms/" + MyConstants.REALM_NAME;

        try {
            return SimpleHttp.doGet(oid4vciWellKnownUrl, httpClient).asJson(CredentialIssuer.class);
        } catch (IOException ioe) {
            throw new MyException("Exception when triggered OID4VCI endpoint", ioe);
        }
    }

    private void collectOID4VCIConfigParams(Map<String, String> params, OID4VCIContext oid4vciCtx) {
        String oid4vciCredential = params.get("oid4vci-credential");
        log.infof("Selected oid4vciCredential: %s", oid4vciCredential);
        oid4vciCtx.setSelectedCredentialId(oid4vciCredential);
    }

    private List<OID4VCIContext.OID4VCCredential> getAvailableCredentials(CredentialIssuer credIssuer) {
        return credIssuer.getCredentialsSupported().entrySet()
                .stream()
                .map(this::getCredential)
                .collect(Collectors.toList());
    }

    private OID4VCIContext.OID4VCCredential getCredential(Map.Entry<String, SupportedCredentialConfiguration> credConfig) {
        OID4VCIContext.OID4VCCredential cred = new OID4VCIContext.OID4VCCredential();
        cred.setId(credConfig.getKey());
        cred.setDisplayName(credConfig.getKey());

        // Prefer english displayName from credential metadata if available
        CredentialMetadata credMetadata = credConfig.getValue().getCredentialMetadata();
        if (credMetadata != null && credMetadata.getDisplay() != null) {
            for (DisplayObject display : credMetadata.getDisplay()) {
                if ("en".equalsIgnoreCase(display.getLocale()) || "en-EN".equalsIgnoreCase(display.getLocale())) {
                    cred.setDisplayName(display.getName());
                }
            }
        }

        return cred;
    }


    private static WebRequestContext<GenericRequestContext, CredentialOfferURI> invokeCredentialOfferCreation(AccessTokenResponse lastTokenResponse, String credentialConfigId) {
        CloseableHttpClient httpClient = Services.instance().getHttpClient();

        try {
            IDToken idToken = new JWSInput(lastTokenResponse.getIdToken()).readJsonContent(IDToken.class);
            String userId = idToken.getPreferredUsername(); // TODO: The parameter is called user_id, but expects username instead. See https://github.com/keycloak/keycloak/issues/44642
            String clientId = idToken.getIssuedFor();
            String credentialOfferCreationUri = getCredentialOfferUri(credentialConfigId, true, userId, clientId);

            CredentialOfferURI credentialOfferURI = SimpleHttp.doGet(credentialOfferCreationUri, httpClient)
                    .auth(lastTokenResponse.getAccessToken())
                    .asJson(CredentialOfferURI.class); // TODO: This should not return JSON, but rather some generic URI instead. Should this be even invoked from this app?

            GenericRequestContext request = new GenericRequestContext(credentialOfferCreationUri, Map.of(HttpHeaders.AUTHORIZATION, "Bearer " + lastTokenResponse.getAccessToken()), (String) null);
            return new WebRequestContext<>(request, credentialOfferURI);
        } catch (JWSInputException | IOException ioe) {
            throw new MyException("Exception when triggered OID4VCI credential offer creation endpoint", ioe);
        }
    }

    private static WebRequestContext<String, CredentialsOffer> invokeCredentialOfferURI(CredentialOfferURI credOfferURI) {
        CloseableHttpClient httpClient = Services.instance().getHttpClient();

        try {
            String credentialOfferURI = credOfferURI.getIssuer() + credOfferURI.getNonce();
            CredentialsOffer credOffer = SimpleHttp
                    .doGet(credentialOfferURI, httpClient)
                    .asJson(CredentialsOffer.class);

            return new WebRequestContext<>(credentialOfferURI, credOffer);
        } catch (IOException ioe) {
            throw new MyException("Exception when triggered OID4VCI Credential offer URI", ioe);
        }
    }

    private static String getCredentialOfferUri(String credentialConfigId, Boolean preAuthorized, String appUserId, String appClientId) {
        String res = Services.instance().getSession().getAuthServerInfo().getIssuer() + "/protocol/oid4vc/credential-offer-uri?credential_configuration_id=" + credentialConfigId;
        if (preAuthorized != null)
            res += "&pre_authorized=" + preAuthorized;
        if (appClientId != null)
            res += "&client_id=" + appClientId;
        if (appUserId != null)
            res += "&user_id=" + appUserId;
        return res;
    }

    private static WebRequestContext<GenericRequestContext, String> triggerTokenRequestOfPreAuthorizationGrant(String tokenEndpoint, CredentialsOffer credentialsOffer, SessionData session) {
        CloseableHttpClient httpClient = Services.instance().getHttpClient();
        PreAuthorizedCode preAuthorizedCode = credentialsOffer.getGrants().getPreAuthorizedCode();

        try {
            SimpleHttp simpleHttp = SimpleHttp.doPost(tokenEndpoint, httpClient);
            Map<String, String> params = Map.of(
                    OAuth2Constants.GRANT_TYPE, PreAuthorizedCodeGrantTypeFactory.GRANT_TYPE,
                    PreAuthorizedCodeGrantTypeFactory.CODE_REQUEST_PARAM, preAuthorizedCode.getPreAuthorizedCode()
            );
            for (Map.Entry<String, String> param : params.entrySet()) {
                simpleHttp.param(param.getKey(), param.getValue());
            }
            String response = simpleHttp.asString();

            List<OID4VCAuthorizationDetailsResponse> authzDetails = parseAuthorizationDetails(response);
            if (authzDetails.size() != 1) {
                throw new MyException("Unexpected size of the authzDetails. Size was " + authzDetails.size() + ". The response was: " + response);
            }
            session.getOrCreateOID4VCIContext().setAuthzDetails(authzDetails.get(0));

            GenericRequestContext requestCtx = new GenericRequestContext(tokenEndpoint, null, params);

            return new WebRequestContext<>(requestCtx, response);
        } catch (IOException ioe) {
            throw new MyException("Exception when triggered Token endpoint of pre-authorized grant", ioe);
        }
    }

    /**
     * Parse authorization details from the token response.
     */
    // TODO: Could be simplified probably...
    private static List<OID4VCAuthorizationDetailsResponse> parseAuthorizationDetails(String responseBody) {
        try {
            // Parse the JSON response to extract authorization_details
            Map<String, Object> responseMap = JsonSerialization.readValue(responseBody, Map.class);
            Object authDetailsObj = responseMap.get("authorization_details");

            if (authDetailsObj == null) {
                return Collections.emptyList();
            }

            // Convert to list of OID4VCAuthorizationDetailsResponse
            return JsonSerialization.readValue(JsonSerialization.writeValueAsString(authDetailsObj),
                    new TypeReference<List<OID4VCAuthorizationDetailsResponse>>() {
                    });
        } catch (IOException e) {
            throw new MyException("Failed to parse authorization_details from response", e);
        }
    }

    private static WebRequestContext<GenericRequestContext, CredentialResponse> triggerCredentialRequest(OID4VCIContext oid4VCIContext, String accessTokenResponse) {
        CloseableHttpClient httpClient = Services.instance().getHttpClient();
        try {
            CredentialRequest credentialRequest = new CredentialRequest();
            credentialRequest.setCredentialIdentifier(oid4VCIContext.getAuthzDetails().getCredentialIdentifiers().get(0));

            org.keycloak.representations.AccessTokenResponse atResponse = JsonSerialization.readValue(accessTokenResponse, org.keycloak.representations.AccessTokenResponse.class);
            String accessToken = atResponse.getToken();

            String credentialEndpoint = oid4VCIContext.getCredentialIssuerMetadata().getCredentialEndpoint();
            String credResponse = SimpleHttp.doPost(credentialEndpoint, httpClient)
                    .auth(accessToken)
                    .header(HttpHeaders.CONTENT_TYPE, "application/json")
                    .json(credentialRequest)
                    .asString();

            CredentialResponse credentialResponse;
            try {
                credentialResponse = JsonSerialization.readValue(credResponse, CredentialResponse.class);
            } catch (IOException ioe) {
                throw new MyException("Failed to parse credential response. The response returned was: " + credResponse);
            }

            Map<String, String> headers = Map.of(HttpHeaders.CONTENT_TYPE, "application/json",
                    "Authorization", "Bearer " + accessToken);

            GenericRequestContext ctx = new GenericRequestContext(credentialEndpoint, headers, JsonSerialization.writeValueAsPrettyString(credentialRequest));
            return new WebRequestContext<>(ctx, credentialResponse);
        } catch (IOException e) {
            throw new MyException("Failed to invoke credential request or parse credential response", e);
        }
    }

    private InfoBean getLastCredentialResponse(ActionHandlerContext actionContext) {
        OID4VCIContext oid4vciCtx = actionContext.getSession().getOrCreateOID4VCIContext();
        if (oid4vciCtx.getCredentialResponse() == null) {
            return new InfoBean("No verifiable credential", "No OID4VCI verifiable credential present. Please first start credential issuance flow.");
        } else {
            CredentialResponse credentialResponse = oid4vciCtx.getCredentialResponse();
            String credentialStr = credentialResponse.getCredentials().get(0).getCredential().toString();

            try {
                // Assumptions it is Sd-JWT VC. TODO: Make it working for W3C credentials...
                SdJwtVP sdJWTVP = SdJwtVP.of(credentialStr);
                JWSHeader jwsHeader = sdJWTVP.getIssuerSignedJWT().getJwsHeader();
                JsonNode payloadNode = sdJWTVP.getIssuerSignedJWT().getPayload();

                StringBuilder claimsStr = new StringBuilder("{");
                for (Map.Entry<String, ArrayNode> claim : sdJWTVP.getClaims().entrySet()) {
                    claimsStr.append(" " + claim.getKey() + " = " + JsonSerialization.writeValueAsPrettyString(claim.getValue()) + "\n");
                }
                claimsStr.append("}");

                return new InfoBean(
                        "Plain-credential", credentialStr,
                        "Sd-JWT credential - header", JsonSerialization.writeValueAsPrettyString(jwsHeader),
                        "Sd-JWT credential - payload", JsonSerialization.writeValueAsPrettyString(payloadNode),
                        "Sd-JWT disclosed claims", claimsStr.toString());
            } catch (IOException ioe) {
                throw new MyException("Exception when displaying latest verifiable credential", ioe);
            }
        }
    }
}
