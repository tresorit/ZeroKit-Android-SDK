package com.tresorit.zerokit.response;

import com.tresorit.zerokit.util.ZerokitJson;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * This represents the information that can be retrieved by the sdk without the password of the link, public in a sense that anyone in possession of the link can view it.
 */
public class ResponseZerokitInvitationLinkInfo extends ZerokitJson {

    /**
     * the user id of the creator of this link
     */
    private String creatorUserId;
    /**
     * bool value indicating if the links needs a password entered.
     */
    private boolean isPasswordProtected;

    /**
     * arbitrary string data, given at the time of creation
     */
    private String message;


    /**
     *  link information for internal use, used as a parameter for acceptInvitationLink
     */
    private String $token;

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public Boolean getPasswordProtected() {
        return isPasswordProtected;
    }

    public String getMessage() {
        return message;
    }

    public String getToken() {
        return $token;
    }

    @SuppressWarnings("unchecked")
    @Override
    public ResponseZerokitInvitationLinkInfo parse(String json){
        try {
            JSONObject jsonobject = new JSONObject(json);
            creatorUserId = jsonobject.getString("creatorUserId");
            isPasswordProtected = jsonobject.getBoolean("isPasswordProtected");
            message = jsonobject.getString("message");
            $token = jsonobject.getString("$token");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this;
    }
}
