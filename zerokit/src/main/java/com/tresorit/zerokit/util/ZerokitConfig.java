package com.tresorit.zerokit.util;

import org.json.JSONException;
import org.json.JSONObject;

public class ZerokitConfig extends ZerokitJson {

    private String AdminKey;
    private String AdminUserId;

    public String getAdminKey() {
        return AdminKey;
    }


    public String getAdminUserId() {
        return AdminUserId;
    }


    @SuppressWarnings("unchecked")
    @Override
    public ZerokitConfig parse(String json) {
        try {
            JSONObject jsonobject = new JSONObject(json);
            AdminKey = jsonobject.getString("AdminKey");
            AdminUserId = jsonobject.getString("AdminUserId");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }

}
