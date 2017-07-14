package com.tresorit.zerokit.response;

import com.tresorit.zerokit.util.JSONObject;
import com.tresorit.zerokit.util.ZerokitJson;

public class ResponseZerokitError extends ZerokitJson {
    private String type;
    private String code;
    private String message;
    private String description;
    private final ResponseZerokitInternalException internalException;

    public ResponseZerokitError(String type, String code, String message, String description) {
        this.type = type;
        this.code = code;
        this.message = message;
        this.description = description;
        this.internalException = new ResponseZerokitInternalException();
    }

    @Override
    public String toString() {
        return String.format("type: %s, code: %s, message: %s, description: %s", getType(), getCode(), getMessage(), getDescription());
    }

    public ResponseZerokitError(String description) {
        this("", "", "", description);
    }

    public ResponseZerokitError(String message, String description) {
        this("", "", message, description);
    }

    public ResponseZerokitError() {
        this("", "", "", "");
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setMessage(String message) {
        this.message = message;
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

    public String toJSON() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("description", description);
        jsonObject.put("code", code);
        jsonObject.put("message", message);
        jsonObject.put("type", type);
        return jsonObject.toString();
    }

    @SuppressWarnings("unchecked")
    @Override
    public ResponseZerokitError parse(String json) {
        JSONObject jsonobject = new JSONObject(json);
        description = jsonobject.getString("description");
        type = jsonobject.getString("type");
        code = jsonobject.getString("code");
        message = jsonobject.getString("message");
        internalException.parse(jsonobject.getJSONObject("internalException").toString());
        return this;
    }

}


