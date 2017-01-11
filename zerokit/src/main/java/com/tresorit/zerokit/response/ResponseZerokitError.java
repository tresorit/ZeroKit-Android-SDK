package com.tresorit.zerokit.response;

import com.tresorit.zerokit.util.ZerokitJson;

import org.json.JSONException;
import org.json.JSONObject;

public class ResponseZerokitError extends ZerokitJson {
    private String type;
    private String code;
    private String message;
    private String description;

    public ResponseZerokitError(String type, String code, String message, String description) {
        this.type = type;
        this.code = code;
        this.message = message;
        this.description = description;
    }

    public ResponseZerokitError(String description) {
        this("", "", "", description);
    }

    public ResponseZerokitError() {
        this("", "", "", "");
    }

    public String getType() {
        return type;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }

    public String toJSON(){
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put("description", description);
            jsonObject.put("code", code);
            jsonObject.put("message", message);
            jsonObject.put("type", type);
        } catch (JSONException ignored) {
        }
        return jsonObject.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ResponseZerokitError parse(String json){
        ResponseZerokitError result = new ResponseZerokitError();
        try {
            JSONObject jsonobject = new JSONObject(json);
            result.description = jsonobject.getString("description");
            result.type = jsonobject.getString("type");
            result.code = jsonobject.getString("code");
            result.message = jsonobject.getString("message");
        } catch (JSONException ignored) {
        }
        return result;
    }

}


