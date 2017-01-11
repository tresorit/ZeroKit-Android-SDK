package com.tresorit.adminapi.response;

import com.tresorit.zerokit.util.ZerokitJson;

import org.json.JSONException;
import org.json.JSONObject;

public class ResponseAdminApiInitUserRegistration extends ZerokitJson {
    private String UserId;
    private String RegSessionId;
    private String RegSessionVerifier;

    public String getUserId() {
        return UserId;
    }

    public String getRegSessionId() {
        return RegSessionId;
    }

    public String getRegSessionVerifier() {
        return RegSessionVerifier;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ResponseAdminApiInitUserRegistration parse(String json){
        try {
            JSONObject jsonobject = new JSONObject(json);
            UserId = jsonobject.getString("UserId");
            RegSessionId = jsonobject.getString("RegSessionId");
            RegSessionVerifier = jsonobject.getString("RegSessionVerifier");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }
}
