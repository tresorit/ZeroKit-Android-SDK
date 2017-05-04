package com.tresorit.zerokit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.security.KeyPairGeneratorSpec;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.IdlingResource;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.URLUtil;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.tresorit.zerokit.call.Action;
import com.tresorit.zerokit.call.ActionCallback;
import com.tresorit.zerokit.call.Call;
import com.tresorit.zerokit.call.CallAction;
import com.tresorit.zerokit.call.CallAsync;
import com.tresorit.zerokit.call.CallAsyncAction;
import com.tresorit.zerokit.call.Callback;
import com.tresorit.zerokit.call.CallbackExecutor;
import com.tresorit.zerokit.response.IdentityTokens;
import com.tresorit.zerokit.response.ResponseZerokitChangePassword;
import com.tresorit.zerokit.response.ResponseZerokitCreateInvitationLink;
import com.tresorit.zerokit.response.ResponseZerokitError;
import com.tresorit.zerokit.response.ResponseZerokitInvitationLinkInfo;
import com.tresorit.zerokit.response.ResponseZerokitLogin;
import com.tresorit.zerokit.response.ResponseZerokitPasswordStrength;
import com.tresorit.zerokit.response.ResponseZerokitRegister;
import com.tresorit.zerokit.util.PRNGFixes;
import com.tresorit.zerokit.util.ZerokitJson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

import static com.tresorit.zerokit.Zerokit.Function.Type.Cmd;
import static com.tresorit.zerokit.Zerokit.Function.Type.Default;
import static com.tresorit.zerokit.Zerokit.Function.Type.MobileCmd;


public final class Zerokit {

    @SuppressWarnings("WeakerAccess")
    final Executor executorWebView;


    private final static class MainThreadExecutor implements Executor {
        private final Handler handler;

        MainThreadExecutor() {
            handler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void execute(Runnable r) {
            handler.post(r);
        }
    }

    private final static class WebViewThreadExecutor implements Executor {
        private final Handler handler;

        WebViewThreadExecutor() {
            HandlerThread handlerThread = new HandlerThread("WebView thread");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
        }

        @Override
        public void execute(Runnable r) {
            handler.post(r);
        }
    }


    @SuppressWarnings("WeakerAccess")
    static final String HTTPS_SCHEME = "https";
    private static final String REDIRECT_URI_FORMAT_PATTERN = "%1$s.%2$s.api.tresorit.io";

    @SuppressWarnings("WeakerAccess")
    static final String param_error = "error";
    @SuppressWarnings("WeakerAccess")
    static final String param_error_description = "error_description";
    @SuppressWarnings("WeakerAccess")
    static final String param_code = "code";
    @SuppressWarnings("WeakerAccess")
    static final String param_state = "state";
    @SuppressWarnings("WeakerAccess")
    static final String param_id_token = "id_token";

    private static final String ALPHANUMERIC_CHARACTERS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

    private final SecureRandom secureRandom;

    /**
     * Log tag
     */
    private static final String TAG = "Zerokit";

    /**
     * Key to get the api root metadata from manifest file
     */
    public static final String API_ROOT = "com.tresorit.zerokitsdk.API_ROOT";

    /**
     * The name of the keystore file
     */
    private static final String KEY_STORE_PATH = "login";
    /**
     * The name of the keystore alias
     */
    private static final String KEY_STORE_ALIAS = "key_login";
    /**
     * The name of the Android Keystore instance
     */
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";

    /**
     * Singleton instance for Zerokit
     */
    @SuppressLint("StaticFieldLeak")
    private static Zerokit instance;

    /**
     * The webview instance, which will be responsible for the javascript communication.
     * This webview requires a context, which need to be provided by the SDK user.
     * This view will be not attached to any other view.
     */
    @SuppressWarnings("WeakerAccess")
    WebView webView;

    @SuppressWarnings("WeakerAccess")
    WebView webViewIDP;
    /**
     * Own webview client which is responsible for handle the 'page-finished' events
     */
    @SuppressWarnings("WeakerAccess")
    final ZerokitWebViewClientBase clientWebView;

    @SuppressWarnings("WeakerAccess")
    final ZerokitWebViewClientBase clientIdpWebView;

    @SuppressWarnings("WeakerAccess")
    final StateHandler idpStateHandler;

    /**
     * Represents the states of the initialization process
     */
    @SuppressWarnings("WeakerAccess")
    final StateHandler initStateHandler;

    /**
     * Collection of the registered observers, which will be triggered after a javascript function returns a result
     */
    @SuppressWarnings("WeakerAccess")
    final Map<String, CallbackExecutor<? super String, ? super String>> observers;

    /**
     * The api root url
     */
    @SuppressWarnings("WeakerAccess")
    final String apiRoot;

    @SuppressWarnings("WeakerAccess")
    final String apiRootUrl;

    @SuppressWarnings("WeakerAccess")
    final String tenantId;

    /**
     * The javascript source of javascript
     */
    @SuppressWarnings("WeakerAccess")
    final String serializerJavaScriptSource;

    @SuppressWarnings("WeakerAccess")
    final String idpHelperJavaScriptSource;

    /**
     * The JavaScript interface which is callable from the javascript side and handles the results of the promises
     */
    @SuppressWarnings("WeakerAccess")
    final JSInterfaceResponseHandler jsInterfaceResponseHandler;
    /**
     * The JavaScript interface which is responsible for pass byte arrays to the javascript side
     */
    @SuppressWarnings("WeakerAccess")
    final JSInterfaceByteArrayProvider jsInterfaceByteArrayProvider;

    @SuppressWarnings("WeakerAccess")
    final JSInterfaceHtmlExporter jsInterfaceHtmlExporter;

    @SuppressWarnings("WeakerAccess")
    @Nullable
    ZerokitCountingIdlingResource idlingResource;

    /**
     * Represents the states of the initialization process
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({State.NOT_STARTED, State.RUNNING, State.FINISHED})
    private @interface State {
        int NOT_STARTED = 0;
        int RUNNING = 1;
        int FINISHED = 2;
    }

    /**
     * Init the Zerokit singleton instance
     *
     * @param context a Context object used to pass it to the WebView and to get the metadata from manifest file
     */
    static void init(@NonNull Context context, @NonNull String url) {
        if (TextUtils.isEmpty(url))
            throw new IllegalStateException("No ApiRoot definition found in the AndroidManifest.xml");
        PRNGFixes.apply();
        instance = new Zerokit(context, url);
    }

    /**
     * Returns the Zerokit instance
     *
     * @return the Zerokit instance
     */
    public static Zerokit getInstance() {
        return instance;
    }

    /**
     * Constructs a new Zerokit instance with a Context object.
     *
     * @param context a Context object used to pass it to the WebView
     * @param url     the url of the provided api root
     */
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private Zerokit(@NonNull final Context context, @NonNull String url) {
        Uri uri = Uri.parse(url);

        executorWebView = new WebViewThreadExecutor();
        secureRandom = new SecureRandom();

        apiRoot = uri.getAuthority();
        tenantId = apiRoot.split("\\.")[0];
        apiRootUrl = uri.buildUpon().appendPath("static").appendPath("v4").appendPath("api.html").build().toString();

        observers = new HashMap<>();

        clientWebView = new ZerokitWebViewClientBase();
        clientIdpWebView = new ZerokitWebViewClientBase();

        idpStateHandler = new StateHandler();
        initStateHandler = new StateHandler();

        idpStateHandler.addListener(new StateChangeListener() {
            @Override
            public void onStateChanged(@State int state) {
                switch (state) {
                    case State.FINISHED:
                        decrementIdlingResource();
                        jsInterfaceHtmlExporter.removeAllListeners();
                        break;
                    case State.RUNNING:
                        incrementIdlingResoure();
                        break;
                }
            }
        });

        jsInterfaceResponseHandler = new JSInterfaceResponseHandler();
        jsInterfaceByteArrayProvider = new JSInterfaceByteArrayProvider();
        jsInterfaceHtmlExporter = new JSInterfaceHtmlExporter();

        clientWebView.addPageFinishListener(new PageFinishListener() {
            @Override
            public void onPageFinished(String url) {
                if (initStateHandler.getState() == State.RUNNING){
                    log("Init finished: " + url);
                    initStateHandler.setState(State.FINISHED);
                    clientWebView.removePageFinishListener(this);
                }
            }

            @Override
            public void onReceivedError(int errorCode) {
                switch (errorCode){
                    case WebViewClient.ERROR_HOST_LOOKUP:
                        log("Init failed: " + errorCode);
                        initStateHandler.setState(State.NOT_STARTED);
                        break;
                }
            }
        });

        serializerJavaScriptSource = loadJavaScriptSource(context, R.raw.javascript);
        idpHelperJavaScriptSource = loadJavaScriptSource(context, R.raw.javascript_idp);

        executorWebView.execute(new Runnable() {
            @Override
            public void run() {
                webView = createWebView(context);
                webView.addJavascriptInterface(jsInterfaceResponseHandler, "JSInterfaceResponseHandler");
                webView.addJavascriptInterface(jsInterfaceByteArrayProvider, "JSInterfaceByteArrayProvider");
                webView.setWebViewClient(clientWebView);
                webView.setWebChromeClient(new ZerokitWebChromeClient());

                webViewIDP = createWebView(context);
                webViewIDP.addJavascriptInterface(jsInterfaceHtmlExporter, "JSInterfaceHtmlExporter");
                webViewIDP.setWebViewClient(clientIdpWebView);

                synchronized (executorWebView) {
                    executorWebView.notify();
                }
            }
        });

        synchronized (executorWebView) {
            try {
                executorWebView.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    @SuppressWarnings({"WeakerAccess", "SetJavaScriptEnabled"})
    WebView createWebView(Context context) {
        WebView webView = new WebView(context);
        webView.setWillNotDraw(true);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        return webView;
    }

    @NonNull
    private String loadJavaScriptSource(@NonNull Context context, int resId) {
        BufferedReader reader = null;
        StringBuilder stringBuffer = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(resId)));
            String line = reader.readLine();
            while (line != null) {
                stringBuffer.append(line);
                line = reader.readLine();
            }
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (reader != null)
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
        }
        return stringBuffer.toString();
    }

