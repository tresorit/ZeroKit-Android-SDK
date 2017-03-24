package com.tresorit.adminapi;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.tresorit.adminapi.request.RequestAdminApiApproveTresorCreation;
import com.tresorit.adminapi.request.RequestAdminApiOperationId;
import com.tresorit.adminapi.request.RequestAdminApiValidateUser;
import com.tresorit.adminapi.response.ResponseAdminApiError;
import com.tresorit.adminapi.response.ResponseAdminApiInitUserRegistration;
import com.tresorit.zerokit.call.ActionCallback;
import com.tresorit.zerokit.call.Call;
import com.tresorit.zerokit.call.CallAction;
import com.tresorit.zerokit.call.Callback;
import com.tresorit.zerokit.util.ZerokitJson;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class AdminApi {

    private static final String BASE_URL = "api/v4/admin/";

    public AdminApi(String adminUId, String adminKey, String apiRoot) {
        this.adminUId = adminUId;
        this.adminKey = adminKey;
        this.apiRoot = apiRoot;
    }

    static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    @SuppressWarnings("WeakerAccess")
    final String adminUId;
    @SuppressWarnings("WeakerAccess")
    final String adminKey;
    @SuppressWarnings("WeakerAccess")
    final String apiRoot;

    /**
     * Observer implementation for handle JSON responses
     *
     * @param <T> the concrete JSON response type
     */
    private class CallbackJSON<T extends ZerokitJson> extends CallbackZerokit<T> {

        private final T response;

        /**
         * Constructs an observer object which can handle JSON responses
         *
         * @param subscriber which will handle the responses
         * @param response   the response object
         */
        CallbackJSON(@NonNull Callback<? super T, ? super ResponseAdminApiError> subscriber, @NonNull T response) {
            super(subscriber);
            this.response = response;
        }

        T getResult(String result) {
            return response.parse(result);
        }
    }

    private abstract class CallbackZerokit<T> implements Callback<String, String> {

        /**
         * the subscriber which will handle the results
         */
        final Callback<? super T, ? super ResponseAdminApiError> subscriber;

        private CallbackZerokit(@NonNull Callback<? super T, ? super ResponseAdminApiError> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onError(@NonNull final String e) {
            AdminApi.this.log("JS AdminApi: onError", e);
            subscriber.onError(new ResponseAdminApiError().parse(e));
        }

        @Override
        public void onSuccess(@NonNull final String result) {
            AdminApi.this.log("JS AdminApi: onSuccess", result);
            subscriber.onSuccess(getResult(result));
        }

        abstract T getResult(String result);
    }

    /**
     * Observer implementation for handle String responses
     */
    private class CallbackString extends CallbackZerokit<String> {
        /**
         * Constructs an observer object which can handle JSON responses
         *
         * @param subscriber which will handle the responses
         */
        CallbackString(@NonNull Callback<? super String, ? super ResponseAdminApiError> subscriber) {
            super(subscriber);
        }

        @Override
        @NonNull
        String getResult(@NonNull String result) {
            return result.replaceAll("\"", "");
        }
    }

    @SuppressWarnings("WeakerAccess")
    void log(@NonNull String tag, @NonNull String msg) {
        if (BuildConfig.DEBUG)
            Log.d(tag, msg);
    }

    public Call<String, ResponseAdminApiError> validateUser(final String userId, final String regSessionId, final String regSessionVerifier, final String regValidationVerifier, final String alias) {
        return new CallAction<>(new ActionCallback<String, ResponseAdminApiError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseAdminApiError> subscriber) {
                AdminApi.this.administrativeCall(BASE_URL + "user/validate-user-registration", new RequestAdminApiValidateUser(alias, regSessionId, regSessionVerifier, regValidationVerifier, userId).stringify(), new CallbackString(subscriber));
            }
        });
    }

    public Call<ResponseAdminApiInitUserRegistration, ResponseAdminApiError> initUserRegistration() {
        return new CallAction<>(new ActionCallback<ResponseAdminApiInitUserRegistration, ResponseAdminApiError>() {
            @Override
            public void call(Callback<? super ResponseAdminApiInitUserRegistration, ? super ResponseAdminApiError> subscriber) {
                AdminApi.this.administrativeCall(BASE_URL + "user/init-user-registration", "", new CallbackJSON<>(subscriber, new ResponseAdminApiInitUserRegistration()));
            }
        });
    }

    public Call<String, ResponseAdminApiError> approveTresorCreation(final String tresorId) {
        return new CallAction<>(new ActionCallback<String, ResponseAdminApiError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseAdminApiError> subscriber) {
                AdminApi.this.administrativeCall(BASE_URL + "tresor/approve-tresor-creation", new RequestAdminApiApproveTresorCreation(tresorId).stringify(), new CallbackString(subscriber));
            }
        });
    }

    public Call<String, ResponseAdminApiError> approvekick(final String operationId) {
        return new CallAction<>(new ActionCallback<String, ResponseAdminApiError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseAdminApiError> subscriber) {
                AdminApi.this.administrativeCall(BASE_URL + "tresor/approve-kick", new RequestAdminApiOperationId(operationId).stringify(), new CallbackString(subscriber));
            }
        });
    }

    public Call<String, ResponseAdminApiError> approveShare(final String inviteId) {
        return new CallAction<>(new ActionCallback<String, ResponseAdminApiError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseAdminApiError> subscriber) {
                AdminApi.this.administrativeCall(BASE_URL + "tresor/approve-share", new RequestAdminApiOperationId(inviteId).stringify(), new CallbackString(subscriber));
            }
        });
    }

    public Call<String, ResponseAdminApiError> approveCreateInvitationLink(final String id) {
        return new CallAction<>(new ActionCallback<String, ResponseAdminApiError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseAdminApiError> subscriber) {
                AdminApi.this.administrativeCall(BASE_URL + "tresor/approve-invitation-link-creation", new RequestAdminApiOperationId(id).stringify(), new CallbackString(subscriber));
            }
        });
    }

    public Call<String, ResponseAdminApiError> approveInvitationLinkAcception(final String id) {
        return new CallAction<>(new ActionCallback<String, ResponseAdminApiError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseAdminApiError> subscriber) {
                AdminApi.this.administrativeCall(BASE_URL + "tresor/approve-invitation-link-acception", new RequestAdminApiOperationId(id).stringify(), new CallbackString(subscriber));
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    void administrativeCall(final String url, final String body, final CallbackZerokit subscriber) {

//        if (idlingResource != null) idlingResource.increment();
        EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection urlConnection = null;
                OutputStream outputStream = null;
                BufferedInputStream bufferedInputStream;

                try {

                    boolean hasBody = body != null;
                    byte[] contentBytes = hasBody ? string_to_bytes(body) : null;

                    Map<String, String> headers = adminPostAuth(adminUId, adminKey, url, contentBytes);


                    urlConnection = (HttpURLConnection) new URL(apiRoot + "/" + url).openConnection();
                    urlConnection.setRequestMethod(hasBody ? "POST" : "GET");
                    if (hasBody) urlConnection.setDoOutput(true);

                    for (String key : headers.keySet())
                        urlConnection.setRequestProperty(key, headers.get(key));

                    if (hasBody) {
                        outputStream = urlConnection.getOutputStream();
                        outputStream.write(contentBytes);
                    }


                    int responseCode = urlConnection.getResponseCode();
                    boolean isOK = responseCode >= HttpURLConnection.HTTP_OK && responseCode < HttpURLConnection.HTTP_MULT_CHOICE;

                    StringBuilder stringBuilder = new StringBuilder();
                    bufferedInputStream = new BufferedInputStream(isOK ? urlConnection.getInputStream() : urlConnection.getErrorStream());
                    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(bufferedInputStream));

                    String inputLine;
                    while ((inputLine = bufferedReader.readLine()) != null)
                        stringBuilder.append(inputLine);
                    if (isOK) subscriber.onSuccess(stringBuilder.toString());
                    else subscriber.onError(stringBuilder.toString());


                } catch (InvalidKeyException | NoSuchAlgorithmException | IOException e) {
                    subscriber.onError(e.getMessage());
                } finally {
//                    if (idlingResource != null) idlingResource.decrement();
                    if (outputStream != null)
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                    if (urlConnection != null)
                        urlConnection.disconnect();
                }
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    static byte[] string_to_bytes(String str) {
        return str.getBytes();
    }

    private static byte[] sha256(byte[] bytes) throws NoSuchAlgorithmException {
        return MessageDigest.getInstance("SHA-256").digest(bytes);
    }

    final private static char[] hexArray = "0123456789ABCDEF".toCharArray();

    private static String bytes_to_hex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private static String bytes_to_base64(byte[] arr) {
        return Base64.encodeToString(arr, Base64.DEFAULT);
    }

    private static byte[] hex_to_bytes(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static String toISOString(Date date) {
        @SuppressLint("SimpleDateFormat") DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        return dateFormat.format(date);
    }

    private static String getHeaderStringToHash(String verb, String url, Map<String, String> headers, List<String> hmacHeaders) {
        List<String> headerLines = new LinkedList<>();
        for (String key : hmacHeaders) headerLines.add(key + ":" + headers.get(key));
        return "" + verb + "\n" + url + "\n" + TextUtils.join("\n", headerLines);
    }

    private static byte[] hmacSha256(String keyHex, String stringData) throws NoSuchAlgorithmException, InvalidKeyException {
        byte[] keyBytes = hex_to_bytes(keyHex);
        byte[] data = string_to_bytes(stringData);

        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(keyBytes, "HmacSHA256");
        sha256_HMAC.init(secret_key);

        return sha256_HMAC.doFinal(data);
    }

    @SuppressWarnings("WeakerAccess")
    static Map<String, String> adminPostAuth(String adminUId, String adminKey, String url, byte[] contentBuffer) throws InvalidKeyException, NoSuchAlgorithmException {
        String date = toISOString(new Date());
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("UserId", adminUId);
        headers.put("TresoritDate", date);

        if (contentBuffer != null) {
            byte[] hash = sha256(contentBuffer);
            headers.put("Content-Type", "application/json");
            headers.put("Content-SHA256", bytes_to_hex(hash));
        }

        List<String> hmacHeaders = new LinkedList<>(headers.keySet());
        hmacHeaders.add("HMACHeaders");
        headers.put("HMACHeaders", TextUtils.join(",", hmacHeaders));
        String headerStringToHash = getHeaderStringToHash(contentBuffer != null ? "POST" : "GET", url, headers, hmacHeaders);
        byte[] hmacBytes = hmacSha256(adminKey, headerStringToHash);
        headers.put("Authorization", "AdminKey " + bytes_to_base64(hmacBytes));
        return headers;
    }

}
