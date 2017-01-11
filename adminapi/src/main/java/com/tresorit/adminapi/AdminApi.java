package com.tresorit.adminapi;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import com.tresorit.adminapi.request.RequestAdminApiApproveTresorCreation;
import com.tresorit.adminapi.request.RequestAdminApiOperationId;
import com.tresorit.adminapi.request.RequestAdminApiValidateUser;
import com.tresorit.adminapi.response.ResponseAdminApiError;
import com.tresorit.adminapi.response.ResponseAdminApiInitUserRegistration;
import com.tresorit.zerokit.extension.Result;
import com.tresorit.zerokit.extension.Synchronized;
import com.tresorit.zerokit.observer.Observable;
import com.tresorit.zerokit.observer.Observer;
import com.tresorit.zerokit.observer.Subscriber;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class AdminApi {

    private final Sync sync = new Sync();

    private static final String BASE_URL = "api/v4/admin/";

    public AdminApi(String adminUId, String adminKey, String apiRoot) {
        this.adminUId = adminUId;
        this.adminKey = adminKey;
        this.apiRoot = apiRoot;

        this.path = apiRoot.substring(apiRoot.indexOf("/", "https://host".length()) + 1);

        handler = new Handler(Looper.getMainLooper());
    }

    @SuppressWarnings("WeakerAccess")
    final String adminUId;
    @SuppressWarnings("WeakerAccess")
    final String adminKey;
    @SuppressWarnings("WeakerAccess")
    final String apiRoot;
    @SuppressWarnings("WeakerAccess")
    final String path;

    private final Handler handler;

    @SuppressWarnings("WeakerAccess")
    void runOnUiThread(Runnable runnable) {
        handler.post(runnable);
    }


    /**
     * Observer implementation for handle JSON responses
     *
     * @param <T> the concrete JSON response type
     */
    private class ObserverJSON<T extends ZerokitJson> extends ObserverZerokit<T> {

        private final T response;

        /**
         * Constructs an observer object which can handle JSON responses
         *
         * @param subscriber which will handle the responses
         * @param response   the response object
         */
        ObserverJSON(@NonNull Subscriber<? super T, ? super ResponseAdminApiError> subscriber, @NonNull T response) {
            super(subscriber);
            this.response = response;
        }

        T getResult(String result) {
            return response.parse(result);
        }
    }

    private abstract class ObserverZerokit<T> implements Observer<String, String> {

        /**
         * the subscriber which will handle the results
         */
        final Subscriber<? super T, ? super ResponseAdminApiError> subscriber;

        private ObserverZerokit(@NonNull Subscriber<? super T, ? super ResponseAdminApiError> subscriber) {
            this.subscriber = subscriber;
        }

        @Override
        public void onFail(@NonNull final String e) {
            AdminApi.this.log("JS AdminApi: onFail", e);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    subscriber.onFail(new ResponseAdminApiError().parse(e));
                }
            });
        }

        @Override
        public void onSuccess(@NonNull final String result) {
            AdminApi.this.log("JS AdminApi: onSuccess", result);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    subscriber.onSuccess(getResult(result));
                }
            });
        }

        abstract T getResult(String result);
    }

    /**
     * Observer implementation for handle String responses
     */
    private class ObserverString extends ObserverZerokit<String> {
        /**
         * Constructs an observer object which can handle JSON responses
         *
         * @param subscriber which will handle the responses
         */
        ObserverString(@NonNull Subscriber<? super String, ? super ResponseAdminApiError> subscriber) {
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

    public Sync sync() {
        return sync;
    }

    public Observable<String, ResponseAdminApiError> validateUser(final String userId, final String regSessionId, final String regSessionVerifier, final String regValidationVerifier, final String alias) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseAdminApiError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseAdminApiError> subscriber) {
                AdminApi.this.administrativeCall(BASE_URL + "user/validate-user-registration", new RequestAdminApiValidateUser(alias, regSessionId, regSessionVerifier, regValidationVerifier, userId).stringify(), new ObserverString(subscriber));
            }
        });
    }

    public Observable<ResponseAdminApiInitUserRegistration, ResponseAdminApiError> initUserRegistration() {
        return new Observable<>(new Observable.OnSubscribe<ResponseAdminApiInitUserRegistration, ResponseAdminApiError>() {
            @Override
            public void call(Subscriber<? super ResponseAdminApiInitUserRegistration, ? super ResponseAdminApiError> subscriber) {
                AdminApi.this.administrativeCall(BASE_URL + "user/init-user-registration", "", new ObserverJSON<>(subscriber, new ResponseAdminApiInitUserRegistration()));
            }
        });
    }

    public Observable<String, ResponseAdminApiError> approveTresorCreation(final String tresorId) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseAdminApiError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseAdminApiError> subscriber) {
                AdminApi.this.administrativeCall(BASE_URL + "tresor/approve-tresor-creation", new RequestAdminApiApproveTresorCreation(tresorId).stringify(), new ObserverString(subscriber));
            }
        });
    }

    public Observable<String, ResponseAdminApiError> approvekick(final String operationId) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseAdminApiError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseAdminApiError> subscriber) {
                AdminApi.this.administrativeCall(BASE_URL + "tresor/approve-kick", new RequestAdminApiOperationId(operationId).stringify(), new ObserverString(subscriber));
            }
        });
    }

    public Observable<String, ResponseAdminApiError> approveShare(final String inviteId) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseAdminApiError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseAdminApiError> subscriber) {
                AdminApi.this.administrativeCall(BASE_URL + "tresor/approve-share", new RequestAdminApiOperationId(inviteId).stringify(), new ObserverString(subscriber));
            }
        });
    }

    public Observable<String, ResponseAdminApiError> approveCreateInvitationLink(final String id) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseAdminApiError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseAdminApiError> subscriber) {
                AdminApi.this.administrativeCall(BASE_URL + "tresor/approve-invitation-link-creation", new RequestAdminApiOperationId(id).stringify(), new ObserverString(subscriber));
            }
        });
    }

    public Observable<String, ResponseAdminApiError> approveInvitationLinkAcception(final String id) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseAdminApiError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseAdminApiError> subscriber) {
                AdminApi.this.administrativeCall(BASE_URL + "tresor/approve-invitation-link-acception", new RequestAdminApiOperationId(id).stringify(), new ObserverString(subscriber));
            }
        });
    }


    public class Sync {

        public Result<String, ResponseAdminApiError> validateUser(String userId, String regSessionId, String regSessionVerifier, String regValidationVerifier, String alias) {
            return Synchronized.call(AdminApi.this.validateUser(userId, regSessionId, regSessionVerifier, regValidationVerifier, alias));
        }

        public Result<ResponseAdminApiInitUserRegistration, ResponseAdminApiError> initUserRegistration() {
            return Synchronized.call(AdminApi.this.initUserRegistration());
        }

        public Result<String, ResponseAdminApiError> approveTresorCreation(String tresorId) {
            return Synchronized.call(AdminApi.this.approveTresorCreation(tresorId));
        }

        public Result<String, ResponseAdminApiError> approveShare(String inviteId) {
            return Synchronized.call(AdminApi.this.approveShare(inviteId));
        }

        public Result<String, ResponseAdminApiError> approveKick(String operationId) {
            return Synchronized.call(AdminApi.this.approvekick(operationId));
        }

        public Result<String, ResponseAdminApiError> approveCreateInvitationLink(String id) {
            return Synchronized.call(AdminApi.this.approveCreateInvitationLink(id));
        }

        public Result<String, ResponseAdminApiError> approveInvitationLinkAcception(String id) {
            return Synchronized.call(AdminApi.this.approveInvitationLinkAcception(id));
        }
    }

    @SuppressWarnings("WeakerAccess")
    void administrativeCall(final String url, final String body, final ObserverZerokit subscriber) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpURLConnection urlConnection = null;
                OutputStream outputStream = null;
                BufferedInputStream bufferedInputStream;

                try {

                    boolean hasBody = body != null;
                    byte[] contentBytes = hasBody ? string_to_bytes(body) : null;

                    Map<String, String> headers = adminPostAuth(adminUId, adminKey, path + "/" + url, contentBytes);


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
                    else subscriber.onFail(stringBuilder.toString());


                } catch (InvalidKeyException | NoSuchAlgorithmException | IOException e) {
                    subscriber.onFail(e.getMessage());
                } finally {
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
        }).start();
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
