package com.tresorit.zerokit.response;

import com.tresorit.zerokit.util.ZerokitJson;

import org.json.JSONException;
import org.json.JSONObject;

public class ResponseZerokitRegister extends ZerokitJson {
    private String RegValidationVerifier;

    public String getRegValidationVerifier() {
        return RegValidationVerifier;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ResponseZerokitRegister parse(String json) {
        try {
            JSONObject jsonobject = new JSONObject(json);
            RegValidationVerifier = jsonobject.getString("RegValidationVerifier");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

}