    @SuppressWarnings({"WeakerAccess"})
    CallAsync<Void, String> idpCheck() {
        return new CallAsyncAction<>(new ActionCallback<Void, String>() {
            @Override
            public void call(final Callback<? super Void, ? super String> subscriber) {
                if (idpStateHandler.getState() == State.RUNNING) {
                    idpStateHandler.addListener(new StateChangeListener() {
                        @Override
                        public void onStateChanged(@State int state) {
                            if (state == State.FINISHED) {
                                subscriber.onSuccess(null);
                                idpStateHandler.removeListener(this);
                            }
                        }
                    });
                } else
                    /*
                    * If the idp request has been finished
                    */
                    subscriber.onSuccess(null);
            }
        });
    }

    /**
     * The init method of the webview. This function is responsible for load init url
     *
     * @return an Observable object which will tell us when the init finished
     */
    @NonNull
    @SuppressWarnings({"WeakerAccess"})
    CallAsync<Void, String> initCheck() {
        return new CallAsyncAction<>(new ActionCallback<Void, String>() {
            @Override
            public void call(final Callback<? super Void, ? super String> subscriber) {
                if (initStateHandler.getState() != State.FINISHED) {
                    log("Init not finished");
                    /*
                    * If the init url not loaded yet
                    */
                    initStateHandler.addListener(new StateChangeListener() {
                        @Override
                        public void onStateChanged(@State int state) {
                            if (state == State.FINISHED) {
                                subscriber.onSuccess(null);
                                initStateHandler.removeListener(this);
                            } else if (state == State.NOT_STARTED){
                                subscriber.onError("Initialization failed");
                                initStateHandler.removeListener(this);
                            }
                        }
                    });

                    /*
                    * Ensure that the init url has been called only once
                    */
                    if (initStateHandler.getState() == State.NOT_STARTED) {
                        if (!URLUtil.isValidUrl(apiRootUrl)) subscriber.onError("Invalid root url");
                        else {
                            log("Init started");
                            initStateHandler.setState(State.RUNNING);
                            loadUrl(webView, apiRootUrl);
                        }
                    }
                } else
                    /*
                    * If the init already has been finished
                    */
                    subscriber.onSuccess(null);
            }
        });
    }

    @SuppressWarnings({"WeakerAccess"})
    <T> void callFunction(@NonNull final Function function, @NonNull final CallbackByteArrayIds<T> callback, final Object... arguments) {
        callFunction(function, new CallbackExecutor<>(callback), callback.ids, arguments);
    }


