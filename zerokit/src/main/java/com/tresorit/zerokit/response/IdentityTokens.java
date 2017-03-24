package com.tresorit.zerokit.response;

public class IdentityTokens {
    private final String AuthorizationCode;

    private final String IdentityToken;

    private final String CodeVerifier;

    public IdentityTokens(String authorizationCode, String identityToken, String codeVerifier) {
        AuthorizationCode = authorizationCode;
        IdentityToken = identityToken;
        CodeVerifier = codeVerifier;
    }

    public IdentityTokens(String authorizationCode, String identityToken) {
        this(authorizationCode, identityToken, null);
    }

    public String getAuthorizationCode() {
        return AuthorizationCode;
    }

    public String getCodeVerifier() {
        return CodeVerifier;
    }

    public String getIdentityToken() {
        return IdentityToken;
    }
}
