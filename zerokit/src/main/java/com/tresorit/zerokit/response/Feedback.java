package com.tresorit.zerokit.response;

import android.text.TextUtils;

import com.tresorit.zerokit.util.JSONObject;
import com.tresorit.zerokit.util.ZerokitJson;

import java.util.List;

@SuppressWarnings("WeakerAccess")
public class Feedback extends ZerokitJson {
    private List<String> suggestions;
    private String warning;

    public List<String> getSuggestions() {
        return suggestions;
    }

    public String getWarning() {
        return warning;
    }

    @Override
    public String toString() {
        return String.format("suggestions: %s, warning: %s", TextUtils.join(", ", suggestions), warning);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ZerokitJson> T parse(String json) {
        JSONObject jsonObject = new JSONObject(json);
        suggestions = jsonObject.getStringArray("suggestions");
        warning = jsonObject.getString("warning");
        return (T) this;
    }
}
