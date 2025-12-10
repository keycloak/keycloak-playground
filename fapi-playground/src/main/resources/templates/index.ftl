<html>
    <head>
        <title>FAPI demo</title>
        <link rel="stylesheet" type="text/css" href="${url.cssUrl}"/>
    </head>
    <body>
        <h1>FAPI playground</h1>

        <h3>Server info</h3>
        Keycloak server URL: <b>${serverInfo.authServerInfo} </b><br />
        Realm name: <b>${serverInfo.realmName} </b><br />
        <br />
        <hr />


<form id="my-form" method="post" action="${url.action}">
    <h3>Client Registration</h3>

    <div>
        <table>
            <tr><td>Init token: </td><td><input id="init-token" name="init-token" value="${clientConfigCtx.initialAccessToken!}"></td></tr>
            <tr>
                <td>Client authentication method: </td>
                <td>
                    <select name="client-auth-method" id="client-auth-method" value="${clientConfigCtx.clientAuthMethod!}">
                        <#if clientConfigCtx.clientAuthMethod == "none">
                            <option value="none" selected>none</option>
                        <#else>
                            <option value="none">none</option>
                        </#if>
                        <#if clientConfigCtx.clientAuthMethod == "client_secret_basic">
                            <option value="client_secret_basic" selected>client_secret_basic</option>
                        <#else>
                            <option value="client_secret_basic">client_secret_basic</option>
                        </#if>
                        <#if clientConfigCtx.clientAuthMethod == "tls_client_auth">
                            <option value="tls_client_auth" selected>tls_client_auth</option>
                        <#else>
                            <option value="tls_client_auth">tls_client_auth</option>
                        </#if>
                    </select>
                </td>
            </tr>
            <tr><td>Generate client keys: </td>
                <td>
                    <#if clientConfigCtx.generateJwks>
                        <input id="jwks" name="jwks" type="checkbox" checked>
                    <#else>
                        <input id="jwks" name="jwks" type="checkbox">
                    </#if>
                </td>
            </tr>
        </table>
    </div>
    <br />
    <div>
        <button onclick="submitWithAction('wellknown-endpoint')">Show response from OIDC well-known endpoint</button>
        <button onclick="submitWithAction('register-client')">Register client</button>
        <button onclick="submitWithAction('show-registered-client')">Show last registered client</button>
    </div>

    <br />
    <hr />

    <h3>OIDC flow</h3>

    <div>
        <table>
            <tr>
                <td>Use PKCE: </td><td>
                <#if oidcConfigCtx.usePkce>
                    <input id="pkce" name="pkce" type="checkbox" checked>
                <#else>
                    <input id="pkce" name="pkce" type="checkbox">
                </#if>
                </td>
            </tr>
            <tr>
                <td>Use Nonce parameter: </td><td>
                <#if oidcConfigCtx.useNonce>
                    <input id="nonce" name="nonce" type="checkbox" checked>
                <#else>
                    <input id="nonce" name="nonce" type="checkbox">
                </#if>
                </td></tr>
            <tr>
                <td>Use Request object: </td><td>
                <#if oidcConfigCtx.useRequestObject>
                    <input id="request-object" name="request-object" type="checkbox" checked>
                <#else>
                    <input id="request-object" name="request-object" type="checkbox">
                </#if>
                </td>
            </tr>
            <tr>
                <td>Use DPoP: </td><td>
                <#if oidcConfigCtx.useDPoP>
                    <input id="dpop" name="dpop" type="checkbox" checked>
                <#else>
                    <input id="dpop" name="dpop" type="checkbox">
                </#if>
                </td>
            </tr>
            <tr>
                <td>Use DPoP Authorization Code Binding: </td><td>
                <#if oidcConfigCtx.useDPoPAuthzCodeBinding>
                    <input id="dpop-authz-code-binding" name="dpop-authz-code-binding" type="checkbox" checked>
                <#else>
                    <input id="dpop-authz-code-binding" name="dpop-authz-code-binding" type="checkbox">
                </#if>
                </td>
            </tr>
        </table>
    </div>
    <br />
    <div>
    <button onclick="submitWithAction('create-login-url')">Create Login URL</button>
    <button onclick="submitWithAction('refresh-token')">Refresh token</button>
    <button onclick="submitWithAction('send-user-info')">Send UserInfo request</button>
    <button onclick="submitWithAction('rotate-dpop-keys')">Rotate DPoP keys</button>
    <button onclick="submitWithAction('show-last-token-response')">Show Last Token Response</button>
    <button onclick="submitWithAction('show-last-tokens')">Show Last Tokens</button>
    <button onclick="submitWithAction('show-last-dpop-proof')">Show Last DPoP JWT</button>
    </div>

    <br />
    <hr />

    <h3>OID4VCI</h3>

    <div>
        <table>
            <tr>
                <td>OID4VCI Credential Config: </td>
                <td>
                    <select name="oid4vci-credential" id="oid4vci-credential" value="${oid4vciCtx.selectedCredentialId!}">
                        <#list oid4vciCtx.availableCredentials as currentCredential>
                            <#if oid4vciCtx.selectedCredentialId == currentCredential.id>
                                <option value="${currentCredential.id}" selected>${currentCredential.displayName}</option>
                            <#else>
                                <option value="${currentCredential.id}">${currentCredential.displayName}</option>
                            </#if>
                        </#list>
                    </select>
                </td>
            </tr>
            <tr><td>Claims to present (divided by comma): </td><td><input id="oid4ci-claims-to-present" name="oid4ci-claims-to-present" value="${oid4vciCtx.claimsToPresent!}"></td></tr>
        </table>
    </div>
    <br />
    <div>
        <button onclick="submitWithAction('oid4vci-wellknown-endpoint')">Get OID4VCI metadata from well-known endpoint</button>
        <button onclick="submitWithAction('oid4vci-authz-code-flow')">Credential issuance - Authorization code grant</button>
        <button onclick="submitWithAction('oid4vci-pre-authz-code-flow')">Credential issuance - Pre-Authorized code grant</button>
        <button onclick="submitWithAction('oid4vci-credential-request')">Credential request</button>
        <button onclick="submitWithAction('oid4vci-last-credential-response')">Get last Verifiable Credential</button>
        <button onclick="submitWithAction('oid4vci-create-presentation')">Create presentation from last Verifiable Credential</button>
    </div>


    <input type="hidden" id="my-action" name="my-action">

</form>

<br />
<br />

<hr />

<#if info??>
    <#list info.outputs as out>
        <h3>${out.title!}</h3>
        <pre id="output">${out.content!}</pre>
        <hr />
    </#list>
</#if>

<#if authRequestUrl??>
    <a href="${authRequestUrl}">Login</a>
</#if>

<script>

    function submitWithAction(myAction) {
        document.getElementById('my-action').value = myAction;
        document.getElementById('my-form').submit();
    }

    function redirectToAccountManagement() {
        window.location.href = "${url.accountConsoleUrl}";
    }

</script>
</body>
</html>