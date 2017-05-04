package com.tresorit.zerokit.response;

import com.tresorit.zerokit.util.JSONObject;
import com.tresorit.zerokit.util.ZerokitJson;

public class ResponseZerokitChangePassword extends ZerokitJson {
    private int MasterFragmentVersion;

    public int getMasterFragmentVersion() {
        return MasterFragmentVersion;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ResponseZerokitChangePassword parse(String json) {
        JSONObject jsonobject = new JSONObject(json);
        MasterFragmentVersion = jsonobject.getInt("MasterFragmentVersion");
        return this;
    }

}


