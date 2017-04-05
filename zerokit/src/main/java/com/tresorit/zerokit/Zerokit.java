package com.tresorit.zerokit;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.security.KeyPairGeneratorSpec;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.tresorit.zerokit.extension.Listener;
import com.tresorit.zerokit.extension.Listenered;
import com.tresorit.zerokit.extension.Result;
import com.tresorit.zerokit.extension.Synchronized;
import com.tresorit.zerokit.observer.Action1;
import com.tresorit.zerokit.observer.Observable;
import com.tresorit.zerokit.observer.Observer;
import com.tresorit.zerokit.observer.Subscriber;
import com.tresorit.zerokit.response.ResponseZerokitCreateInvitationLink;
import com.tresorit.zerokit.response.ResponseZerokitError;
import com.tresorit.zerokit.response.ResponseZerokitInvitationLinkInfo;
import com.tresorit.zerokit.response.ResponseZerokitLogin;
import com.tresorit.zerokit.response.ResponseZerokitRegister;
import com.tresorit.zerokit.util.ZerokitJson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
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
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
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

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;


public final class Zerokit {

    /**
     * Log tag
     */
    private static final String TAG = "Zerokit";

    /**
     * Key to get the api root metadata from manifest file
     */
    public static final String API_ROOT = "com.tresorit.zerokitsdk.API_ROOT";

    /**
     * Rivest Shamir Adleman (RSA) key.
     */
    private static final String KEY_ALGORITHM_RSA = "RSA";
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
     * The base url
     */
    private static final String BASE_URL = "/static/v4/api.html";

    /**
     * Singleton instance for Zerokit
     */
    @SuppressLint("StaticFieldLeak")
    private static Zerokit instance;

    /**
     * An instance of synchronized method access.
     * With the help of this instance the public methods of the API can be called
     * synchronized way instead of the original async way
     */
    private final Sync sync = new Sync();


    /**
     * The webview instance, which will be responsible for the javascript communication.
     * This webview requires a context, which need to be provided by the SDK user.
     * This view will be not attached to any other view.
     */
    @SuppressWarnings("WeakerAccess")
    WebView webView;
    /**
     * Own webview client which is responsible for handle the 'page-finished' events
     */
    @SuppressWarnings("WeakerAccess")
    final ZerokitWebViewClient clientWebView;
    /**
     * Collection of the registered observers, which will be triggered after a javascript function returns a result
     */
    @SuppressWarnings("WeakerAccess")
    final Map<String, Observer<? super String, ? super String>> observers;

    /**
     * The api root url
     */
    @SuppressWarnings("WeakerAccess")
    final String apiRoot;
    /**
     * A handler instance to run on the own thread
     */
    @SuppressWarnings("WeakerAccess")
    final Handler handler;

    /**
     * The javascript source of javascript
     */
    @SuppressWarnings("WeakerAccess")
    final String serializerJavaScriptSource;

    /**
     * Represents the states of the initialization process
     */
    @InitState
    @SuppressWarnings("WeakerAccess")
    int initState;

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

    /**
     * Represents the states of the initialization process
     */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({InitState.NOT_INITED, InitState.INITING, InitState.INIT_FINISHED})
    private @interface InitState {
        /**
         * The loadUrl with the init URL is not yet called
         */
        int NOT_INITED = 0;
        /**
         * The loadUrl with the init URL is already called, but it is not finished yet
         */
        int INITING = 1;
        /**
         * The loadUrl with the init URL is already called and also finished
         */
        int INIT_FINISHED = 2;
    }

    /**
     * Init the Zerokit singleton instance
     *
     * @param context a Context object used to pass it to the WebView and to get the metadata from manifest file
     */
    static void init(@NonNull Context context) {
        try {
            String apiRoot = context.getPackageManager().getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA).metaData.getString(API_ROOT);
            if (TextUtils.isEmpty(apiRoot))
                throw new IllegalStateException("No ApiRoot definition found in the AndroidManifest.xml");
            instance = new Zerokit(context, apiRoot);
        } catch (PackageManager.NameNotFoundException e) {
            throw new IllegalStateException("No ApiRoot definition found in the AndroidManifest.xml");
        }
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
     * @param apiRoot the url of the provided api root
     */
    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private Zerokit(@NonNull final Context context, @NonNull String apiRoot) {
        this.apiRoot = apiRoot;
        observers = new HashMap<>();
        clientWebView = new ZerokitWebViewClient();

        initState = InitState.NOT_INITED;
        jsInterfaceResponseHandler = new JSInterfaceResponseHandler();
        jsInterfaceByteArrayProvider = new JSInterfaceByteArrayProvider();

        clientWebView.addPageFinishListener(new PageFinishListener() {
            @Override
            public void onPageFinished(WebView view, String url) {
                if (initState == InitState.INITING){
                    log("JS Zerokit: init", "Init finished");
                    initState = InitState.INIT_FINISHED;
                    clientWebView.removePageFinishListener(this);
                }
            }

            @Override
            public void onReceivedError(int errorCode) {
                switch (errorCode){
                    case WebViewClient.ERROR_HOST_LOOKUP:
                        log("JS Zerokit: init", "Init failed: " + errorCode);
                        initState = InitState.NOT_INITED;
                        break;
                }
            }
        });

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT && BuildConfig.DEBUG && 0 != (context.getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE))
            WebView.setWebContentsDebuggingEnabled(true);

        serializerJavaScriptSource = getJavaScript(context);

        HandlerThread handlerThread = new HandlerThread("WebView thread");
        handlerThread.start();