    /**
     * The common "function caller" method, which transmits a request to the javascript section
     *
     * @param function   The concrete function which we would like to call
     * @param subscriber an Observer object, which will handle the result of the method
     * @param arguments  the arguments of the function which we would like to call
     */
    @SuppressWarnings({"WeakerAccess", "unchecked"})
    void callFunction(@NonNull final Function function, @NonNull final CallbackExecutor<String, String> subscriber, final String[] ids, final Object... arguments) {
        log(String.format("call: %s", function.name()));

        initCheck().enqueue(new Action<Void>() {
            @Override
            public void call(Void result) {
                String id = UUID.randomUUID().toString();
                observers.put(id, subscriber);
                incrementIdlingResoure();

                try {
                    JSONObject callData = new JSONObject();
                    callData.put("id", id);
                    callData.put("type", function.type);
                    callData.put("functionName", function.name());
                    JSONArray args = new JSONArray();
                    for (Object arg : arguments) args.put(arg);
                    callData.put("args", args);


                    JSONArray extraArgs = new JSONArray();
                    for (int i = 0; i < function.extraArgs.length; i++) {
                        ExtraArg extraArg = function.extraArgs[i];
                        JSONObject jsonExtraArg = new JSONObject();
                        jsonExtraArg.put("position", (extraArg.position == -1) ? (args.length() + i) : extraArg.position);
                        jsonExtraArg.put("id", ids[i]);
                        jsonExtraArg.put("type", extraArg.type);
                        extraArgs.put(jsonExtraArg);
                    }
                    callData.put("extraArgs", extraArgs);

                    callData.put("responseFormatter", function.responseFormatter);

                    loadUrl(webView,
                            "javascript:\n" +
                                    serializerJavaScriptSource +
                                    String.format("\ncallFunction(%s);", JSONObject.quote(callData.toString())));

                } catch (JSONException e1) {
                    observers.remove(id);
                    decrementIdlingResource();
                    subscriber.onError(new ResponseZerokitError("JSONException").toJSON());
                }
            }
        }, new Action<String>() {
            @Override
            public void call(String error) {
                subscriber.onError(new ResponseZerokitError(error).toJSON());
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    void callFunctionIdp() {
        initCheck().enqueue(new Action<Void>() {
            @Override
            public void call(Void aVoid) {
                loadUrl(webViewIDP, "javascript:\n" +
                        idpHelperJavaScriptSource +
                        "\ncallFunction();");
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    void callFuncionIdpUrl(final String url) {
        initCheck().enqueue(new Action<Void>() {
            @Override
            public void call(Void aVoid) {
                loadUrl(webViewIDP, url);
            }
        });
    }

    /**
     * Loads an url in the WebView instance (on the Handler thread)
     *
     * @param url The url which will be loaded in the webview
     */
    @SuppressWarnings("WeakerAccess")
    void loadUrl(@NonNull final WebView webView, @NonNull final String url) {
        executorWebView.execute(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(url);
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    void storeSecret(@NonNull String alias, @NonNull String secret) {
        Context context = webView.getContext();
        FileOutputStream fileOutputStream = null;
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            // Create the keys if necessary
            if (!keyStore.containsAlias(alias)) generateNewKey(context, alias);

            Cipher inCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            inCipher.init(Cipher.ENCRYPT_MODE, ((KeyStore.PrivateKeyEntry) keyStore.getEntry(alias, null)).getCertificate().getPublicKey());

            fileOutputStream = new FileOutputStream(new File(context.getFilesDir(), KEY_STORE_PATH));
            CipherOutputStream cipherOutputStream = new CipherOutputStream(fileOutputStream, inCipher);
            cipherOutputStream.write(secret.getBytes("UTF-8"));
            cipherOutputStream.close();

        } catch (NoSuchAlgorithmException | NoSuchProviderException | InvalidAlgorithmParameterException | KeyStoreException | CertificateException | IOException | UnrecoverableEntryException | InvalidKeyException | UnsupportedOperationException | NoSuchPaddingException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (fileOutputStream != null)
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
        }
    }


    @SuppressWarnings("WeakerAccess")
    String getSecret(@NonNull String alias) {
        Context context = webView.getContext();
        FileInputStream fileInputStream = null;
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);

            Cipher outCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
            KeyStore.Entry entry = keyStore.getEntry(alias, null);
            if (entry != null) {
                outCipher.init(Cipher.DECRYPT_MODE, ((KeyStore.PrivateKeyEntry) entry).getPrivateKey());

                fileInputStream = new FileInputStream(new File(context.getFilesDir(), KEY_STORE_PATH));
                CipherInputStream cipherInputStream = new CipherInputStream(fileInputStream, outCipher);

                int nextByte;
                ArrayList<Byte> values = new ArrayList<>();
                while ((nextByte = cipherInputStream.read()) != -1) values.add((byte) nextByte);
                byte[] bytes = new byte[values.size()];
                for (int i = 0; i < bytes.length; i++) bytes[i] = values.get(i);
                return new String(bytes, 0, bytes.length, "UTF-8");
            }

        } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException | UnrecoverableEntryException | InvalidKeyException | UnsupportedOperationException | NoSuchPaddingException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } finally {
            if (fileInputStream != null)
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, Log.getStackTraceString(e));
                }
        }
        return null;
    }

    @SuppressWarnings("WeakerAccess")
    void deleteSecret(@NonNull String alias) {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
            keyStore.load(null);
            keyStore.deleteEntry(alias);
        } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException | UnsupportedOperationException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
    }

    private void generateNewKey(@NonNull Context context, @NonNull String alias) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        Calendar notBefore = Calendar.getInstance();
        Calendar notAfter = Calendar.getInstance();
        notAfter.add(Calendar.YEAR, 1);

        KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                .setAlias(alias)
                .setSubject(new X500Principal("CN=zerokit"))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(notBefore.getTime())
                .setEndDate(notAfter.getTime())
                .build();
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", ANDROID_KEYSTORE);
        generator.initialize(spec);
        generator.generateKeyPair();
    }

    @SuppressWarnings("WeakerAccess")
    String getSha256(String text) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(text.getBytes("UTF-8"));
        return Base64.encodeToString(messageDigest.digest(), Base64.URL_SAFE).split("=")[0];
    }

    @SuppressWarnings("WeakerAccess")
    String getRandomString(int length) {
        StringBuilder sb = new StringBuilder(length);
        int lengthAll = ALPHANUMERIC_CHARACTERS.length();
        for (int i = 0; i < length; i++)
            sb.append(ALPHANUMERIC_CHARACTERS.charAt(secureRandom.nextInt(lengthAll)));
        return sb.toString();
    }

    @SuppressWarnings("WeakerAccess")
    void requireIdp(final String clientId, final boolean useProofKey, final Callback<? super IdentityTokens, ? super ResponseZerokitError> callback) {
        try {
            final String redirectUriAuthority = String.format(REDIRECT_URI_FORMAT_PATTERN, clientId, tenantId).toLowerCase();

            final String nonce = getRandomString(64);
            final String state = getRandomString(64);
            Uri.Builder builder = initIdpUriBuilder(nonce, state, clientId);

            final String codeVerifier = useProofKey ? getRandomString(64) : null;
            if (useProofKey)
                builder.appendQueryParameter("code_challenge_method", "S256")
                        .appendQueryParameter("code_challenge", getSha256(codeVerifier));

            final PageFinishListener[] pageFinishListener = new PageFinishListener[1];
            final InterfaceHtmlExporter[] interfaceHtmlExporter = new InterfaceHtmlExporter[1];

            pageFinishListener[0] = new PageFinishListener() {

                @Override
                public void onReceivedError(int errorCode) {

                }

                @Override
                public void onPageFinished(String url) {
                    Uri uri = Uri.parse(url);
                    if (redirectUriAuthority.equals(uri.getAuthority())) {
                        clientIdpWebView.removePageFinishListener(this);
                        String fragment = uri.getFragment();
                        if (!TextUtils.isEmpty(fragment)) {

                            Map<String, String> parameters = new HashMap<>();
                            for (String pair : fragment.split("&")) {
                                String[] split = pair.split("=");
                                parameters.put(split[0], split[1]);
                            }

                            if (parameters.containsKey(param_error)) {
                                String error = parameters.get(param_error);
                                String error_desc = parameters.containsKey(param_error_description) ? parameters.get(param_error_description) : "";
                                if (!useProofKey && "invalid_request".equals(error) && "code challenge required".equals(error_desc)) {
                                    jsInterfaceHtmlExporter.removeListener(interfaceHtmlExporter[0]);
                                    requireIdp(clientId, true, callback);
                                } else {
                                    callback.onError(new ResponseZerokitError(error, error_desc));
                                }
                            } else if (parameters.containsKey(param_code)) {
                                if (!state.equals(parameters.get(param_state)))
                                    callback.onError(new ResponseZerokitError("The state has changed during the idp process"));
                                else
                                    callback.onSuccess(new IdentityTokens(parameters.get(param_code), parameters.containsKey(param_id_token) ? parameters.get(param_id_token) : "", codeVerifier));
                            }
                        } else {
                            if (uri.getQueryParameterNames().contains(param_error))
                                callback.onError(new ResponseZerokitError(uri.getQueryParameter(param_error)));
                        }
                    } else callFunctionIdp();
                }
            };

            interfaceHtmlExporter[0] = new InterfaceHtmlExporter() {
                @Override
                public void onGetError(String error, String description) {
                    if (!TextUtils.isEmpty(error) || !TextUtils.isEmpty(error)) {
                        callback.onError(new ResponseZerokitError(error, description));
                        jsInterfaceHtmlExporter.removeListener(this);
                        clientIdpWebView.removePageFinishListener(pageFinishListener[0]);
                    }
                }
            };

            clientIdpWebView.addPageFinishListener(pageFinishListener[0]);
            jsInterfaceHtmlExporter.addListener(interfaceHtmlExporter[0]);

            callFuncionIdpUrl(builder.build().toString());
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            callback.onError(new ResponseZerokitError(e.getMessage()));
        }
    }


    @SuppressWarnings("WeakerAccess")
    Uri.Builder initIdpUriBuilder(String nonce, String state, String clientId) {
        return new Uri.Builder()
                .scheme(HTTPS_SCHEME)
                .authority(apiRoot)
                .appendPath("idp")
                .appendPath("connect")
                .appendPath("authorize")
                .appendQueryParameter("client_id", clientId)
                .appendQueryParameter("redirect_uri", new Uri.Builder().scheme(HTTPS_SCHEME).authority(String.format(REDIRECT_URI_FORMAT_PATTERN, clientId, tenantId)).appendPath("").build().toString())
                .appendQueryParameter("response_type", "code id_token")
                .appendQueryParameter("scope", "openid profile")
                .appendQueryParameter("state", state)
                .appendQueryParameter("nonce", nonce)
                .appendQueryParameter("response_mode", "fragment")
                .appendQueryParameter("prompt", "none");
    }

    /**
     * Converts char array to byte array securely
     *
     * @param chars the char array which will be converted to byte array
     * @return the byte array
     */
    @NonNull
    private byte[] toBytes(@NonNull char[] chars) {
        CharBuffer charBuffer = CharBuffer.wrap(chars);
        ByteBuffer byteBuffer = Charset.forName("UTF-8").encode(charBuffer);
        byte[] bytes = Arrays.copyOfRange(byteBuffer.array(), byteBuffer.position(), byteBuffer.limit());
        Arrays.fill(charBuffer.array(), '\u0000'); // clear sensitive data
        Arrays.fill(byteBuffer.array(), (byte) 0); // clear sensitive data
        return bytes;
    }

    @SuppressWarnings("WeakerAccess")
    boolean isNetworkAvailable() {
        NetworkInfo activeNetworkInfo = ((ConnectivityManager) webView.getContext().getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Send a log message if app is in debug mode
     *
     * @param msg The message you would like logged.
     */
    @SuppressWarnings("WeakerAccess")
    void log(@NonNull String msg) {
        if (BuildConfig.DEBUG)
            Log.d("Zerokit", msg);
    }

    @SuppressWarnings("WeakerAccess")
    void incrementIdlingResoure() {
        if (idlingResource != null) idlingResource.increment();
    }

    @SuppressWarnings("WeakerAccess")
    void decrementIdlingResource() {
        if (idlingResource != null) idlingResource.decrement();
    }

    private class ZerokitCountingIdlingResource implements IdlingResource {

        private final AtomicInteger counter;

        private volatile ResourceCallback resourceCallback;

        ZerokitCountingIdlingResource() {
            this(0);
        }

        ZerokitCountingIdlingResource(int initialCount) {
            this.counter = new AtomicInteger(initialCount);
        }

        @Override
        public String getName() {
            return getClass().getName();
        }

        @Override
        public boolean isIdleNow() {
            return counter.get() == 0;
        }

        @Override
        public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
            this.resourceCallback = resourceCallback;
        }

        void increment() {
            counter.getAndIncrement();
        }

        void decrement() {
            int counterVal = counter.decrementAndGet();
            if (counterVal == 0) {
                if (null != resourceCallback) {
                    resourceCallback.onTransitionToIdle();
                }
            }

            if (counterVal < 0) {
                throw new IllegalArgumentException("Counter has been corrupted!");
            }
        }
    }

    @VisibleForTesting
    @NonNull
    public IdlingResource getIdlingResource() {
        if (idlingResource == null) idlingResource = new ZerokitCountingIdlingResource();
        return idlingResource;
    }


    private class JSInterfaceByteArrayProvider {

        private final Map<String, byte[]> byteArrays;

        /**
         * Package private constructor to avoid method generation to access private constructor of inner class
         */
        JSInterfaceByteArrayProvider() {
            byteArrays = new HashMap<>();
        }

        public String add(@NonNull byte[] array) {
            String key = UUID.randomUUID().toString();
            byteArrays.put(key, Arrays.copyOf(array, array.length));
            return key;
        }

        void remove(String key) {
            byte[] bytes = byteArrays.get(key);
            if (bytes != null) Arrays.fill(bytes, (byte) 0);
            byteArrays.remove(key);
        }

        @JavascriptInterface
        @SuppressWarnings("unused")
        public byte getByte(String id, int index) {
            return byteArrays.get(id)[index];
        }


        @JavascriptInterface
        @SuppressWarnings("unused")
        public int getLength(String id) {
            return byteArrays.get(id).length;
        }
    }

    interface InterfaceHtmlExporter {

        void onGetError(String error, String description);
    }

    private class JSInterfaceHtmlExporter implements InterfaceHtmlExporter {

        final List<InterfaceHtmlExporter> listeners;

        JSInterfaceHtmlExporter() {
            listeners = new ArrayList<>();
        }

        void addListener(InterfaceHtmlExporter listener) {
            listeners.add(listener);
        }

        void removeListener(InterfaceHtmlExporter listener) {
            listeners.remove(listener);
        }

        void removeAllListeners() {
            listeners.clear();
        }

        @JavascriptInterface
        @Override
        public void onGetError(final String error, final String description) {
            for (InterfaceHtmlExporter listener : listeners)
                listener.onGetError(error, description);
        }
    }

    /**
     * The Java interface which is callable from the javascript side and handles the results from the promises
     */
    private class JSInterfaceResponseHandler {

        /**
         * Package private constructor to avoid method generation to access private constructor of inner class
         */
        JSInterfaceResponseHandler() {
        }

        /**
         * Called if a function has finished successfully
         *
         * @param result the result which is returned by the javascript method
         * @param key    the key which identify which observers are responsible for handle the result
         */
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void onSuccess(final String result, final String key) {
            final CallbackExecutor<? super String, ? super String> subscriber = observers.get(key);
            if (subscriber != null) {
                subscriber.onSuccess(result);
                observers.remove(key);
                decrementIdlingResource();
            }
        }

        /**
         * Called if a function has finished with error
         *
         * @param result the error result which is returned by the javascript method
         * @param key    the key which identify which observers are responsible for handle the error
         */
        @JavascriptInterface
        @SuppressWarnings("unused")
        public void onError(final String result, final String key) {
            final CallbackExecutor<? super String, ? super String> subscriber = observers.get(key);
            if (subscriber != null) {
                subscriber.onError(result);
                observers.remove(key);
                decrementIdlingResource();
            }
        }
    }

    private enum ArgType {
        ByteArray, JSONToken
    }

    private static class ExtraArg {
        private final ArgType type;
        private final int position;

        ExtraArg(ArgType type) {
            this(type, -1);
        }

        ExtraArg(ArgType type, int position) {
            this.type = type;
            this.position = position;
        }
    }

    /**
     * An enum which represents a javascript function with parameters
     * From this will be generated the concrete javascript function
     */
    enum Function {
        zxcvbn(Default, null, new ExtraArg(ArgType.ByteArray, 0)),

        login(MobileCmd, null, new ExtraArg(ArgType.ByteArray)),
        loginByRememberMeKey(MobileCmd, null),
        getRememberMeKey(MobileCmd, null, new ExtraArg(ArgType.ByteArray)),
        register(MobileCmd, null, new ExtraArg(ArgType.ByteArray)),
        createInvitationLink(MobileCmd, null, new ExtraArg(ArgType.ByteArray)),
        acceptInvitationLink(MobileCmd, null, new ExtraArg(ArgType.JSONToken, 0), new ExtraArg(ArgType.ByteArray)),
        changePassword(MobileCmd, null, new ExtraArg(ArgType.ByteArray), new ExtraArg(ArgType.ByteArray)),

        acceptInvitationLinkNoPassword(new ExtraArg(ArgType.JSONToken, 0)),
        createInvitationLinkNoPassword,
        createTresor,
        decrypt,
        encrypt,
        getInvitationLinkInfo(Cmd, ArgType.JSONToken),
        kickFromTresor,
        logout,
        shareTresor,
        whoAmI;


        @IntDef({Default, Cmd, MobileCmd})
        @interface Type {
            int Default = 0;
            int Cmd = 1;
            int MobileCmd = 2;
        }

        /**
         * Modifies the url, which the function will be called on
         */
        @Type
        private final int type;
        private final ExtraArg[] extraArgs;
        private final ArgType responseFormatter;

        /**
         * Constructs a Function with the given parameters, which will represent a javascript function.
         * The isMobile is false by default
         */
        Function() {
            this(Cmd, null);
        }

        Function(@Type int type) {
            this(type, null);
        }

        Function(ExtraArg... extraArgs) {
            this(Cmd, null, extraArgs);
        }


        /**
         * Constructs a Function with the given parameters, which will represent a javascript function
         */
        Function(@Type int type, ArgType responseFormatter, ExtraArg... extraArgs) {
            this.type = type;
            this.extraArgs = extraArgs;
            this.responseFormatter = responseFormatter;
        }

    }

    /**
     * Interface for page finish listening
     */
    interface PageFinishListener {


        /**
         * Notify the host application that a page has finished loading. This method
         * is called only for main frame. When onPageFinished() is called, the
         * rendering picture may not be updated yet. To get the notification for the
         * new Picture, use {@link WebView.PictureListener#onNewPicture}.
         *
         * @param url The url of the page.
         */
        void onPageFinished(@SuppressWarnings("UnusedParameters") String url);

        /**
         * Report an error to the host application. These errors are unrecoverable
         * (i.e. the main resource is unavailable). The errorCode parameter
         * corresponds to one of the ERROR_* constants.
         * @param errorCode The error code corresponding to an ERROR_* value.
         */
        void onReceivedError(int errorCode);
    }


    interface StateChangeListener {
        void onStateChanged(@State int state);
    }

    private class StateHandler {
        private final List<StateChangeListener> stateChangeListeners;

        @State
        private int state;

        StateHandler() {
            state = State.NOT_STARTED;
            stateChangeListeners = new ArrayList<>();
        }

        private void setState(@State int newState) {
            if (newState != state){
                this.state = newState;
                for (StateChangeListener stateChangeListener : new ArrayList<>(stateChangeListeners))
                    stateChangeListener.onStateChanged(state);
            }
        }

        @State
        int getState() {
            return state;
        }

        void addListener(StateChangeListener listener) {
            stateChangeListeners.add(listener);
        }

        void removeListener(StateChangeListener listener) {
            stateChangeListeners.remove(listener);
        }
    }

    /**
     * WebViewClient subclass, which can handle more than one pageFinishListener registration
     */
    private class ZerokitWebViewClientBase extends WebViewClient {
        /**
         * The registered PageFinishListeners
         */
        final List<PageFinishListener> pageFinishListeners;

        /**
         * Constructs a ZerokitWebViewClient
         */
        ZerokitWebViewClientBase() {
            pageFinishListeners = new LinkedList<>();
        }

        /**
         * Register a PageFinishListener
         *
         * @param pageFinishListener which will be registered
         */
        void addPageFinishListener(@NonNull PageFinishListener pageFinishListener) {
            pageFinishListeners.add(pageFinishListener);
        }


        /**
         * Remove a registered a PageFinishListener
         *
         * @param pageFinishListener which will be removed
         */
        void removePageFinishListener(@NonNull PageFinishListener pageFinishListener) {
            pageFinishListeners.remove(pageFinishListener);
        }

        /**
         * Notify the host application that a page has finished loading. This method
         * is called only for main frame. When onPageFinished() is called, the
         * rendering picture may not be updated yet. To get the notification for the
         * new Picture, use {@link WebView.PictureListener#onNewPicture}.
         *
         * @param view The WebView that is initiating the callback.
         * @param url  The url of the page.
         */
        @Override
        public void onPageFinished(@NonNull final WebView view, @NonNull final String url) {
            super.onPageFinished(view, url);
            for (PageFinishListener pageFinishListener : new LinkedList<>(pageFinishListeners)) {
                pageFinishListener.onPageFinished(url);
            }
        }

        /**
         * Report an error to the host application. These errors are unrecoverable
         * (i.e. the main resource is unavailable). The errorCode parameter
         * corresponds to one of the ERROR_* constants.
         * @param view The WebView that is initiating the callback.
         * @param errorCode The error code corresponding to an ERROR_* value.
         * @param description A String describing the error.
         * @param failingUrl The url that failed to load.
         */
        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
            for (PageFinishListener pageFinishListener : new LinkedList<>(pageFinishListeners)) {
                pageFinishListener.onReceivedError(errorCode);
            }
        }

    }

    /**
     * WebChromeClient implementation which is responsible for handle console errors
     */
    private class ZerokitWebChromeClient extends WebChromeClient {

        /**
         * Package private constructor to avoid method generation to access private constructor of inner class
         */
        ZerokitWebChromeClient() {
        }

        /**
         * Report a JavaScript console message to the host application. The ChromeClient
         * should override this to process the log message as they see fit.
         *
         * @param consoleMessage Object containing details of the console message.
         * @return true if the message is handled by the client.
         */
        @Override
        public boolean onConsoleMessage(@NonNull ConsoleMessage consoleMessage) {
            if (consoleMessage.messageLevel() == ConsoleMessage.MessageLevel.ERROR) {
                for (Callback<? super String, ? super String> callback : observers.values())
                    callback.onError(new ResponseZerokitError(!isNetworkAvailable() ? "No network connection" : consoleMessage.message()).toJSON());
                observers.clear();
            }
            return true;
        }
    }

    private abstract class CallbackByteArrayIds<T> implements Callback<String, String> {

        String[] ids = null;

        /**
         * the subscriber which will handle the results
         */
        final Callback<? super T, ? super ResponseZerokitError> subscriber;

        private CallbackByteArrayIds(@NonNull Callback<? super T, ? super ResponseZerokitError> subscriber) {
            this(subscriber, (String) null);
        }

        private CallbackByteArrayIds(@NonNull Callback<? super T, ? super ResponseZerokitError> subscriber, String... ids) {
            this.ids = ids;
            this.subscriber = subscriber;
        }

        @Override
        public void onError(@NonNull String e) {
            Zerokit.this.log("onError " + e);
            if (ids != null)
                for (String id : ids) jsInterfaceByteArrayProvider.remove(id);
            subscriber.onError(new ResponseZerokitError().parse(e));
        }

        @Override
        public void onSuccess(@NonNull String result) {
            Zerokit.this.log("onSuccess " + result);
            if (ids != null)
                for (String id : ids) jsInterfaceByteArrayProvider.remove(id);
            subscriber.onSuccess(getResult(result));
        }

        abstract T getResult(String result);
    }

    /**
     * Observer implementation for handle JSON responses
     *
     * @param <T> the concrete JSON response type
     */
    private class CallbackJsonResult<T extends ZerokitJson> extends CallbackByteArrayIds<T> {

        private final T response;

        /**
         * Constructs an observer object which can handle JSON responses
         *
         * @param subscriber which will handle the responses
         * @param response   the response object
         */
        CallbackJsonResult(@NonNull Callback<? super T, ? super ResponseZerokitError> subscriber, @NonNull T response) {
            super(subscriber);
            this.response = response;
        }

        CallbackJsonResult(@NonNull Callback<? super T, ? super ResponseZerokitError> subscriber, @NonNull T response, String... ids) {
            super(subscriber, ids);
            this.response = response;
        }

        @Override
        @NonNull
        T getResult(@NonNull String result) {
            return response.parse(result);
        }
    }

    /**
     * Observer implementation for handle String responses
     */
    private class CallbackStringResult extends CallbackByteArrayIds<String> {

        /**
         * Constructs an observer object which can handle JSON responses
         *
         * @param subscriber which will handle the responses
         */
        CallbackStringResult(@NonNull Callback<? super String, ? super ResponseZerokitError> subscriber) {
            super(subscriber);
        }

        CallbackStringResult(@NonNull Callback<? super String, ? super ResponseZerokitError> subscriber, String... ids) {
            super(subscriber, ids);
        }

        @Override
        @NonNull
        String getResult(@NonNull String result) {
            if (!"null".equals(result)) try {
                return (String) new JSONTokener(result).nextValue();
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return result;
        }
    }


    /**
     * This method tries to log in the given user with the given password entered by the user
     *
     * @param userId   The userId of the user to log in.
     * @param password The password of the user to log in.
     * @return Resolved userId of the logged in user.
     */
    @NonNull
    @SuppressWarnings("WeakerAccess")
    CallAsync<ResponseZerokitLogin, ResponseZerokitError> _login(@NonNull final String userId, @NonNull final byte[] password) {
        return new CallAsyncAction<>(new ActionCallback<ResponseZerokitLogin, ResponseZerokitError>() {

            @Override
            public void call(final Callback<? super ResponseZerokitLogin, ? super ResponseZerokitError> subscriber) {
                callFunction(Function.login, new CallbackJsonResult<>(subscriber, new ResponseZerokitLogin(), jsInterfaceByteArrayProvider.add(password)), userId);
            }
        });
    }

    /**
     * Use this method for login if 'remember me' was set to yes for a previous login with password.
     *
     * @param userId The user ID to log in with
     * @param key    The remember key
     * @return Resolved userId of the logged in user.
     */
    @NonNull
    @SuppressWarnings("WeakerAccess")
    CallAsync<String, ResponseZerokitError> _loginByRememberMeKey(@NonNull final String userId, @NonNull final String key) {
        return new CallAsyncAction<>(new ActionCallback<String, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.loginByRememberMeKey, new CallbackStringResult(subscriber), userId, key);
            }
        });
    }

