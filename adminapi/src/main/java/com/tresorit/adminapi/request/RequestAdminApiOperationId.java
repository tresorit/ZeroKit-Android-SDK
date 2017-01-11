package com.tresorit.adminapi.request;

import org.json.JSONException;
import org.json.JSONObject;

public class RequestAdminApiOperationId {
    private final String OperationId;

    public RequestAdminApiOperationId(String operationId) {
        OperationId = operationId;
    }

    public String stringify() {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("OperationId", OperationId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObject.toString();
    }
}
