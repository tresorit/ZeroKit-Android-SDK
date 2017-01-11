package com.tresorit.zerokit.response;

import com.tresorit.zerokit.util.ZerokitJson;

import org.json.JSONException;
import org.json.JSONObject;

public class ResponseZerokitLogin extends ZerokitJson {
    private String userId;

    public String getUserId() {
        return userId;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ResponseZerokitLogin parse(String json){
        try {
            JSONObject jsonobject = new JSONObject(json);
            userId = jsonobject.getString("userId");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

}
