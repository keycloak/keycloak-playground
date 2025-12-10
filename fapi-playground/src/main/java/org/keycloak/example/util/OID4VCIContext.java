package org.keycloak.example.util;

import org.jboss.logging.Logger;
import org.keycloak.protocol.oid4vc.issuance.OID4VCAuthorizationDetailsResponse;
import org.keycloak.protocol.oid4vc.model.CredentialIssuer;
import org.keycloak.protocol.oid4vc.model.CredentialOfferURI;
import org.keycloak.protocol.oid4vc.model.CredentialResponse;
import org.keycloak.protocol.oid4vc.model.CredentialsOffer;

import java.util.Collections;
import java.util.List;

public class OID4VCIContext {

    private static final Logger log = Logger.getLogger(OID4VCIContext.class);

    private List<OID4VCCredential> availableCredentials = Collections.emptyList();

    // Obtained from config
    private String selectedCredentialId = "";

    // Obtained from requests
    private CredentialIssuer credentialIssuerMetadata;
    private CredentialOfferURI credentialOfferURI;
    private CredentialsOffer credentialsOffer;
    private OID4VCAuthorizationDetailsResponse authzDetails;
    private CredentialResponse credentialResponse;
    private String accessToken;

    public String getSelectedCredentialId() {
        return selectedCredentialId;
    }

    public void setSelectedCredentialId(String selectedCredentialId) {
        this.selectedCredentialId = selectedCredentialId;
    }

    public List<OID4VCCredential> getAvailableCredentials() {
        return availableCredentials;
    }

    public void setAvailableCredentials(List<OID4VCCredential> availableCredentials) {
        this.availableCredentials = availableCredentials;
    }

    public CredentialIssuer getCredentialIssuerMetadata() {
        return credentialIssuerMetadata;
    }

    public void setCredentialIssuerMetadata(CredentialIssuer credentialIssuerMetadata) {
        this.credentialIssuerMetadata = credentialIssuerMetadata;
    }

    public CredentialOfferURI getCredentialOfferURI() {
        return credentialOfferURI;
    }

    public void setCredentialOfferURI(CredentialOfferURI credentialOfferURI) {
        this.credentialOfferURI = credentialOfferURI;
    }

    public CredentialsOffer getCredentialsOffer() {
        return credentialsOffer;
    }

    public void setCredentialsOffer(CredentialsOffer credentialsOffer) {
        this.credentialsOffer = credentialsOffer;
    }

    public OID4VCAuthorizationDetailsResponse getAuthzDetails() {
        return authzDetails;
    }

    public void setAuthzDetails(OID4VCAuthorizationDetailsResponse authzDetails) {
        this.authzDetails = authzDetails;
    }

    public CredentialResponse getCredentialResponse() {
        return credentialResponse;
    }

    public void setCredentialResponse(CredentialResponse credentialResponse) {
        this.credentialResponse = credentialResponse;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public static class OID4VCCredential {

        private String id;
        private String displayName;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        @Override
        public String toString() {
            return "OID4VCCredential { " + id + " = " + displayName + " }";
        }
    }
}
