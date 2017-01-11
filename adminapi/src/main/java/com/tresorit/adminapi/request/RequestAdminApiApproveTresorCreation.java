package com.tresorit.adminapi.request;

import org.json.JSONException;
import org.json.JSONObject;

public class RequestAdminApiApproveTresorCreation {
    private final String TresorId;

    public RequestAdminApiApproveTresorCreation(String tresorId) {
        TresorId = tresorId;
    }

    public String stringify() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("TresorId", TresorId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
