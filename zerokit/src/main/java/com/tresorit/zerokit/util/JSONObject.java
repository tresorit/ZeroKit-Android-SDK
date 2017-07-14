package com.tresorit.zerokit.util;


import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class JSONObject {

    private org.json.JSONObject jsonObject;

    public JSONObject() {
        jsonObject = new org.json.JSONObject();
    }

    public JSONObject(String json) {
        try {
            jsonObject = new org.json.JSONObject(json);
        } catch (JSONException e) {
            //e.printStackTrace();
            jsonObject = new org.json.JSONObject();
        }
    }

    public org.json.JSONObject getJSONObject(String name) {
        if (jsonObject != null)
            try {
                return jsonObject.getJSONObject(name);
            } catch (JSONException e) {
                //e.printStackTrace();
            }
        return new org.json.JSONObject();
    }


    public String getString(String name) {
        if (jsonObject != null)
            try {
                return jsonObject.getString(name);
            } catch (JSONException e) {
                //e.printStackTrace();
            }
        return "";
    }

    public double getDouble(String name) {
        if (jsonObject != null)
            try {
                return jsonObject.getDouble(name);
            } catch (JSONException e) {
                //e.printStackTrace();
            }
        return 0;
    }

    public int getInt(String name) {
        if (jsonObject != null)
            try {
                return jsonObject.getInt(name);
            } catch (JSONException e) {
                //e.printStackTrace();
            }
        return 0;
    }

    public List<String> getStringArray(String name) {
        if (jsonObject != null)
            try {
                JSONArray jsonArray = jsonObject.getJSONArray(name);
                List<String> result = new ArrayList<>();
                for (int i = 0; i < jsonArray.length(); i++)
                    result.add(jsonArray.getString(i));
                return result;
            } catch (JSONException e) {
                //e.printStackTrace();
            }
        return new ArrayList<>();
    }

    public JSONObject put(String name, Object value)  {
        if (jsonObject != null)
            try {
                jsonObject.put(name, value);
            } catch (JSONException e) {
                //e.printStackTrace();
            }
        return this;
    }

    @Override
    public String toString() {
        return jsonObject != null ? jsonObject.toString() : "";
    }
}
