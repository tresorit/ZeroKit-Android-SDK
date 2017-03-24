package com.tresorit.zerokit.response;

import com.tresorit.zerokit.util.JSONObject;
import com.tresorit.zerokit.util.ZerokitJson;

public class ResponseZerokitInternalException extends ZerokitJson {
    private String type;
    private String code;

    @Override
    public String toString() {
        return String.format("type: %s, code: %s", type, code);
    }

    @SuppressWarnings("unchecked")
    @Override
    public ResponseZerokitInternalException parse(String json) {
        JSONObject jsonobject = new JSONObject(json);
        type = jsonobject.getString("type");
        code = jsonobject.getString("code");
        return this;
    }

}


