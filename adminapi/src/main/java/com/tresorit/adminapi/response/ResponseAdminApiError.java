package com.tresorit.adminapi.response;

import com.tresorit.zerokit.util.ZerokitJson;

import org.json.JSONException;
import org.json.JSONObject;

public class ResponseAdminApiError extends ZerokitJson {

    private String ErrorMessage;
    private String ErrorCode;
    private String Trace;

    public String getErrorMessage() {
        return ErrorMessage;
    }

    public String getErrorCode() {
        return ErrorCode;
    }

    public String getTrace() {
        return Trace;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ResponseAdminApiError parse(String json) {
        try {
            JSONObject jsonobject = new JSONObject(json);
            ErrorMessage = jsonobject.getString("ErrorMessage");
            ErrorCode = jsonobject.getString("ErrorCode");
            Trace = jsonobject.getString("Trace");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return  this;
    }
}
