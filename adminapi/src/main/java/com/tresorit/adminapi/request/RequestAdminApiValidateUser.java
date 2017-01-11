package com.tresorit.adminapi.request;

import org.json.JSONException;
import org.json.JSONObject;

public class RequestAdminApiValidateUser {
    private final String Alias;
    private final String RegSessionId;
    private final String RegSessionVerifier;
    private final String RegValidationVerifier;
    private final String UserId;

    public RequestAdminApiValidateUser(String alias, String regSessionId, String regSessionVerifier, String regValidationVerifier, String userId) {
        Alias = alias;
        RegSessionId = regSessionId;
        RegSessionVerifier = regSessionVerifier;
        RegValidationVerifier = regValidationVerifier;
        UserId = userId;
    }

    public String stringify() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("Alias", Alias);
            jsonObject.put("RegSessionId", RegSessionId);
            jsonObject.put("RegSessionVerifier", RegSessionVerifier);
            jsonObject.put("RegValidationVerifier", RegValidationVerifier);
            jsonObject.put("UserId", UserId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