        handler = new Handler(handlerThread.getLooper());
        runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                webView = new WebView(context);
                webView.setWillNotDraw(true);
                webView.getSettings().setJavaScriptEnabled(true);
                webView.getSettings().setDomStorageEnabled(true);
                webView.addJavascriptInterface(jsInterfaceResponseHandler, "JSInterfaceResponseHandler");
                webView.addJavascriptInterface(jsInterfaceByteArrayProvider, "JSInterfaceByteArrayProvider");
                webView.setWebViewClient(clientWebView);
                webView.setWebChromeClient(new ZerokitWebChromeClient());
                synchronized (handler) {
                    handler.notify();
                }
            }
        });

        synchronized (handler) {
            try {
                handler.wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the string which contains the javascript javascript source
     *
     * @param context from can the resource can be loaded
     * @return the string which contains the javascript javascript source
     */
    @NonNull
    private String getJavaScript(@NonNull Context context) {
        BufferedReader reader = null;
        StringBuilder stringBuffer = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(context.getResources().openRawResource(R.raw.javascript)));
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

    /**
     * The init method of the webview. This function is responsible for load init url
     *
     * @return an Observable object which will tell us when the init finished
     */
    @NonNull
    private Observable<Void, String> init() {
        return new Observable<>(new Observable.OnSubscribe<Void, String>() {
            @Override
            public void call(final Subscriber<? super Void, ? super String> subscriber) {
                if (initState != InitState.INIT_FINISHED) {
                    log("JS Zerokit: init", "Init not finished");
                    /*
                    * If the init url not loaded yet
                    */
                    clientWebView.addPageFinishListener(new PageFinishListener() {
                        @Override
                        public void onPageFinished(WebView view, String url) {
                            if (initState == InitState.INIT_FINISHED) {
                                subscriber.onSuccess(null);
                                clientWebView.removePageFinishListener(this);
                            }
                        }

                        @Override
                        public void onReceivedError(int errorCode) {
                            if (initState == InitState.NOT_INITED) {
                                subscriber.onFail("Initialization failed");
                                clientWebView.removePageFinishListener(this);
                            }
                        }
                    });

                    /*
                    * Ensure that the init url has been called only once
                    */
                    if (initState == InitState.NOT_INITED) {
                        log("JS Zerokit: init", "Init started");
                        initState = InitState.INITING;
                        loadUrl(apiRoot + BASE_URL);
                    }
                } else
                    /*
                    * If the init already has been finished
                    */
                    subscriber.onSuccess(null);
            }
        });
    }

    /**
     * The common "function caller" method, which transmits a request to the javascript section
     *
     * @param function   The concrete function which we would like to call
     * @param subscriber an Observer object, which will handle the result of the method
     * @param arguments  the arguments of the function which we would like to call
     */
    @SuppressWarnings({"WeakerAccess", "unchecked"})
    void callFunction(@NonNull final Function function, @NonNull final ObserverZerokit subscriber, final Object... arguments) {
        log("JS Zerokit: call", function.getFunctionName());

        init().subscribe(new Action1<Void>() {
            @Override
            public void call(Void result) {
                String id = UUID.randomUUID().toString();
                observers.put(id, subscriber);

                try {
                    JSONObject callData = new JSONObject();
                    callData.put("id", id);
                    callData.put("isMobile", function.isMobile);
                    callData.put("functionName", function.getFunctionName());
                    JSONArray args = new JSONArray();
                    for (Object arg : arguments) args.put(arg);
                    callData.put("args", args);


                    JSONArray extraArgs = new JSONArray();
                    for (int i = 0; i < function.extraArgs.length; i++) {
                        ExtraArg extraArg = function.extraArgs[i];
                        JSONObject jsonExtraArg = new JSONObject();
                        jsonExtraArg.put("position", (extraArg.position == -1) ? (args.length() + i) : extraArg.position);
                        jsonExtraArg.put("id", subscriber.ids[i]);
                        jsonExtraArg.put("type", extraArg.type);
                        extraArgs.put(jsonExtraArg);
                    }
                    callData.put("extraArgs", extraArgs);

                    callData.put("responseFormatter", function.responseFormatter);

                    loadUrl("javascript:\n" +
                            serializerJavaScriptSource +
                            String.format("\ncallFunction(%s);", JSONObject.quote(callData.toString())));

                } catch (JSONException e1) {
                    observers.remove(id);
                    subscriber.onFail(new ResponseZerokitError("JSONException").toJSON());
                }
            }
        }, new Action1<String>() {
            @Override
            public void call(String s) {
                subscriber.onFail(new ResponseZerokitError(s).toJSON());
            }
        });
    }

    /**
     * Loads an url in the WebView instance (on the Handler thread)
     *
     * @param url The url which will be loaded in the webview
     */
    @SuppressWarnings("WeakerAccess")
    void loadUrl(@NonNull final String url) {
        runOnHandlerThread(new Runnable() {
            @Override
            public void run() {
                webView.loadUrl(url);
            }
        });
    }

    /**
     * Runs a Runnable on the handler thread
     *
     * @param runnable The Runnable which will run on Handler thread
     */
    @SuppressWarnings("WeakerAccess")
    void runOnHandlerThread(@NonNull Runnable runnable) {
        handler.post(runnable);
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
        KeyPairGenerator generator = KeyPairGenerator.getInstance(KEY_ALGORITHM_RSA, ANDROID_KEYSTORE);
        generator.initialize(spec);
        generator.generateKeyPair();
    }

    /**
     * Send a log message if app is in debug mode
     *
     * @param tag Used to identify the source of a log message.  It usually identifies
     *            the class or activity where the log call occurs.
     * @param msg The message you would like logged.
     */
    @SuppressWarnings("WeakerAccess")
    void log(@NonNull String tag, @NonNull String msg) {
//        if (BuildConfig.DEBUG)
//        Log.d(tag, msg);
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
            runOnHandlerThread(new Runnable() {
                @Override
                public void run() {
                    Observer<? super String, ? super String> subscriber = observers.get(key);
                    if (subscriber != null) {
                        subscriber.onSuccess(result);
                        observers.remove(key);
                    }
                }
            });
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
            runOnHandlerThread(new Runnable() {
                @Override
                public void run() {
                    Observer<? super String, ? super String> subscriber = observers.get(key);
                    if (subscriber != null) {
                        subscriber.onFail(result);
                        observers.remove(key);
                    }
                }
            });
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
    private enum Function {
        login(true, null, new ExtraArg(ArgType.ByteArray)),
        loginByRememberMeKey(true, null),
        getRememberMeKey(true, null, new ExtraArg(ArgType.ByteArray)),
        register(true, null, new ExtraArg(ArgType.ByteArray)),
        createInvitationLink(true, null, new ExtraArg(ArgType.ByteArray)),
        acceptInvitationLink(true, null, new ExtraArg(ArgType.JSONToken, 0), new ExtraArg(ArgType.ByteArray)),
        changePassword(true, null, new ExtraArg(ArgType.ByteArray), new ExtraArg(ArgType.ByteArray)),

        acceptInvitationLinkNoPassword(new ExtraArg(ArgType.JSONToken, 0)),
        createInvitationLinkNoPassword,
        createTresor,
        decrypt,
        encrypt,
        getInvitationLinkInfo(false, ArgType.JSONToken),
        kickFromTresor,
        logout,
        shareTresor,
        whoAmI;

        /**
         * Modifies the url, which the function will be called on
         */
        private final boolean isMobile;
        private final ExtraArg[] extraArgs;
        private final ArgType responseFormatter;

        /**
         * Constructs a Function with the given parameters, which will represent a javascript function.
         * The isMobile is false by default
         */
        Function() {
            this(false, null);
        }

        Function(ExtraArg... extraArgs) {
            this(false, null, extraArgs);
        }


        /**
         * Constructs a Function with the given parameters, which will represent a javascript function
         *
         * @param isMobile modifies the url, which the function will be called on
         */
        Function(boolean isMobile, ArgType responseFormatter, ExtraArg... extraArgs) {
            this.isMobile = isMobile;
            this.extraArgs = extraArgs;
            this.responseFormatter = responseFormatter;
        }

        /**
         * Returns the generated name of the javascript function, which depends on the isMobile switch
         *
         * @return the generated name of the javascript function, which depends on the isMobile switch
         */
        @NonNull
        public String getFunctionName() {
            return name().replace("_", "");
        }
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
     * Interface for page finish listening
     */
    interface PageFinishListener {


        /**
         * Notify the host application that a page has finished loading. This method
         * is called only for main frame. When onPageFinished() is called, the
         * rendering picture may not be updated yet. To get the notification for the
         * new Picture, use {@link WebView.PictureListener#onNewPicture}.
         *
         * @param view The WebView that is initiating the callback.
         * @param url  The url of the page.
         */
        void onPageFinished(@SuppressWarnings("UnusedParameters") WebView view, @SuppressWarnings("UnusedParameters") String url);

        /**
         * Report an error to the host application. These errors are unrecoverable
         * (i.e. the main resource is unavailable). The errorCode parameter
         * corresponds to one of the ERROR_* constants.
         * @param errorCode The error code corresponding to an ERROR_* value.
         */
        void onReceivedError(int errorCode);
    }


    /**
     * WebViewClient subclass, which can handle more than one pageFinishListener registration
     */
    private class ZerokitWebViewClient extends WebViewClient {

        /**
         * The registered PageFinishListeners
         */
        private final List<PageFinishListener> pageFinishListeners;

        /**
         * Constructs a ZerokitWebViewClient
         */
        ZerokitWebViewClient() {
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
        public void onPageFinished(@NonNull WebView view, @NonNull String url) {
            super.onPageFinished(view, url);
            for (PageFinishListener pageFinishListener : new LinkedList<>(pageFinishListeners)) {
                pageFinishListener.onPageFinished(view, url);
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
                for (Observer<? super String, ? super String> observer : observers.values())
                    observer.onFail(new ResponseZerokitError(!isNetworkAvailable() ? "No network connection" : consoleMessage.message()).toJSON());
                observers.clear();
            }
            return super.onConsoleMessage(consoleMessage);
        }
    }

    private abstract class ObserverZerokit<T> implements Observer<String, String> {

        private String[] ids = null;

        /**
         * the subscriber which will handle the results
         */
        final Subscriber<? super T, ? super ResponseZerokitError> subscriber;

        private ObserverZerokit(@NonNull Subscriber<? super T, ? super ResponseZerokitError> subscriber) {
            this(subscriber, (String) null);
        }

        private ObserverZerokit(@NonNull Subscriber<? super T, ? super ResponseZerokitError> subscriber, String... ids) {
            this.ids = ids;
            this.subscriber = subscriber;
        }

        @Override
        public void onFail(@NonNull String e) {
            Zerokit.this.log("JS Zerokit: onError", e);
            if (ids != null)
                for (String id : ids) {
                    jsInterfaceByteArrayProvider.remove(id);
                }
            subscriber.onFail(new ResponseZerokitError().parse(e));
        }

        @Override
        public void onSuccess(@NonNull String result) {
            Zerokit.this.log("JS Zerokit: onSuccess", result);
            if (ids != null)
                for (String id : ids) {
                    jsInterfaceByteArrayProvider.remove(id);
                }
            subscriber.onSuccess(getResult(result));
        }

        abstract T getResult(String result);
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
        ObserverJSON(@NonNull Subscriber<? super T, ? super ResponseZerokitError> subscriber, @NonNull T response) {
            super(subscriber);
            this.response = response;
        }

        ObserverJSON(@NonNull Subscriber<? super T, ? super ResponseZerokitError> subscriber, @NonNull T response, String... ids) {
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
    private class ObserverString extends ObserverZerokit<String> {

        /**
         * Constructs an observer object which can handle JSON responses
         *
         * @param subscriber which will handle the responses
         */
        ObserverString(@NonNull Subscriber<? super String, ? super ResponseZerokitError> subscriber) {
            super(subscriber);
        }

        ObserverString(@NonNull Subscriber<? super String, ? super ResponseZerokitError> subscriber, String... ids) {
            super(subscriber, ids);
        }

        @Override
        @NonNull
        String getResult(@NonNull String result) {
            return result.replaceAll("\"", "");
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
    Observable<ResponseZerokitLogin, ResponseZerokitError> _login(@NonNull final String userId, @NonNull final byte[] password) {
        return new Observable<>(new Observable.OnSubscribe<ResponseZerokitLogin, ResponseZerokitError>() {

            @Override
            public void call(final Subscriber<? super ResponseZerokitLogin, ? super ResponseZerokitError> subscriber) {
                callFunction(Function.login, new ObserverJSON<>(subscriber, new ResponseZerokitLogin(), jsInterfaceByteArrayProvider.add(password)), userId);
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
    Observable<String, ResponseZerokitError> _loginByRememberMeKey(@NonNull final String userId, @NonNull final String key) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseZerokitError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.loginByRememberMeKey, new ObserverString(subscriber), userId, key);
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
    Observable<String, ResponseZerokitError> _getRememberMeKey(@NonNull final String userId, @NonNull final byte[] password) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseZerokitError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseZerokitError> subscriber) {
                callFunction(Function.getRememberMeKey, new ObserverString(subscriber, jsInterfaceByteArrayProvider.add(password)), userId);
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
    Observable<String, ResponseZerokitError> _logout() {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseZerokitError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.logout, new ObserverString(subscriber));
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
    Observable<String, ResponseZerokitError> _changePassword(@NonNull final String userId, @NonNull final byte[] oldPassword, @NonNull final byte[] newPassword) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseZerokitError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.changePassword, new ObserverString(subscriber, jsInterfaceByteArrayProvider.add(oldPassword), jsInterfaceByteArrayProvider.add(newPassword)), userId);
            }
        });
    }


    @NonNull
    public Sync sync() {
        return sync;
    }

    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

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
    public Observable<String, ResponseZerokitError> acceptInvitationLinkNoPassword(@NonNull final String token) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseZerokitError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.acceptInvitationLinkNoPassword, new ObserverString(subscriber, token));
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
    public Observable<ResponseZerokitInvitationLinkInfo, ResponseZerokitError> getInvitationLinkInfo(@NonNull final String secret) {
        return new Observable<>(new Observable.OnSubscribe<ResponseZerokitInvitationLinkInfo, ResponseZerokitError>() {
            @Override
            public void call(Subscriber<? super ResponseZerokitInvitationLinkInfo, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.getInvitationLinkInfo, new ObserverJSON<>(subscriber, new ResponseZerokitInvitationLinkInfo()), secret);
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
    public Observable<ResponseZerokitCreateInvitationLink, ResponseZerokitError> createInvitationLinkNoPassword(@NonNull final String linkBase, @NonNull final String tresorId, @Nullable final String message) {
        return new Observable<>(new Observable.OnSubscribe<ResponseZerokitCreateInvitationLink, ResponseZerokitError>() {
            @Override
            public void call(Subscriber<? super ResponseZerokitCreateInvitationLink, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.createInvitationLinkNoPassword, new ObserverJSON<>(subscriber, new ResponseZerokitCreateInvitationLink()), linkBase, tresorId, TextUtils.isEmpty(message) ? "" : message);
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
    public Observable<ResponseZerokitCreateInvitationLink, ResponseZerokitError> createInvitationLink(@NonNull final String linkBase, @NonNull final String tresorId, @Nullable final String message, @NonNull final byte[] password) {
        return new Observable<>(new Observable.OnSubscribe<ResponseZerokitCreateInvitationLink, ResponseZerokitError>() {
            @Override
            public void call(Subscriber<? super ResponseZerokitCreateInvitationLink, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.createInvitationLink, new ObserverJSON<>(subscriber, new ResponseZerokitCreateInvitationLink(), jsInterfaceByteArrayProvider.add(password)), linkBase, tresorId, TextUtils.isEmpty(message) ? "" : message);
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
    public Observable<ResponseZerokitCreateInvitationLink, ResponseZerokitError> createInvitationLink(@NonNull final String linkBase, @NonNull final String tresorId, @Nullable final String message, @NonNull PasswordEditText.PasswordExporter passwordExporter) {
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
    public Observable<ResponseZerokitCreateInvitationLink, ResponseZerokitError> createInvitationLink(@NonNull final String linkBase, @NonNull final String tresorId, @Nullable final String message, @NonNull PasswordEditText passwordEditText) {
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
    public Observable<String, ResponseZerokitError> acceptInvitationLink(@NonNull final String token, @NonNull final byte[] password) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseZerokitError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.acceptInvitationLink, new ObserverString(subscriber, token, jsInterfaceByteArrayProvider.add(password)));
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
    public Observable<String, ResponseZerokitError> acceptInvitationLink(@NonNull final String token, PasswordEditText.PasswordExporter passwordExporter) {
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
    public Observable<String, ResponseZerokitError> acceptInvitationLink(@NonNull final String token, PasswordEditText passwordEditText) {
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
    public Observable<ResponseZerokitRegister, ResponseZerokitError> register(@NonNull final String userId, @NonNull final String regSessionId, @NonNull final byte[] password) {
        return new Observable<>(new Observable.OnSubscribe<ResponseZerokitRegister, ResponseZerokitError>() {
            @Override
            public void call(Subscriber<? super ResponseZerokitRegister, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.register, new ObserverJSON<>(subscriber, new ResponseZerokitRegister(), jsInterfaceByteArrayProvider.add(password)), userId, regSessionId);
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
    public Observable<ResponseZerokitRegister, ResponseZerokitError> register(@NonNull final String userId, @NonNull final String regSessionId, @NonNull PasswordEditText passwordEditText) {
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
    public Observable<ResponseZerokitRegister, ResponseZerokitError> register(@NonNull final String userId, @NonNull final String regSessionId, @NonNull PasswordEditText.PasswordExporter passwordExporter) {
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
    public Observable<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final byte[] password, final boolean rememberMe) {
        return new Observable<>(new Observable.OnSubscribe<ResponseZerokitLogin, ResponseZerokitError>() {
            @Override
            public void call(final Subscriber<? super ResponseZerokitLogin, ? super ResponseZerokitError> subscriber) {
                _login(userId, password).subscribe(new Action1<ResponseZerokitLogin>() {
                    @Override
                    public void call(final ResponseZerokitLogin responseLogin) {
                        if (rememberMe)
                            _getRememberMeKey(userId, password).subscribe(new Action1<String>() {
                                @Override
                                public void call(String rememberKey) {
                                    storeSecret(KEY_STORE_ALIAS, rememberKey);
                                    subscriber.onSuccess(responseLogin);
                                }
                            }, new Action1<ResponseZerokitError>() {
                                @Override
                                public void call(ResponseZerokitError responseZerokitError) {
                                    subscriber.onSuccess(responseLogin);
                                }
                            });
                        else
                            subscriber.onSuccess(responseLogin);
                        Arrays.fill(password, (byte) 0);
                    }
                }, new Action1<ResponseZerokitError>() {
                    @Override
                    public void call(ResponseZerokitError responseZerokitError) {
                        Arrays.fill(password, (byte) 0);
                        subscriber.onFail(responseZerokitError);
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
    public Observable<String, ResponseZerokitError> login(@NonNull final String userId) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseZerokitError>() {
            @Override
            public void call(final Subscriber<? super String, ? super ResponseZerokitError> subscriber) {
                String secret = getSecret(KEY_STORE_ALIAS);
                if (secret == null)
                    subscriber.onFail(new ResponseZerokitError("No secret found"));
                else
                    _loginByRememberMeKey(userId, secret).subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            subscriber.onSuccess(s);
                        }
                    }, new Action1<ResponseZerokitError>() {
                        @Override
                        public void call(ResponseZerokitError responseZerokitError) {
                            subscriber.onFail(responseZerokitError);
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
    public Observable<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final PasswordEditText passwordField) {
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
    public Observable<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final PasswordEditText.PasswordExporter passwordExporter) {
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
    public Observable<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final byte[] password) {
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
    public Observable<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final PasswordEditText passwordField, final boolean rememberMe) {
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
    public Observable<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final PasswordEditText.PasswordExporter passwordExporter, final boolean rememberMe) {
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
    public Observable<String, ResponseZerokitError> changePassword(@Nullable final String userId, @NonNull final byte[] oldPassword, @NonNull final byte[] newPassword) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseZerokitError>() {
            @Override
            public void call(final Subscriber<? super String, ? super ResponseZerokitError> subscriber) {
                final Action1<ResponseZerokitError> onFail = new Action1<ResponseZerokitError>() {
                    @Override
                    public void call(ResponseZerokitError responseZerokitError) {
                        Arrays.fill(newPassword, (byte) 0);
                        Arrays.fill(oldPassword, (byte) 0);
                        subscriber.onFail(responseZerokitError);
                    }
                };
                Action1<String> onSuccessWhoAmI = new Action1<String>() {
                    @Override
                    public void call(final String userId_) {
                        _changePassword(userId_, oldPassword, newPassword).subscribe(new Action1<String>() {
                            @Override
                            public void call(final String changePasswordResult) {
                                if (getSecret(KEY_STORE_ALIAS) != null) {
                                    _getRememberMeKey(userId_, newPassword).subscribe(new Action1<String>() {
                                        @Override
                                        public void call(String rememberKey) {
                                            storeSecret(KEY_STORE_ALIAS, rememberKey);
                                            subscriber.onSuccess(changePasswordResult);
                                        }
                                    }, new Action1<ResponseZerokitError>() {
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
                    whoAmI().subscribe(onSuccessWhoAmI, onFail);
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
    public Observable<String, ResponseZerokitError> changePassword(@NonNull final byte[] oldPassword, @NonNull final byte[] newPassword) {
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
    public Observable<String, ResponseZerokitError> changePassword(@NonNull final String userId, @NonNull final PasswordEditText passwordEditTextOld, @NonNull PasswordEditText passwordEditTextNew) {
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
    public Observable<String, ResponseZerokitError> changePassword(@NonNull final PasswordEditText passwordEditTextOld, @NonNull PasswordEditText passwordEditTextNew) {
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
    public Observable<String, ResponseZerokitError> changePassword(@NonNull final String userId, @NonNull final PasswordEditText.PasswordExporter passwordExporterOld, @NonNull PasswordEditText.PasswordExporter passwordExporterNew) {
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
    public Observable<String, ResponseZerokitError> changePassword(@NonNull final PasswordEditText.PasswordExporter passwordExporterOld, @NonNull PasswordEditText.PasswordExporter passwordExporterNew) {
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
    public Observable<String, ResponseZerokitError> logout(final boolean deleteRememberMe) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseZerokitError>() {
            @Override
            public void call(final Subscriber<? super String, ? super ResponseZerokitError> subscriber) {
                if (deleteRememberMe)
                    whoAmI().subscribe(new Action1<String>() {
                        @Override
                        public void call(String userId) {
                            deleteSecret(KEY_STORE_ALIAS);
                            _logout().subscribe(new Action1<String>() {
                                @Override
                                public void call(String s) {
                                    subscriber.onSuccess(s);
                                }
                            }, new Action1<ResponseZerokitError>() {
                                @Override
                                public void call(ResponseZerokitError responseZerokitError) {
                                    subscriber.onFail(responseZerokitError);
                                }
                            });
                        }
                    }, new Action1<ResponseZerokitError>() {
                        @Override
                        public void call(ResponseZerokitError responseZerokitError) {
                            subscriber.onFail(responseZerokitError);
                        }
                    });
                else
                    _logout().subscribe(new Action1<String>() {
                        @Override
                        public void call(String s) {
                            subscriber.onSuccess(s);
                        }
                    }, new Action1<ResponseZerokitError>() {
                        @Override
                        public void call(ResponseZerokitError responseZerokitError) {
                            subscriber.onFail(responseZerokitError);
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
    public Observable<String, ResponseZerokitError> logout() {
        return logout(true);
    }

    /**
     * Use this methods to get the logged in user's identity.
     *
     * @return the user ID if logged in or `null` if not.
     */
    @NonNull
    public Observable<String, ResponseZerokitError> whoAmI() {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseZerokitError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.whoAmI, new ObserverString(subscriber));
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
    public Observable<String, ResponseZerokitError> createTresor() {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseZerokitError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.createTresor, new ObserverString(subscriber));
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
    public Observable<String, ResponseZerokitError> shareTresor(@NonNull final String tresorId, @NonNull final String userId) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseZerokitError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.shareTresor, new ObserverString(subscriber), tresorId, userId);
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
    public Observable<String, ResponseZerokitError> kickFromTresor(@NonNull final String tresorId, @NonNull final String userId) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseZerokitError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.kickFromTresor, new ObserverString(subscriber), tresorId, userId);
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
    public Observable<String, ResponseZerokitError> decrypt(@NonNull final String cipherText) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseZerokitError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.decrypt, new ObserverString(subscriber), cipherText);
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
    public Observable<String, ResponseZerokitError> encrypt(@NonNull final String tresorId, @NonNull final String plainText) {
        return new Observable<>(new Observable.OnSubscribe<String, ResponseZerokitError>() {
            @Override
            public void call(Subscriber<? super String, ? super ResponseZerokitError> subscriber) {
                Zerokit.this.callFunction(Function.encrypt, new ObserverString(subscriber), tresorId, plainText);
            }
        });
    }


    //::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::::

    /**
     * A link with no password can be accepted by any logged in user that has access to the token returned by getInvitationLinkInfo through the basic sdk.
     *
     * @param token    The token is the token field of the InvitationLinkPublicInfo of the link returned by getInvitationLinkInfo.
     * @param listener Resolves to the operation id that must be approved for the operation to be effective.
     */
    public void acceptInvitationLinkNoPassword(@NonNull final String token, Listener<String, ResponseZerokitError> listener) {
        Listenered.call(acceptInvitationLinkNoPassword(token), listener);
    }

    /**
     * You can get some information about the link by calling getInvitationLinkInfo with the link secret.
     * The returned object contains a token necessary to accept the invitation.
     * This also is a client side secret, that should never be uploaded to your site as that would compromise the zero knowledge nature of the system by providing ways to open the tresor.
     *
     * @param secret   The secret is the one that was concatenated to the end of the url in createInvitationLink.
     * @param listener Resolves to all the information available.
     */
    public void getInvitationLinkInfo(@NonNull final String secret, Listener<ResponseZerokitInvitationLinkInfo, ResponseZerokitError> listener) {
        Listenered.call(getInvitationLinkInfo(secret), listener);
    }

    /**
     * You can create an invitation link with no password.
     *
     * @param linkBase the base of the link. The link secret is concatenated after this after a '#'
     * @param tresorId the id of the tresor
     * @param message  optional arbitrary string data that can be retrieved without a password or any other information
     * @param listener Resolves to the operation id and the url of the created link. The operation must be approved before the link is enabled.
     */
    public void createInvitationLinkNoPassword(@NonNull final String linkBase, @NonNull final String tresorId, @Nullable final String message, Listener<ResponseZerokitCreateInvitationLink, ResponseZerokitError> listener) {
        Listenered.call(createInvitationLinkNoPassword(linkBase, tresorId, message), listener);
    }

    /**
     * This method creates an invitation link with the password entered
     *
     * @param linkBase the base of the link. The link secret is concatenated after this after a '#'
     * @param tresorId the id of the tresor
     * @param message  optional arbitrary string data that can be retrieved without a password or any other information
     * @param password the password to accept the link
     * @param listener Resolves to the operation id and the url of the created link. The operation must be approved before the link is enabled.
     */
    public void createInvitationLink(@NonNull final String linkBase, @NonNull final String tresorId, @Nullable final String message, @NonNull final byte[] password, Listener<ResponseZerokitCreateInvitationLink, ResponseZerokitError> listener) {
        Listenered.call(createInvitationLink(linkBase, tresorId, message, password), listener);
    }

    /**
     * This method creates an invitation link with the password entered
     *
     * @param linkBase         the base of the link. The link secret is concatenated after this after a '#'
     * @param tresorId         the id of the tresor
     * @param message          optional arbitrary string data that can be retrieved without a password or any other information
     * @param passwordExporter the passwordexporter that holds the password to accept the link
     * @param listener         Resolves to the operation id and the url of the created link. The operation must be approved before the link is enabled.
     */
    public void createInvitationLink(@NonNull final String linkBase, @NonNull final String tresorId, @Nullable final String message, @NonNull PasswordEditText.PasswordExporter passwordExporter, Listener<ResponseZerokitCreateInvitationLink, ResponseZerokitError> listener) {
        Listenered.call(createInvitationLink(linkBase, tresorId, message, passwordExporter), listener);
    }

    /**
     * This method creates an invitation link with the password entered
     *
     * @param linkBase         the base of the link. The link secret is concatenated after this after a '#'
     * @param tresorId         the id of the tresor
     * @param message          optional arbitrary string data that can be retrieved without a password or any other information
     * @param passwordEditText the passwordEditText that holds the password to accept the link
     * @param listener         Resolves to the operation id and the url of the created link. The operation must be approved before the link is enabled.
     */
    public void createInvitationLink(@NonNull final String linkBase, @NonNull final String tresorId, @Nullable final String message, @NonNull PasswordEditText passwordEditText, Listener<ResponseZerokitCreateInvitationLink, ResponseZerokitError> listener) {
        Listenered.call(createInvitationLink(linkBase, tresorId, message, passwordEditText), listener);
    }

    /**
     * This method will add the user to the tresor of the link using the password entered.
     *
     * @param token    The token is the $token field of the InvitationLinkPublicInfo of the link returned by getInvitationLinkInfo.
     * @param password The password for the link
     * @param listener Resolves to the operation id that must be approved for the operation to be effective.
     */
    public void acceptInvitationLink(@NonNull final String token, @NonNull final byte[] password, Listener<String, ResponseZerokitError> listener) {
        Listenered.call(acceptInvitationLink(token, password), listener);
    }

    /**
     * This method will add the user to the tresor of the link using the password entered.
     *
     * @param token            The token is the $token field of the InvitationLinkPublicInfo of the link returned by getInvitationLinkInfo.
     * @param passwordExporter The passwordexporter that holds the password for the link
     * @param listener         Resolves to the operation id that must be approved for the operation to be effective.
     */
    public void acceptInvitationLink(@NonNull final String token, PasswordEditText.PasswordExporter passwordExporter, Listener<String, ResponseZerokitError> listener) {
        Listenered.call(acceptInvitationLink(token, passwordExporter), listener);
    }

    /**
     * This method will add the user to the tresor of the link using the password entered.
     *
     * @param token            The token is the $token field of the InvitationLinkPublicInfo of the link returned by getInvitationLinkInfo.
     * @param passwordEditText The passwordEditText that holds the password for the link
     * @param listener         Resolves to the operation id that must be approved for the operation to be effective.
     */
    public void acceptInvitationLink(@NonNull final String token, PasswordEditText passwordEditText, Listener<String, ResponseZerokitError> listener) {
        Listenered.call(acceptInvitationLink(token, passwordEditText), listener);
    }

    /**
     * Registers the user in ZKit with the provided userId.
     * The returned value is the regValidationVerifier which is used during user validation, so it should be saved on the app server.
     *
     * @param userId       The userId provided by the InitUserRegistration API call for the given alias
     * @param regSessionId The regSessionId provided by the InitUserRegistration API call for the given alias
     * @param password     The password provided by the User
     * @param listener     the RegValidationVerifier property.
     */
    public void register(@NonNull final String userId, @NonNull final String regSessionId, @NonNull final byte[] password, Listener<ResponseZerokitRegister, ResponseZerokitError> listener) {
        Listenered.call(register(userId, regSessionId, password), listener);
    }

    /**
     * Registers the user in ZKit with the provided userId
     * The returned value is the regValidationVerifier which is used during user validation, so it should be saved on the app server.
     *
     * @param userId           The userId provided by the InitUserRegistration API call for the given alias
     * @param regSessionId     The regSessionId provided by the InitUserRegistration API call for the given alias
     * @param passwordEditText field that holds the password provided by the user
     * @param listener         the RegValidationVerifier property.
     */
    public void register(@NonNull final String userId, @NonNull final String regSessionId, @NonNull PasswordEditText passwordEditText, Listener<ResponseZerokitRegister, ResponseZerokitError> listener) {
        Listenered.call(register(userId, regSessionId, passwordEditText), listener);
    }

    /**
     * Registers the user in ZKit with the provided userId
     * The returned value is the regValidationVerifier which is used during user validation, so it should be saved on the app server.
     *
     * @param userId           The userId provided by the InitUserRegistration API call for the given alias
     * @param regSessionId     The regSessionId provided by the InitUserRegistration API call for the given alias
     * @param passwordExporter exporter that holds the password provided by the User
     * @param listener         the RegValidationVerifier property.
     */
    public void register(@NonNull final String userId, @NonNull final String regSessionId, @NonNull PasswordEditText.PasswordExporter passwordExporter, Listener<ResponseZerokitRegister, ResponseZerokitError> listener) {
        Listenered.call(register(userId, regSessionId, passwordExporter), listener);
    }

    /**
     * This method tries to log in the given user with the given password entered by the user
     *
     * @param userId     The userId of the user to log in.
     * @param password   The password of the user to log in.
     * @param rememberMe If true, than next time the login without password will be possible
     * @param listener   Resolved userId of the logged in user.
     */
    public void login(@NonNull final String userId, @NonNull final byte[] password, final boolean rememberMe, Listener<ResponseZerokitLogin, ResponseZerokitError> listener) {
        Listenered.call(login(userId, password, rememberMe), listener);
    }

    /**
     * This method tries to log in the given user with the stored remember me key
     *
     * @param userId   The userId of the user to log in.
     * @param listener Resolved userId of the logged in user.
     */
    public void login(@NonNull final String userId, Listener<String, ResponseZerokitError> listener) {
        Listenered.call(login(userId), listener);
    }

    /**
     * This method tries to log in the given user with the given password entered by the user
     * The logged in user will not be remembered by default
     *
     * @param userId        The userId of the user to log in.
     * @param passwordField field that stores password of the user to log in.
     * @param listener      Resolved userId of the logged in user.
     */
    public void login(@NonNull final String userId, @NonNull final PasswordEditText passwordField, Listener<ResponseZerokitLogin, ResponseZerokitError> listener) {
        Listenered.call(login(userId, passwordField), listener);
    }

    /**
     * This method tries to log in the given user with the given password entered by the user
     * The logged in user will not be remembered by default
     *
     * @param userId           The userId of the user to log in.
     * @param passwordExporter exporter that stores password of the user to log in.
     * @param listener         Resolved userId of the logged in user.
     */
    public void login(@NonNull final String userId, @NonNull final PasswordEditText.PasswordExporter passwordExporter, Listener<ResponseZerokitLogin, ResponseZerokitError> listener) {
        Listenered.call(login(userId, passwordExporter), listener);
    }

    /**
     * This method tries to log in the given user with the given password entered by the user
     * The logged in user will not be remembered by default
     *
     * @param userId   The userId of the user to log in.
     * @param password The password of the user to log in.
     * @param listener Resolved userId of the logged in user.
     */
    public void login(@NonNull final String userId, @NonNull final byte[] password, Listener<ResponseZerokitLogin, ResponseZerokitError> listener) {
        Listenered.call(login(userId, password), listener);
    }

    /**
     * This method tries to log in the given user with the given password entered by the user
     *
     * @param userId        The userId of the user to log in.
     * @param passwordField field that stores the password of the user to log in.
     * @param rememberMe    If true, than next time the login without password will be possible
     * @param listener      Resolved userId of the logged in user.
     */
    public void login(@NonNull final String userId, @NonNull final PasswordEditText passwordField, final boolean rememberMe, Listener<ResponseZerokitLogin, ResponseZerokitError> listener) {
        Listenered.call(login(userId, passwordField, rememberMe), listener);
    }

    /**
     * This method tries to log in the given user with the given password entered by the user
     *
     * @param userId           The userId of the user to log in.
     * @param passwordExporter exporter that stores the password of the user to log in.
     * @param rememberMe       If true, than next time the login without password will be possible
     * @param listener         Resolved userId of the logged in user.
     */
    public void login(@NonNull final String userId, @NonNull final PasswordEditText.PasswordExporter passwordExporter, final boolean rememberMe, Listener<ResponseZerokitLogin, ResponseZerokitError> listener) {
        Listenered.call(login(userId, passwordExporter, rememberMe), listener);
    }

    /**
     * This method logs into a security session and changes the password of the user.
     *
     * @param userId      Optional parameter to specify the id of the user changing password. This is only required if the user is not logged in.
     * @param oldPassword The currently used password
     * @param newPassword The new password
     * @param listener    Resolves to the userId of the logged in user
     *                    <p>
     *                    InvalidAuthorization    - Invalid username or password
     *                    UserNameDoesntExist	   - The user does not exist
     */
    public void changePassword(@Nullable final String userId, @NonNull final byte[] oldPassword, @NonNull final byte[] newPassword, Listener<String, ResponseZerokitError> listener) {
        Listenered.call(changePassword(userId, oldPassword, newPassword), listener);
    }

    /**
     * This method logs into a security session and changes the password of the user.
     *
     * @param oldPassword The currently used password
     * @param newPassword The new password
     * @param listener    Resolves to the userId of the logged in user
     */
    public void changePassword(@NonNull final byte[] oldPassword, @NonNull final byte[] newPassword, Listener<String, ResponseZerokitError> listener) {
        Listenered.call(changePassword(oldPassword, newPassword), listener);
    }

    /**
     * This method logs into a security session and changes the password of the user.
     *
     * @param userId              Optional parameter to specify the id of the user changing password. This is only required if the user is not logged in.
     * @param passwordEditTextOld The currently used password
     * @param passwordEditTextNew The new password
     * @param listener            Resolves to the userId of the logged in user
     */
    public void changePassword(@NonNull final String userId, @NonNull final PasswordEditText passwordEditTextOld, @NonNull PasswordEditText passwordEditTextNew, Listener<String, ResponseZerokitError> listener) {
        Listenered.call(changePassword(userId, passwordEditTextOld, passwordEditTextNew), listener);
    }

    /**
     * This method logs into a security session and changes the password of the user.
     *
     * @param passwordEditTextOld The currently used password
     * @param passwordEditTextNew The new password
     * @param listener            Resolves to the userId of the logged in user
     */
    public void changePassword(@NonNull final PasswordEditText passwordEditTextOld, @NonNull PasswordEditText passwordEditTextNew, Listener<String, ResponseZerokitError> listener) {
        Listenered.call(changePassword(passwordEditTextOld, passwordEditTextNew), listener);
    }

    /**
     * This method logs into a security session and changes the password of the user.
     *
     * @param userId              Optional parameter to specify the id of the user changing password. This is only required if the user is not logged in.
     * @param passwordExporterOld The currently used password
     * @param passwordExporterNew The new password
     * @param listener            Resolves to the userId of the logged in user
     */
    public void changePassword(@NonNull final String userId, @NonNull final PasswordEditText.PasswordExporter passwordExporterOld, @NonNull PasswordEditText.PasswordExporter passwordExporterNew, Listener<String, ResponseZerokitError> listener) {
        Listenered.call(changePassword(userId, passwordExporterOld, passwordExporterNew), listener);
    }

    /**
     * This method logs into a security session and changes the password of the user.
     *
     * @param passwordExporterOld The currently used password
     * @param passwordExporterNew The new password
     * @param listener            Resolves to the userId of the logged in user
     */
    public void changePassword(@NonNull final PasswordEditText.PasswordExporter passwordExporterOld, @NonNull PasswordEditText.PasswordExporter passwordExporterNew, Listener<String, ResponseZerokitError> listener) {
        Listenered.call(changePassword(passwordExporterOld, passwordExporterNew), listener);
    }

    /**
     * Log out the current user
     *
     * @param deleteRememberMe If true, after logout the remember me key will be deleted
     * @param listener         the results
     */
    public void logout(final boolean deleteRememberMe, Listener<String, ResponseZerokitError> listener) {
        Listenered.call(logout(deleteRememberMe), listener);
    }

    /**
     * Log out the current user
     *
     * @param listener Observable about the results
     */
    public void logout(Listener<String, ResponseZerokitError> listener) {
        Listenered.call(logout(), listener);
    }

    /**
     * Use this methods to get the logged in user's identity.
     *
     * @param listener the user ID if logged in or `null` if not.
     */
    public void whoAmI(Listener<String, ResponseZerokitError> listener) {
        Listenered.call(whoAmI(), listener);
    }

    /**
     * Tresors are the basic unit of key handling and sharing.
     * They can be referenced by a server generated id, returned on tresor creation.
     * We currently provide no means to list a user's tresors, so the application should save these ids.
     * Both tresor creation and sharing needs administrative approval to be effective.
     * Since the encrypted data has the tresor id included, it can be decrypted even if the tresorId is lost from the application database.
     *
     * @param listener Resolves to the tresorId of the newly created tresor. This id can be used to approve the tresor creation and to encrypt/decrypt using the tresor.
     */
    public void createTresor(Listener<String, ResponseZerokitError> listener) {
        Listenered.call(createTresor(), listener);
    }

    /**
     * The shareTresor method will share the tresor with the given user.
     * The operation will only be effective after it is approved using the returned OperationId.
     * This uploads a modified tresor, but the new version is downloadable only after it has been approved.
     * This should be done as soon as possible, as approving any operation to a tresor may invalidate any pending ones.
     *
     * @param tresorId The id of the tresor to invite the user to.
     * @param userId   The id of the user to invite. Important to notice, that this is not an alias.
     * @param listener Resolves to the OperationId that can be used to approve this share.
     *                 <p>
     *                 BadInput	        - Invalid tresor or userId
     *                 TresorNotExists  - Couldn't find a tresor by the give tresorId
     *                 Forbidden	    - This user does not have access to the tresor
     *                 UserNotFound     - There is no user by that id
     */
    public void shareTresor(@NonNull final String tresorId, @NonNull final String userId, Listener<String, ResponseZerokitError> listener) {
        Listenered.call(shareTresor(tresorId, userId), listener);
    }

    /**
     * Removes the given user from the tresor. The operation will only be effective after it is approved using the returned OperationId.
     *
     * @param tresorId The id of the tresor, which from the user will be kicked out
     * @param userId   The id of the user, who will be kicked out
     * @param listener Resolves to the operation id. The operation must be approved before the user is kicked out.
     *                 <p>
     *                 InvalidAuthorization     - Invalid username or password
     *                 UserNameDoesntExist	    - The user does not exist
     */
    public void kickFromTresor(@NonNull final String tresorId, @NonNull final String userId, Listener<String, ResponseZerokitError> listener) {
        Listenered.call(kickFromTresor(tresorId, userId), listener);
    }

    /**
     * Decrypts the given cipherText
     *
     * @param cipherText ZeroKit encrypted text
     * @param listener   Resolves to the plain text.
     *                   <p>
     *                   BadInput 	- Invalid cipherText
     *                   Forbidden	- This user does not have access to the tresor
     */
    public void decrypt(@NonNull final String cipherText, Listener<String, ResponseZerokitError> listener) {
        Listenered.call(decrypt(cipherText), listener);
    }

    /**
     * Encrypts the plaintext by the given tresor.
     *
     * @param tresorId  The id of the tresor, that will be used to encrypt the text
     * @param plainText The plainText to encrypt
     * @param listener  Resolves to the cipher text. It contains the tresorId, so the it can be decrypted by itself.
     *                  <p>
     *                  BadInput         - The tresorId and plainText has to be a non-empty string
     *                  BadInput         - Invalid tresorId
     *                  TresorNotExists  - Couldn't find a tresor by the given id
     *                  Forbidden	    - This user does not have access to the tresor
     */
    public void encrypt(@NonNull final String tresorId, @NonNull final String plainText, Listener<String, ResponseZerokitError> listener) {
        Listenered.call(encrypt(tresorId, plainText), listener);
    }


    @SuppressWarnings("WeakerAccess")
    public class Sync {
        /**
         * A link with no password can be accepted by any logged in user that has access to the token returned by getInvitationLinkInfo through the basic sdk.
         *
         * @param token The token is the token field of the InvitationLinkPublicInfo of the link returned by getInvitationLinkInfo.
         * @return Resolves to the operation id that must be approved for the operation to be effective.
         */
        public Result<String, ResponseZerokitError> acceptInvitationLinkNoPassword(@NonNull final String token) {
            return Synchronized.call(Zerokit.this.acceptInvitationLinkNoPassword(token));
        }

        /**
         * You can get some information about the link by calling getInvitationLinkInfo with the link secret.
         * The returned object contains a token necessary to accept the invitation.
         * This also is a client side secret, that should never be uploaded to your site as that would compromise the zero knowledge nature of the system by providing ways to open the tresor.
         *
         * @param secret The secret is the one that was concatenated to the end of the url in createInvitationLink.
         * @return Resolves to all the information available.
         */
        public Result<ResponseZerokitInvitationLinkInfo, ResponseZerokitError> getInvitationLinkInfo(@NonNull final String secret) {
            return Synchronized.call(Zerokit.this.getInvitationLinkInfo(secret));
        }

        /**
         * You can create an invitation link with no password.
         *
         * @param linkBase the base of the link. The link secret is concatenated after this after a '#'
         * @param tresorId the id of the tresor
         * @param message  optional arbitrary string data that can be retrieved without a password or any other information
         * @return Resolves to the operation id and the url of the created link. The operation must be approved before the link is enabled.
         */
        public Result<ResponseZerokitCreateInvitationLink, ResponseZerokitError> createInvitationLinkNoPassword(@NonNull final String linkBase, @NonNull final String tresorId, @Nullable final String message) {
            return Synchronized.call(Zerokit.this.createInvitationLinkNoPassword(linkBase, tresorId, message));
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
        public Result<ResponseZerokitCreateInvitationLink, ResponseZerokitError> createInvitationLink(@NonNull final String linkBase, @NonNull final String tresorId, @Nullable final String message, @NonNull final byte[] password) {
            return Synchronized.call(Zerokit.this.createInvitationLink(linkBase, tresorId, message, password));
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
        public Result<ResponseZerokitCreateInvitationLink, ResponseZerokitError> createInvitationLink(@NonNull final String linkBase, @NonNull final String tresorId, @Nullable final String message, @NonNull PasswordEditText.PasswordExporter passwordExporter) {
            return Synchronized.call(Zerokit.this.createInvitationLink(linkBase, tresorId, message, passwordExporter));
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
        public Result<ResponseZerokitCreateInvitationLink, ResponseZerokitError> createInvitationLink(@NonNull final String linkBase, @NonNull final String tresorId, @Nullable final String message, @NonNull PasswordEditText passwordEditText) {
            return Synchronized.call(Zerokit.this.createInvitationLink(linkBase, tresorId, message, passwordEditText));
        }

        /**
         * This method will add the user to the tresor of the link using the password entered.
         *
         * @param token    The token is the $token field of the InvitationLinkPublicInfo of the link returned by getInvitationLinkInfo.
         * @param password The password for the link
         * @return Resolves to the operation id that must be approved for the operation to be effective.
         */
        public Result<String, ResponseZerokitError> acceptInvitationLink(@NonNull final String token, @NonNull final byte[] password) {
            return Synchronized.call(Zerokit.this.acceptInvitationLink(token, password));
        }

        /**
         * This method will add the user to the tresor of the link using the password entered.
         *
         * @param token            The token is the $token field of the InvitationLinkPublicInfo of the link returned by getInvitationLinkInfo.
         * @param passwordExporter The passwordexporter that holds the password for the link
         * @return Resolves to the operation id that must be approved for the operation to be effective.
         */
        public Result<String, ResponseZerokitError> acceptInvitationLink(@NonNull final String token, PasswordEditText.PasswordExporter passwordExporter) {
            return Synchronized.call(Zerokit.this.acceptInvitationLink(token, passwordExporter));
        }

        /**
         * This method will add the user to the tresor of the link using the password entered.
         *
         * @param token            The token is the $token field of the InvitationLinkPublicInfo of the link returned by getInvitationLinkInfo.
         * @param passwordEditText The passwordEditText that holds the password for the link
         * @return Resolves to the operation id that must be approved for the operation to be effective.
         */
        public Result<String, ResponseZerokitError> acceptInvitationLink(@NonNull final String token, PasswordEditText passwordEditText) {
            return Synchronized.call(Zerokit.this.acceptInvitationLink(token, passwordEditText));
        }

        /**
         * Registers the user in ZKit with the provided userId.
         * The returned value is the regValidationVerifier which is used during user validation, so it should be saved on the app server.
         *
         * @param userId       The userId provided by the InitUserRegistration API call for the given alias
         * @param regSessionId The regSessionId provided by the InitUserRegistration API call for the given alias
         * @param password     The password provided by the User
         * @return the RegValidationVerifier property.
         */
        public Result<ResponseZerokitRegister, ResponseZerokitError> register(@NonNull final String userId, @NonNull final String regSessionId, @NonNull final byte[] password) {
            return Synchronized.call(Zerokit.this.register(userId, regSessionId, password));
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
        public Result<ResponseZerokitRegister, ResponseZerokitError> register(@NonNull final String userId, @NonNull final String regSessionId, @NonNull PasswordEditText passwordEditText) {
            return Synchronized.call(Zerokit.this.register(userId, regSessionId, passwordEditText));
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
        public Result<ResponseZerokitRegister, ResponseZerokitError> register(@NonNull final String userId, @NonNull final String regSessionId, @NonNull PasswordEditText.PasswordExporter passwordExporter) {
            return Synchronized.call(Zerokit.this.register(userId, regSessionId, passwordExporter));
        }

        /**
         * This method tries to log in the given user with the given password entered by the user
         *
         * @param userId     The userId of the user to log in.
         * @param password   The password of the user to log in.
         * @param rememberMe If true, than next time the login without password will be possible
         * @return Resolved userId of the logged in user.
         */
        public Result<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final byte[] password, final boolean rememberMe) {
            return Synchronized.call(Zerokit.this.login(userId, password, rememberMe));
        }

        /**
         * This method tries to log in the given user with the stored remember me key
         *
         * @param userId The userId of the user to log in.
         * @return Resolved userId of the logged in user.
         */
        public Result<String, ResponseZerokitError> login(@NonNull final String userId) {
            return Synchronized.call(Zerokit.this.login(userId));
        }

        /**
         * This method tries to log in the given user with the given password entered by the user
         * The logged in user will not be remembered by default
         *
         * @param userId        The userId of the user to log in.
         * @param passwordField field that stores password of the user to log in.
         * @return Resolved userId of the logged in user.
         */
        public Result<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final PasswordEditText passwordField) {
            return Synchronized.call(Zerokit.this.login(userId, passwordField));
        }

        /**
         * This method tries to log in the given user with the given password entered by the user
         * The logged in user will not be remembered by default
         *
         * @param userId           The userId of the user to log in.
         * @param passwordExporter exporter that stores password of the user to log in.
         * @return Resolved userId of the logged in user.
         */
        public Result<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final PasswordEditText.PasswordExporter passwordExporter) {
            return Synchronized.call(Zerokit.this.login(userId, passwordExporter));
        }

        /**
         * This method tries to log in the given user with the given password entered by the user
         * The logged in user will not be remembered by default
         *
         * @param userId   The userId of the user to log in.
         * @param password The password of the user to log in.
         * @return Resolved userId of the logged in user.
         */
        public Result<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final byte[] password) {
            return Synchronized.call(Zerokit.this.login(userId, password));
        }

        /**
         * This method tries to log in the given user with the given password entered by the user
         *
         * @param userId        The userId of the user to log in.
         * @param passwordField field that stores the password of the user to log in.
         * @param rememberMe    If true, than next time the login without password will be possible
         * @return Resolved userId of the logged in user.
         */
        public Result<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final PasswordEditText passwordField, final boolean rememberMe) {
            return Synchronized.call(Zerokit.this.login(userId, passwordField, rememberMe));
        }

        /**
         * This method tries to log in the given user with the given password entered by the user
         *
         * @param userId           The userId of the user to log in.
         * @param passwordExporter exporter that stores the password of the user to log in.
         * @param rememberMe       If true, than next time the login without password will be possible
         * @return Resolved userId of the logged in user.
         */
        public Result<ResponseZerokitLogin, ResponseZerokitError> login(@NonNull final String userId, @NonNull final PasswordEditText.PasswordExporter passwordExporter, final boolean rememberMe) {
            return Synchronized.call(Zerokit.this.login(userId, passwordExporter, rememberMe));
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
        public Result<String, ResponseZerokitError> changePassword(@Nullable final String userId, @NonNull final byte[] oldPassword, @NonNull final byte[] newPassword) {
            return Synchronized.call(Zerokit.this.changePassword(userId, oldPassword, newPassword));
        }

        /**
         * This method logs into a security session and changes the password of the user.
         *
         * @param oldPassword The currently used password
         * @param newPassword The new password
         * @return Resolves to the userId of the logged in user
         */
        public Result<String, ResponseZerokitError> changePassword(@NonNull final byte[] oldPassword, @NonNull final byte[] newPassword) {
            return Synchronized.call(Zerokit.this.changePassword(oldPassword, newPassword));
        }

        /**
         * This method logs into a security session and changes the password of the user.
         *
         * @param userId              Optional parameter to specify the id of the user changing password. This is only required if the user is not logged in.
         * @param passwordEditTextOld The currently used password
         * @param passwordEditTextNew The new password
         * @return Resolves to the userId of the logged in user
         */
        public Result<String, ResponseZerokitError> changePassword(@NonNull final String userId, @NonNull final PasswordEditText passwordEditTextOld, @NonNull PasswordEditText passwordEditTextNew) {
            return Synchronized.call(Zerokit.this.changePassword(userId, passwordEditTextOld, passwordEditTextNew));
        }

        /**
         * This method logs into a security session and changes the password of the user.
         *
         * @param passwordEditTextOld The currently used password
         * @param passwordEditTextNew The new password
         * @return Resolves to the userId of the logged in user
         */
        public Result<String, ResponseZerokitError> changePassword(@NonNull final PasswordEditText passwordEditTextOld, @NonNull PasswordEditText passwordEditTextNew) {
            return Synchronized.call(Zerokit.this.changePassword(passwordEditTextOld, passwordEditTextNew));
        }

        /**
         * This method logs into a security session and changes the password of the user.
         *
         * @param userId              Optional parameter to specify the id of the user changing password. This is only required if the user is not logged in.
         * @param passwordExporterOld The currently used password
         * @param passwordExporterNew The new password
         * @return Resolves to the userId of the logged in user
         */
        public Result<String, ResponseZerokitError> changePassword(@NonNull final String userId, @NonNull final PasswordEditText.PasswordExporter passwordExporterOld, @NonNull PasswordEditText.PasswordExporter passwordExporterNew) {
            return Synchronized.call(Zerokit.this.changePassword(userId, passwordExporterOld, passwordExporterNew));
        }

        /**
         * This method logs into a security session and changes the password of the user.
         *
         * @param passwordExporterOld The currently used password
         * @param passwordExporterNew The new password
         * @return Resolves to the userId of the logged in user
         */
        public Result<String, ResponseZerokitError> changePassword(@NonNull final PasswordEditText.PasswordExporter passwordExporterOld, @NonNull PasswordEditText.PasswordExporter passwordExporterNew) {
            return Synchronized.call(Zerokit.this.changePassword(passwordExporterOld, passwordExporterNew));
        }

        /**
         * Log out the current user
         *
         * @param deleteRememberMe If true, after logout the remember me key will be deleted
         * @return the results
         */
        public Result<String, ResponseZerokitError> logout(final boolean deleteRememberMe) {
            return Synchronized.call(Zerokit.this.logout(deleteRememberMe));
        }

        /**
         * Log out the current user
         *
         * @return Observable about the results
         */
        public Result<String, ResponseZerokitError> logout() {
            return Synchronized.call(Zerokit.this.logout());
        }

        /**
         * Use this methods to get the logged in user's identity.
         *
         * @return the user ID if logged in or `null` if not.
         */
        public Result<String, ResponseZerokitError> whoAmI() {
            return Synchronized.call(Zerokit.this.whoAmI());
        }

        /**
         * Tresors are the basic unit of key handling and sharing.
         * They can be referenced by a server generated id, returned on tresor creation.
         * We currently provide no means to list a user's tresors, so the application should save these ids.
         * Both tresor creation and sharing needs administrative approval to be effective.
         * Since the encrypted data has the tresor id included, it can be decrypted even if the tresorId is lost from the application database.
         *
         * @return Resolves to the tresorId of the newly created tresor. This id can be used to approve the tresor creation and to encrypt/decrypt using the tresor.
         */
        public Result<String, ResponseZerokitError> createTresor() {
            return Synchronized.call(Zerokit.this.createTresor());
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
        public Result<String, ResponseZerokitError> shareTresor(@NonNull final String tresorId, @NonNull final String userId) {
            return Synchronized.call(Zerokit.this.shareTresor(tresorId, userId));
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
        public Result<String, ResponseZerokitError> kickFromTresor(@NonNull final String tresorId, @NonNull final String userId) {
            return Synchronized.call(Zerokit.this.kickFromTresor(tresorId, userId));
        }

        /**
         * Decrypts the given cipherText
         *
         * @param cipherText ZeroKit encrypted text
         * @return Resolves to the plain text.
         * <p>
         * BadInput 	- Invalid cipherText
         * Forbidden	- This user does not have access to the tresor
         */
        public Result<String, ResponseZerokitError> decrypt(@NonNull final String cipherText) {
            return Synchronized.call(Zerokit.this.decrypt(cipherText));
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
        public Result<String, ResponseZerokitError> encrypt(@NonNull final String tresorId, @NonNull final String plainText) {
            return Synchronized.call(Zerokit.this.encrypt(tresorId, plainText));
        }

    }
}
