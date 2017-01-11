package com.tresorit.zerokit.response;

import com.tresorit.zerokit.util.ZerokitJson;

import org.json.JSONException;
import org.json.JSONObject;

public class ResponseZerokitCreateInvitationLink extends ZerokitJson {
    private String url;
    private String id;

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ResponseZerokitCreateInvitationLink parse(String json){
        try {
            JSONObject jsonobject = new JSONObject(json);
            url = jsonobject.getString("url");
            id = jsonobject.getString("id");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }
}