    /**
     * Returns the "remember key" for the given user
     *
     * @param userId   The user ID for the key
     * @param password The password for the given user id
     * @return the key
     */
    @NonNull
    @SuppressWarnings("WeakerAccess")
    CallAsync<String, ResponseZerokitError> _getRememberMeKey(@NonNull final String userId, @NonNull final byte[] password) {
        return new CallAsyncAction<>(new ActionCallback<String, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseZerokitError> subscriber) {
                callFunction(Function.getRememberMeKey, new CallbackStringResult(subscriber, jsInterfaceByteArrayProvider.add(password)), userId);
            }
        });
    }


    /**
     * Log out the current user
     *
     * @return Observable about the results
     */
    @NonNull
    @SuppressWarnings("WeakerAccess")
    CallAsync<String, ResponseZerokitError> _logout() {
        return new CallAsyncAction<>(new ActionCallback<String, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.logout, new CallbackStringResult(subscriber));
            }
        });
    }

    /**
     * Use this methods to get the logged in user's identity.
     *
     * @return the user ID if logged in or `null` if not.
     */
    @NonNull
    CallAsync<String, ResponseZerokitError> _whoAmI() {
        return new CallAsyncAction<>(new ActionCallback<String, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.whoAmI, new CallbackStringResult(subscriber));
            }
        });
    }

    /**
     * This method logs into a security session and changes the password of the user.
     *
     * @param userId      Parameter to specify the id of the user changing password. This is only required if the user is not logged in.
     * @param oldPassword The currently used password
     * @param newPassword The new password
     * @return Resolves to the userId of the logged in user
     */
    @NonNull
    @SuppressWarnings("WeakerAccess")
    CallAsync<ResponseZerokitChangePassword, ResponseZerokitError> _changePassword(@NonNull final String userId, @NonNull final byte[] oldPassword, @NonNull final byte[] newPassword) {
        return new CallAsyncAction<>(new ActionCallback<ResponseZerokitChangePassword, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super ResponseZerokitChangePassword, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.changePassword, new CallbackJsonResult<>(subscriber, new ResponseZerokitChangePassword(), jsInterfaceByteArrayProvider.add(oldPassword), jsInterfaceByteArrayProvider.add(newPassword)), userId);
            }
        });
    }


    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    // ====================================
    //  Identity tokens
    // ====================================
    /**
     * Get authorization code and identity tokens for the currenty logged in user.
     *
     * @param clientId The cliend ID for the current ZeroKit OpenID Connect client set up in the management portal.
     * @return IdentityTokens which contains: Authorization code, Identity token, Code Verifier (Contains the code verifier if you have 'Requires proof key' enabled for your client)
     */
    @NonNull
    @SuppressWarnings("WeakerAccess")
    public Call<IdentityTokens, ResponseZerokitError> getIdentityTokens(final String clientId) {
        return getIdentityTokens(clientId, false);
    }

    /**
     * Get authorization code and identity tokens for the currenty logged in user.
     *
     * @param clientId The cliend ID for the current ZeroKit OpenID Connect client set up in the management portal.
     * @param useProofKey Option to use proof key
     * @return IdentityTokens which contains: Authorization code, Identity token, Code Verifier (Contains the code verifier if you have 'Requires proof key' enabled for your client)
     */
    @NonNull
    private Call<IdentityTokens, ResponseZerokitError> getIdentityTokens(final String clientId, final boolean useProofKey) {
        return new CallAction<>(new ActionCallback<IdentityTokens, ResponseZerokitError>() {

            @Override
            public void call(final Callback<? super IdentityTokens, ? super ResponseZerokitError> subscriberInner) {
                idpCheck().enqueue(new Action<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        idpStateHandler.setState(State.RUNNING);

                        requireIdp(clientId, useProofKey, new CallbackExecutor<>(new Callback<IdentityTokens, ResponseZerokitError>() {
                            @Override
                            public void onSuccess(IdentityTokens result) {
                                subscriberInner.onSuccess(result);
                                idpStateHandler.setState(State.FINISHED);
                            }

                            @Override
                            public void onError(ResponseZerokitError e) {
                                subscriberInner.onError(e);
                                idpStateHandler.setState(State.FINISHED);
                            }
                        }));
                    }
                });
            }
        });
    }

    // ====================================
    //  Password
    // ====================================

    /**
     * This methods gives meta-information about the password the user entered
     *
     * @param password The password to get the strength of it
     * @return Part of the result of running zxcvbn on the password
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitPasswordStrength, ResponseZerokitError> getPasswordStrength(@NonNull final byte[] password) {
        return new CallAction<>(new ActionCallback<ResponseZerokitPasswordStrength, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super ResponseZerokitPasswordStrength, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.zxcvbn, new CallbackJsonResult<>(subscriber, new ResponseZerokitPasswordStrength(), jsInterfaceByteArrayProvider.add(password)));
            }
        });
    }

    /**
     * This methods gives meta-information about the password the user entered
     *
     * @param passwordExporter The password to get the strength of it
     * @return Part of the result of running zxcvbn on the password
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitPasswordStrength, ResponseZerokitError> getPasswordStrength(@NonNull final PasswordEditText.PasswordExporter passwordExporter) {
        return getPasswordStrength(toBytes(passwordExporter.getCharArray(false)));
    }

    /**
     * This methods gives meta-information about the password the user entered
     *
     * @param passwordEditText The password to get the strength of it
     * @return Part of the result of running zxcvbn on the password
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitPasswordStrength, ResponseZerokitError> getPasswordStrength(@NonNull final PasswordEditText passwordEditText) {
        return getPasswordStrength(passwordEditText.getPasswordExporter());
    }

    // ====================================
    //  Invitation links
    // ====================================

    /**
     * A link with no password can be accepted by any logged in user that has access to the token returned by getInvitationLinkInfo through the basic sdk.
     *
     * @param token The token is the token field of the InvitationLinkPublicInfo of the link returned by getInvitationLinkInfo.
     * @return Resolves to the operation id that must be approved for the operation to be effective.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<String, ResponseZerokitError> acceptInvitationLinkNoPassword(@NonNull final String token) {
        return new CallAction<>(new ActionCallback<String, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.acceptInvitationLinkNoPassword, new CallbackStringResult(subscriber, token));
            }
        });
    }

    /**
     * You can get some information about the link by calling getInvitationLinkInfo with the link secret.
     * The returned object contains a token necessary to accept the invitation.
     * This also is a client side secret, that should never be uploaded to your site as that would compromise the zero knowledge nature of the system by providing ways to open the tresor.
     *
     * @param secret The secret is the one that was concatenated to the end of the url in createInvitationLink.
     * @return Resolves to all the information available.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitInvitationLinkInfo, ResponseZerokitError> getInvitationLinkInfo(@NonNull final String secret) {
        return new CallAction<>(new ActionCallback<ResponseZerokitInvitationLinkInfo, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super ResponseZerokitInvitationLinkInfo, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.getInvitationLinkInfo, new CallbackJsonResult<>(subscriber, new ResponseZerokitInvitationLinkInfo()), secret);
            }
        });
    }


    /**
     * You can create an invitation link with no password.
     *
     * @param linkBase the base of the link. The link secret is concatenated after this after a '#'
     * @param tresorId the id of the tresor
     * @param message  optional arbitrary string data that can be retrieved without a password or any other information
     * @return Resolves to the operation id and the url of the created link. The operation must be approved before the link is enabled.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitCreateInvitationLink, ResponseZerokitError> createInvitationLinkNoPassword(@NonNull final String linkBase, @NonNull final String tresorId, @Nullable final String message) {
        return new CallAction<>(new ActionCallback<ResponseZerokitCreateInvitationLink, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super ResponseZerokitCreateInvitationLink, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.createInvitationLinkNoPassword, new CallbackJsonResult<>(subscriber, new ResponseZerokitCreateInvitationLink()), linkBase, tresorId, TextUtils.isEmpty(message) ? "" : message);
            }
        });
    }

    /**
     * This method creates an invitation link with the password entered
     *
     * @param linkBase the base of the link. The link secret is concatenated after this after a '#'
     * @param tresorId the id of the tresor
     * @param message  optional arbitrary string data that can be retrieved without a password or any other information
     * @param password the password to accept the link
     * @return Resolves to the operation id and the url of the created link. The operation must be approved before the link is enabled.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitCreateInvitationLink, ResponseZerokitError> createInvitationLink(@NonNull final String linkBase, @NonNull final String tresorId, @Nullable final String message, @NonNull final byte[] password) {
        return new CallAction<>(new ActionCallback<ResponseZerokitCreateInvitationLink, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super ResponseZerokitCreateInvitationLink, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.createInvitationLink, new CallbackJsonResult<>(subscriber, new ResponseZerokitCreateInvitationLink(), jsInterfaceByteArrayProvider.add(password)), linkBase, tresorId, TextUtils.isEmpty(message) ? "" : message);
            }
        });
    }

    /**
     * This method creates an invitation link with the password entered
     *
     * @param linkBase         the base of the link. The link secret is concatenated after this after a '#'
     * @param tresorId         the id of the tresor
     * @param message          optional arbitrary string data that can be retrieved without a password or any other information
     * @param passwordExporter the passwordexporter that holds the password to accept the link
     * @return Resolves to the operation id and the url of the created link. The operation must be approved before the link is enabled.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitCreateInvitationLink, ResponseZerokitError> createInvitationLink(@NonNull final String linkBase, @NonNull final String tresorId, @Nullable final String message, @NonNull PasswordEditText.PasswordExporter passwordExporter) {
        return createInvitationLink(linkBase, tresorId, message, toBytes(passwordExporter.getCharArray(true)));
    }

    /**
     * This method creates an invitation link with the password entered
     *
     * @param linkBase         the base of the link. The link secret is concatenated after this after a '#'
     * @param tresorId         the id of the tresor
     * @param message          optional arbitrary string data that can be retrieved without a password or any other information
     * @param passwordEditText the passwordEditText that holds the password to accept the link
     * @return Resolves to the operation id and the url of the created link. The operation must be approved before the link is enabled.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitCreateInvitationLink, ResponseZerokitError> createInvitationLink(@NonNull final String linkBase, @NonNull final String tresorId, @Nullable final String message, @NonNull PasswordEditText passwordEditText) {
        return createInvitationLink(linkBase, tresorId, message, passwordEditText.getPasswordExporter());
    }

    /**
     * This method will add the user to the tresor of the link using the password entered.
     *
     * @param token    The token is the $token field of the InvitationLinkPublicInfo of the link returned by getInvitationLinkInfo.
     * @param password The password for the link
     * @return Resolves to the operation id that must be approved for the operation to be effective.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<String, ResponseZerokitError> acceptInvitationLink(@NonNull final String token, @NonNull final byte[] password) {
        return new CallAction<>(new ActionCallback<String, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.acceptInvitationLink, new CallbackStringResult(subscriber, token, jsInterfaceByteArrayProvider.add(password)));
            }
        });
    }


    /**
     * This method will add the user to the tresor of the link using the password entered.
     *
     * @param token            The token is the $token field of the InvitationLinkPublicInfo of the link returned by getInvitationLinkInfo.
     * @param passwordExporter The passwordexporter that holds the password for the link
     * @return Resolves to the operation id that must be approved for the operation to be effective.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<String, ResponseZerokitError> acceptInvitationLink(@NonNull final String token, PasswordEditText.PasswordExporter passwordExporter) {
        return acceptInvitationLink(token, toBytes(passwordExporter.getCharArray(true)));
    }

    /**
     * This method will add the user to the tresor of the link using the password entered.
     *
     * @param token            The token is the $token field of the InvitationLinkPublicInfo of the link returned by getInvitationLinkInfo.
     * @param passwordEditText The passwordEditText that holds the password for the link
     * @return Resolves to the operation id that must be approved for the operation to be effective.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<String, ResponseZerokitError> acceptInvitationLink(@NonNull final String token, PasswordEditText passwordEditText) {
        return acceptInvitationLink(token, passwordEditText.getPasswordExporter());
    }

    // ====================================
    //  User handling
    // ====================================

    /**
     * Registers the user in ZKit with the provided userId.
     * The returned value is the regValidationVerifier which is used during user validation, so it should be saved on the app server.
     *
     * @param userId       The userId provided by the InitUserRegistration API call for the given alias
     * @param regSessionId The regSessionId provided by the InitUserRegistration API call for the given alias
     * @param password     The password provided by the User
     * @return the RegValidationVerifier property.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitRegister, ResponseZerokitError> register(@NonNull final String userId, @NonNull final String regSessionId, @NonNull final byte[] password) {
        return new CallAction<>(new ActionCallback<ResponseZerokitRegister, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super ResponseZerokitRegister, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.register, new CallbackJsonResult<>(subscriber, new ResponseZerokitRegister(), jsInterfaceByteArrayProvider.add(password)), userId, regSessionId);
            }
        });
    }

    /**
     * Registers the user in ZKit with the provided userId
     * The returned value is the regValidationVerifier which is used during user validation, so it should be saved on the app server.
     *
     * @param userId           The userId provided by the InitUserRegistration API call for the given alias
     * @param regSessionId     The regSessionId provided by the InitUserRegistration API call for the given alias
     * @param passwordEditText field that holds the password provided by the user
     * @return the RegValidationVerifier property.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitRegister, ResponseZerokitError> register(@NonNull final String userId, @NonNull final String regSessionId, @NonNull PasswordEditText passwordEditText) {
        return register(userId, regSessionId, passwordEditText.getPasswordExporter());
    }

    /**
     * Registers the user in ZKit with the provided userId
     * The returned value is the regValidationVerifier which is used during user validation, so it should be saved on the app server.
     *
     * @param userId           The userId provided by the InitUserRegistration API call for the given alias
     * @param regSessionId     The regSessionId provided by the InitUserRegistration API call for the given alias
     * @param passwordExporter exporter that holds the password provided by the User
     * @return the RegValidationVerifier property.
     */
    @NonNull
    public Call<ResponseZerokitRegister, ResponseZerokitError> register(@NonNull final String userId, @NonNull final String regSessionId, @NonNull PasswordEditText.PasswordExporter passwordExporter) {
        return register(userId, regSessionId, toBytes(passwordExporter.getCharArray(true)));
    }

    /**
     * This method tries to log in the given user with the given password entered by the user
     *
     * @param userId     The userId of the user to log in.
     * @param password   The password of the user to log in.
     * @param rememberMe If true, than next time the login without password will be possible
     * @return Resolved userId of the logged in user.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final byte[] password, final boolean rememberMe) {
        return new CallAction<>(new ActionCallback<ResponseZerokitLogin, ResponseZerokitError>() {
            @Override
            public void call(final Callback<? super ResponseZerokitLogin, ? super ResponseZerokitError> subscriber) {
                _login(userId, password).enqueue(new Action<ResponseZerokitLogin>() {
                    @Override
                    public void call(final ResponseZerokitLogin responseLogin) {
                        if (rememberMe)
                            _getRememberMeKey(userId, password).enqueue(new Action<String>() {
                                @Override
                                public void call(String rememberKey) {
                                    storeSecret(KEY_STORE_ALIAS, rememberKey);
                                    subscriber.onSuccess(responseLogin);
                                }
                            }, new Action<ResponseZerokitError>() {
                                @Override
                                public void call(ResponseZerokitError responseZerokitError) {
                                    subscriber.onSuccess(responseLogin);
                                }
                            });
                        else
                            subscriber.onSuccess(responseLogin);
                        Arrays.fill(password, (byte) 0);
                    }
                }, new Action<ResponseZerokitError>() {
                    @Override
                    public void call(ResponseZerokitError responseZerokitError) {
                        Arrays.fill(password, (byte) 0);
                        subscriber.onError(responseZerokitError);
                    }
                });
            }
        });
    }

    /**
     * This method tries to log in the given user with the stored remember me key
     *
     * @param userId The userId of the user to log in.
     * @return Resolved userId of the logged in user.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<String, ResponseZerokitError> login(@NonNull final String userId) {
        return new CallAction<>(new ActionCallback<String, ResponseZerokitError>() {
            @Override
            public void call(final Callback<? super String, ? super ResponseZerokitError> subscriber) {
                String secret = getSecret(KEY_STORE_ALIAS);
                if (secret == null)
                    subscriber.onError(new ResponseZerokitError("No secret found"));
                else
                    _loginByRememberMeKey(userId, secret).enqueue(new Action<String>() {
                        @Override
                        public void call(String s) {
                            subscriber.onSuccess(s);
                        }
                    }, new Action<ResponseZerokitError>() {
                        @Override
                        public void call(ResponseZerokitError responseZerokitError) {
                            subscriber.onError(responseZerokitError);
                        }
                    });
            }
        });
    }

    /**
     * This method tries to log in the given user with the given password entered by the user
     * The logged in user will not be remembered by default
     *
     * @param userId        The userId of the user to log in.
     * @param passwordField field that stores password of the user to log in.
     * @return Resolved userId of the logged in user.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final PasswordEditText passwordField) {
        return login(userId, passwordField, false);
    }

    /**
     * This method tries to log in the given user with the given password entered by the user
     * The logged in user will not be remembered by default
     *
     * @param userId           The userId of the user to log in.
     * @param passwordExporter exporter that stores password of the user to log in.
     * @return Resolved userId of the logged in user.
     */
    @NonNull
    public Call<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final PasswordEditText.PasswordExporter passwordExporter) {
        return login(userId, passwordExporter, false);
    }

    /**
     * This method tries to log in the given user with the given password entered by the user
     * The logged in user will not be remembered by default
     *
     * @param userId   The userId of the user to log in.
     * @param password The password of the user to log in.
     * @return Resolved userId of the logged in user.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final byte[] password) {
        return login(userId, password, false);
    }

    /**
     * This method tries to log in the given user with the given password entered by the user
     *
     * @param userId        The userId of the user to log in.
     * @param passwordField field that stores the password of the user to log in.
     * @param rememberMe    If true, than next time the login without password will be possible
     * @return Resolved userId of the logged in user.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final PasswordEditText passwordField, final boolean rememberMe) {
        return login(userId, passwordField.getPasswordExporter(), rememberMe);
    }

    /**
     * This method tries to log in the given user with the given password entered by the user
     *
     * @param userId           The userId of the user to log in.
     * @param passwordExporter exporter that stores the password of the user to log in.
     * @param rememberMe       If true, than next time the login without password will be possible
     * @return Resolved userId of the logged in user.
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final PasswordEditText.PasswordExporter passwordExporter, final boolean rememberMe) {
        return login(userId, toBytes(passwordExporter.getCharArray(true)), rememberMe);
    }

    /**
     * This method logs into a security session and changes the password of the user.
     *
     * @param userId      Optional parameter to specify the id of the user changing password. This is only required if the user is not logged in.
     * @param oldPassword The currently used password
     * @param newPassword The new password
     * @return Resolves to the userId of the logged in user
     * <p>
     * InvalidAuthorization    - Invalid username or password
     * UserNameDoesntExist	   - The user does not exist
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitChangePassword, ResponseZerokitError> changePassword(@Nullable final String userId, @NonNull final byte[] oldPassword, @NonNull final byte[] newPassword) {
        return new CallAction<>(new ActionCallback<ResponseZerokitChangePassword, ResponseZerokitError>() {
            @Override
            public void call(final Callback<? super ResponseZerokitChangePassword, ? super ResponseZerokitError> subscriber) {
                final Action<ResponseZerokitError> onFail = new Action<ResponseZerokitError>() {
                    @Override
                    public void call(ResponseZerokitError responseZerokitError) {
                        Arrays.fill(newPassword, (byte) 0);
                        Arrays.fill(oldPassword, (byte) 0);
                        subscriber.onError(responseZerokitError);
                    }
                };
                Action<String> onSuccessWhoAmI = new Action<String>() {
                    @Override
                    public void call(final String userId_) {
                        _changePassword(userId_, oldPassword, newPassword).enqueue(new Action<ResponseZerokitChangePassword>() {
                            @Override
                            public void call(final ResponseZerokitChangePassword changePasswordResult) {
                                if (getSecret(KEY_STORE_ALIAS) != null) {
                                    _getRememberMeKey(userId_, newPassword).enqueue(new Action<String>() {
                                        @Override
                                        public void call(String rememberKey) {
                                            storeSecret(KEY_STORE_ALIAS, rememberKey);
                                            subscriber.onSuccess(changePasswordResult);
                                        }
                                    }, new Action<ResponseZerokitError>() {
                                        @Override
                                        public void call(ResponseZerokitError responseZerokitError) {
                                            subscriber.onSuccess(changePasswordResult);
                                        }
                                    });
                                } else subscriber.onSuccess(changePasswordResult);
                                Arrays.fill(newPassword, (byte) 0);
                                Arrays.fill(oldPassword, (byte) 0);
                            }
                        }, onFail);
                    }
                };
                if (TextUtils.isEmpty(userId))
                    _whoAmI().enqueue(onSuccessWhoAmI, onFail);
                else onSuccessWhoAmI.call(userId);
            }
        });
    }

    /**
     * This method logs into a security session and changes the password of the user.
     *
     * @param oldPassword The currently used password
     * @param newPassword The new password
     * @return Resolves to the userId of the logged in user
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitChangePassword, ResponseZerokitError> changePassword(@NonNull final byte[] oldPassword, @NonNull final byte[] newPassword) {
        return changePassword(null, oldPassword, newPassword);
    }

    /**
     * This method logs into a security session and changes the password of the user.
     *
     * @param userId              Optional parameter to specify the id of the user changing password. This is only required if the user is not logged in.
     * @param passwordEditTextOld The currently used password
     * @param passwordEditTextNew The new password
     * @return Resolves to the userId of the logged in user
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitChangePassword, ResponseZerokitError> changePassword(@NonNull final String userId, @NonNull final PasswordEditText passwordEditTextOld, @NonNull PasswordEditText passwordEditTextNew) {
        return changePassword(userId, passwordEditTextOld.getPasswordExporter(), passwordEditTextNew.getPasswordExporter());
    }

    /**
     * This method logs into a security session and changes the password of the user.
     *
     * @param passwordEditTextOld The currently used password
     * @param passwordEditTextNew The new password
     * @return Resolves to the userId of the logged in user
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitChangePassword, ResponseZerokitError> changePassword(@NonNull final PasswordEditText passwordEditTextOld, @NonNull PasswordEditText passwordEditTextNew) {
        return changePassword(passwordEditTextOld.getPasswordExporter(), passwordEditTextNew.getPasswordExporter());
    }

    /**
     * This method logs into a security session and changes the password of the user.
     *
     * @param userId              Optional parameter to specify the id of the user changing password. This is only required if the user is not logged in.
     * @param passwordExporterOld The currently used password
     * @param passwordExporterNew The new password
     * @return Resolves to the userId of the logged in user
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitChangePassword, ResponseZerokitError> changePassword(@NonNull final String userId, @NonNull final PasswordEditText.PasswordExporter passwordExporterOld, @NonNull PasswordEditText.PasswordExporter passwordExporterNew) {
        return changePassword(userId, toBytes(passwordExporterOld.getCharArray(true)), toBytes(passwordExporterNew.getCharArray(true)));
    }

    /**
     * This method logs into a security session and changes the password of the user.
     *
     * @param passwordExporterOld The currently used password
     * @param passwordExporterNew The new password
     * @return Resolves to the userId of the logged in user
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<ResponseZerokitChangePassword, ResponseZerokitError> changePassword(@NonNull final PasswordEditText.PasswordExporter passwordExporterOld, @NonNull PasswordEditText.PasswordExporter passwordExporterNew) {
        return changePassword(toBytes(passwordExporterOld.getCharArray(true)), toBytes(passwordExporterNew.getCharArray(true)));
    }

    /**
     * Log out the current user
     *
     * @param deleteRememberMe If true, after logout the remember me key will be deleted
     * @return the results
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<String, ResponseZerokitError> logout(final boolean deleteRememberMe) {
        return new CallAction<>(new ActionCallback<String, ResponseZerokitError>() {
            @Override
            public void call(final Callback<? super String, ? super ResponseZerokitError> subscriber) {
                if (deleteRememberMe)
                    _whoAmI().enqueue(new Action<String>() {
                        @Override
                        public void call(String userId) {
                            deleteSecret(KEY_STORE_ALIAS);
                            _logout().enqueue(new Action<String>() {
                                @Override
                                public void call(String s) {
                                    subscriber.onSuccess(s);
                                }
                            }, new Action<ResponseZerokitError>() {
                                @Override
                                public void call(ResponseZerokitError responseZerokitError) {
                                    subscriber.onError(responseZerokitError);
                                }
                            });
                        }
                    }, new Action<ResponseZerokitError>() {
                        @Override
                        public void call(ResponseZerokitError responseZerokitError) {
                            subscriber.onError(responseZerokitError);
                        }
                    });
                else
                    _logout().enqueue(new Action<String>() {
                        @Override
                        public void call(String s) {
                            subscriber.onSuccess(s);
                        }
                    }, new Action<ResponseZerokitError>() {
                        @Override
                        public void call(ResponseZerokitError responseZerokitError) {
                            subscriber.onError(responseZerokitError);
                        }
                    });
            }
        });
    }

    /**
     * Log out the current user
     *
     * @return Observable about the results
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<String, ResponseZerokitError> logout() {
        return logout(true);
    }

    /**
     * Use this methods to get the logged in user's identity.
     *
     * @return the user ID if logged in or `null` if not.
     */
    @NonNull
    public Call<String, ResponseZerokitError> whoAmI() {
        return new CallAction<>(new ActionCallback<String, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.whoAmI, new CallbackStringResult(subscriber));
            }
        });
    }

    // ====================================
    //  Tresors
    // ====================================

    /**
     * Tresors are the basic unit of key handling and sharing.
     * They can be referenced by a server generated id, returned on tresor creation.
     * We currently provide no means to list a user's tresors, so the application should save these ids.
     * Both tresor creation and sharing needs administrative approval to be effective.
     * Since the encrypted data has the tresor id included, it can be decrypted even if the tresorId is lost from the application database.
     *
     * @return Resolves to the tresorId of the newly created tresor. This id can be used to approve the tresor creation and to encrypt/decrypt using the tresor.
     */
    @NonNull
    public Call<String, ResponseZerokitError> createTresor() {
        return new CallAction<>(new ActionCallback<String, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.createTresor, new CallbackStringResult(subscriber));
            }
        });
    }

    /**
     * The shareTresor method will share the tresor with the given user.
     * The operation will only be effective after it is approved using the returned OperationId.
     * This uploads a modified tresor, but the new version is downloadable only after it has been approved.
     * This should be done as soon as possible, as approving any operation to a tresor may invalidate any pending ones.
     *
     * @param tresorId The id of the tresor to invite the user to.
     * @param userId   The id of the user to invite. Important to notice, that this is not an alias.
     * @return Resolves to the OperationId that can be used to approve this share.
     * <p>
     * BadInput	        - Invalid tresor or userId
     * TresorNotExists  - Couldn't find a tresor by the give tresorId
     * Forbidden	    - This user does not have access to the tresor
     * UserNotFound     - There is no user by that id
     */
    @NonNull
    public Call<String, ResponseZerokitError> shareTresor(@NonNull final String tresorId, @NonNull final String userId) {
        return new CallAction<>(new ActionCallback<String, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.shareTresor, new CallbackStringResult(subscriber), tresorId, userId);
            }
        });
    }

    /**
     * Removes the given user from the tresor. The operation will only be effective after it is approved using the returned OperationId.
     *
     * @param tresorId The id of the tresor, which from the user will be kicked out
     * @param userId   The id of the user, who will be kicked out
     * @return Resolves to the operation id. The operation must be approved before the user is kicked out.
     * <p>
     * InvalidAuthorization     - Invalid username or password
     * UserNameDoesntExist	    - The user does not exist
     */
    @SuppressWarnings("WeakerAccess")
    @NonNull
    public Call<String, ResponseZerokitError> kickFromTresor(@NonNull final String tresorId, @NonNull final String userId) {
        return new CallAction<>(new ActionCallback<String, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.kickFromTresor, new CallbackStringResult(subscriber), tresorId, userId);
            }
        });
    }


    // ====================================
    //  Encryption/Decryption
    // ====================================

    /**
     * Decrypts the given cipherText
     *
     * @param cipherText ZeroKit encrypted text
     * @return Resolves to the plain text.
     * <p>
     * BadInput 	- Invalid cipherText
     * Forbidden	- This user does not have access to the tresor
     */
    @NonNull
    public Call<String, ResponseZerokitError> decrypt(@NonNull final String cipherText) {
        return new CallAction<>(new ActionCallback<String, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.decrypt, new CallbackStringResult(subscriber), cipherText);
            }
        });
    }

    /**
     * Encrypts the plaintext by the given tresor.
     *
     * @param tresorId  The id of the tresor, that will be used to encrypt the text
     * @param plainText The plainText to encrypt
     * @return Resolves to the cipher text. It contains the tresorId, so the it can be decrypted by itself.
     * <p>
     * BadInput         - The tresorId and plainText has to be a non-empty string
     * BadInput         - Invalid tresorId
     * TresorNotExists  - Couldn't find a tresor by the given id
     * Forbidden	    - This user does not have access to the tresor
     */
    @NonNull
    public Call<String, ResponseZerokitError> encrypt(@NonNull final String tresorId, @NonNull final String plainText) {
        return new CallAction<>(new ActionCallback<String, ResponseZerokitError>() {
            @Override
            public void call(Callback<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.encrypt, new CallbackStringResult(subscriber), tresorId, plainText);
            }
        });
    }

}
