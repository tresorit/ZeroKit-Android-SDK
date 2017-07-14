package com.tresorit.zerokit;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.VisibleForTesting;
import android.support.test.espresso.IdlingResource;

import com.google.gson.Gson;
import com.tresorit.zerokit.call.Call;
import com.tresorit.zerokit.call.CallBase;
import com.tresorit.zerokit.call.Callback;
import com.tresorit.zerokit.call.Response;
import com.tresorit.zerokit.response.ResponseAdminApiError;
import com.tresorit.zerokit.response.ResponseAdminApiInitUserRegistration;
import com.tresorit.zerokit.response.ResponseAdminApiLoginByCode;
import com.tresorit.zerokit.util.Holder;
import com.zerokit.zerokit.BuildConfig;

import org.json.JSONException;

import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.http.Body;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.POST;
import retrofit2.http.Query;

public class AdminApi {

    private static final String AUTHORIZATION = "Authorization";
    private static final String AUTHORIZATION_HEADER = AUTHORIZATION + ": Bearer %s";

    @SuppressWarnings("WeakerAccess")
    final AdminApiService adminApiService;
    private String clientId;

    @SuppressWarnings("WeakerAccess")
    String token;

    @SuppressWarnings("WeakerAccess")
    final Gson gson;

    @SuppressWarnings("WeakerAccess")
    static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    @SuppressWarnings("WeakerAccess")
    final Executor executorBackground;

    @SuppressWarnings("WeakerAccess")
    @Nullable
    ZerokitCountingIdlingResource idlingResource;

    private static AdminApi instance;

    public static AdminApi init(@NonNull String host, String clientId) {
        instance = new AdminApi(host, clientId);
        return instance;
    }

    public static AdminApi getInstance() {
        return instance;
    }

    private AdminApi(String appbackendUrl, String clientId) {
        this.clientId = clientId;
        OkHttpClient.Builder builderHttpClient = new OkHttpClient.Builder();

        HttpLoggingInterceptor interceptorLogging = new HttpLoggingInterceptor();
        interceptorLogging.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BODY : HttpLoggingInterceptor.Level.NONE);
        Interceptor interceptorAuthorization = new Interceptor() {

            @Override
            public okhttp3.Response intercept(Chain chain) throws IOException {
                Request request = chain.request();
                String authHeader = request.headers().get(AUTHORIZATION);
                if (authHeader != null)
                    request = request.newBuilder().removeHeader(AUTHORIZATION).addHeader(AUTHORIZATION, String.format(authHeader, token)).build();
                return chain.proceed(request);
            }
        };

        builderHttpClient.addInterceptor(interceptorAuthorization);
        builderHttpClient.addInterceptor(interceptorLogging);

        Retrofit.Builder builder = new Retrofit.Builder().baseUrl(appbackendUrl).client(builderHttpClient.build()).addConverterFactory(GsonConverterFactory.create());
        Retrofit retrofit = builder.build();
        adminApiService = retrofit.create(AdminApiService.class);
        gson = new Gson();
        executorBackground = Executors.newSingleThreadExecutor();
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void clearClientId() {
        setClientId(null);
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void clearToken() {
        setToken(null);
    }

    public String getToken() {
        return token;
    }

    private String getData(String data) {
        return getData(data, null);
    }

    private String getData(String data, String tresorId) {
        return new JSONObject().put("data", data).put("tresorId", tresorId).toString();
    }

    public Call<String, ResponseAdminApiError> getUserId(final String userName) {
        return new CallRetrofit<>(adminApiService.getUserId(userName));
    }

    public Call<ResponseAdminApiInitUserRegistration, ResponseAdminApiError> initReg(final String userName, final String profileData) {
        return new CallRetrofit<>(adminApiService.initReg(userName, profileData));
    }

    public Call<Void, ResponseAdminApiError> finishReg(final String userId, final String validationVerifier) {
        return new CallRetrofit<>(adminApiService.finishReg(userId, validationVerifier));
    }

    public Call<String, ResponseAdminApiError> validateUser(final String userId, final String validationCode) {
        return new CallRetrofit<>(adminApiService.validateUser(userId, validationCode));
    }

    public Call<String, ResponseAdminApiError> setProfile(final String profile) {
        return new CallRetrofit<>(adminApiService.setProfile(RequestBody.create(JSON, getData(profile))));
    }

    public Call<String, ResponseAdminApiError> getProfile() {
        return new CallRetrofit<>(adminApiService.getProfile());
    }

    public Call<Void, ResponseAdminApiError> storePublicProfile(final String publicProfile) {
        return new CallRetrofit<>(adminApiService.storePublicProfile(RequestBody.create(JSON, getData(publicProfile))));
    }

    public Call<String, ResponseAdminApiError> getPublicProfile(String userId) {
        return new CallRetrofit<>(adminApiService.getPublicProfile(userId));
    }

    public Call<ResponseAdminApiLoginByCode, ResponseAdminApiError> login(final String code) {
        return new CallRetrofit<>(adminApiService.login(clientId, code));
    }

    public Call<Void, ResponseAdminApiError> createdTresor(final String tresorId) {
        return new CallRetrofit<>(adminApiService.createdTresor(tresorId));
    }

    public Call<Void, ResponseAdminApiError> sharedTresor(final String operationId) {
        return new CallRetrofit<>(adminApiService.sharedTresor(operationId));
    }

    public Call<Void, ResponseAdminApiError> kickedUser(final String operationId) {
        return new CallRetrofit<>(adminApiService.kickedUser(operationId));
    }

    public Call<Void, ResponseAdminApiError> invitedUser(final String operationId) {
        return new CallRetrofit<>(adminApiService.invitedUser(operationId));
    }

    @SuppressWarnings("WeakerAccess")
    public Call<Void, ResponseAdminApiError> storeData(final String tresorId, final String id, final String data) {
        return new CallRetrofit<>(adminApiService.storeData(id, RequestBody.create(JSON, getData(data, tresorId))));
    }

    @SuppressWarnings("WeakerAccess")
    public Call<String, ResponseAdminApiError> fetchData(final String id) {
        return new CallRetrofit<>(adminApiService.fetchData(id));
    }

    public Call<Void, ResponseAdminApiError> acceptedInvitationLink(final String operationId) {
        return new CallRetrofit<>(adminApiService.acceptedInvitationLink(operationId));
    }

    public Call<Void, ResponseAdminApiError> createdInvitationLink(final String operationId) {
        return new CallRetrofit<>(adminApiService.createdInvitationLink(operationId));
    }

    public Call<Void, ResponseAdminApiError> revokedInvitationLink(final String operationId) {
        return new CallRetrofit<>(adminApiService.revokedInvitationLink(operationId));
    }


    private class CallRetrofit<T> extends CallBase<T, ResponseAdminApiError> {

        final retrofit2.Call<T> call;

        CallRetrofit(retrofit2.Call<T> call) {
            this.call = call;
        }

        @Override
        public void enqueue(Callback<? super T, ? super ResponseAdminApiError> callback) {
            incrementIdlingResource();
            call.enqueue(new CallbackRetrofit<>(callback));
        }

        @Override
        public Response<T, ResponseAdminApiError> execute() {
            incrementIdlingResource();
            final Holder<Response<T, ResponseAdminApiError>> result = new Holder<>();
            final CountDownLatch signal = new CountDownLatch(1);
            executorBackground.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        result.t = getResponseFromResponse(call.execute());
                    } catch (EOFException e) {
                        result.t = Response.fromValue(null);
                    } catch (IOException e) {
                        result.t = Response.fromError(new ResponseAdminApiError(e.getMessage()));
                    }
                    signal.countDown();
                }
            });
            if (signal.getCount() > 0)
                try {
                    signal.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            return result.t;
        }
    }


    @SuppressWarnings("WeakerAccess")
    void incrementIdlingResource() {
        if (idlingResource != null) idlingResource.increment();
    }

    @SuppressWarnings("WeakerAccess")
    void decrementIdlingResource() {
        if (idlingResource != null) idlingResource.decrement();
    }


    private class CallbackRetrofit<T> implements retrofit2.Callback<T> {
        private final Callback<? super T, ? super ResponseAdminApiError> callback;

        CallbackRetrofit(Callback<? super T, ? super ResponseAdminApiError> callback) {
            this.callback = callback;
        }

        @Override
        public void onResponse(retrofit2.Call<T> call, retrofit2.Response<T> response) {
            decrementIdlingResource();
            Response<? extends T, ? extends ResponseAdminApiError> responseFromResponse = getResponseFromResponse(response);
            if (responseFromResponse.isError())
                callback.onError(responseFromResponse.getError());
            else
                callback.onSuccess(responseFromResponse.getResult());
        }

        @Override
        public void onFailure(retrofit2.Call<T> call, Throwable t) {
            decrementIdlingResource();
            if (t instanceof EOFException)
                callback.onSuccess(null);
            else
                callback.onError(new ResponseAdminApiError(t.getMessage()));
        }
    }


    @SuppressWarnings("WeakerAccess")
    <T> Response<T, ResponseAdminApiError> getResponseFromResponse(retrofit2.Response<T> response) {
        Response<T, ResponseAdminApiError> result;
        if (response.isSuccessful())
            result = Response.fromValue(response.body());
        else {
            try {
                ResponseBody responseBody = response.errorBody();
                if (responseBody.contentType() != null && "json".equals(responseBody.contentType().subtype()))
                    result = Response.fromError(gson.fromJson(new String(responseBody.bytes()), ResponseAdminApiError.class));
                else
                    result = Response.fromError(new ResponseAdminApiError(new String(responseBody.bytes())));
            } catch (IOException e) {
                result = Response.fromError(new ResponseAdminApiError(e.getMessage()));
            }
        }
        return result;
    }

    private interface AdminApiService {

        @GET("/api/user/get-user-id")
        retrofit2.Call<String> getUserId(@Query("userName") String userName);

        @FormUrlEncoded
        @POST("/api/user/init-user-registration")
        retrofit2.Call<ResponseAdminApiInitUserRegistration> initReg(@Field("userName") String userName, @Field("profileData") String profileData);

        @FormUrlEncoded
        @POST("/api/user/finish-user-registration")
        retrofit2.Call<Void> finishReg(@Field("userId") String userId, @Field("validationVerifier") String validationVerifier);

        @FormUrlEncoded
        @POST("/api/user/validate-user")
        retrofit2.Call<String> validateUser(@Field("userId") String userId, @Field("validationCode") String validationCode);

        @FormUrlEncoded
        @POST("/api/auth/login-by-code?token=true")
        retrofit2.Call<ResponseAdminApiLoginByCode> login(@Query("clientId") String clientId, @Field("code") String code);

        @FormUrlEncoded
        @Headers(AUTHORIZATION_HEADER)
        @POST("/api/tresor/created")
        retrofit2.Call<Void> createdTresor(@Field("tresorId") String tresorId);

        @FormUrlEncoded
        @Headers(AUTHORIZATION_HEADER)
        @POST("/api/tresor/invited-user")
        retrofit2.Call<Void> sharedTresor(@Field("operationId") String operationId);

        @FormUrlEncoded
        @Headers(AUTHORIZATION_HEADER)
        @POST("/api/tresor/kicked-user")
        retrofit2.Call<Void> kickedUser(@Field("operationId") String operationId);

        @FormUrlEncoded
        @Headers(AUTHORIZATION_HEADER)
        @POST("/api/user/invited-user")
        retrofit2.Call<Void> invitedUser(@Field("operationId") String operationId);

        @Headers(AUTHORIZATION_HEADER)
        @POST("/api/data/store")
        retrofit2.Call<Void> storeData(@Query("id") String id, @Body RequestBody data);

        @Headers(AUTHORIZATION_HEADER)
        @GET("/api/data/get")
        retrofit2.Call<String> fetchData(@Query("id") String id);

        @Headers(AUTHORIZATION_HEADER)
        @POST("/api/data/profile")
        retrofit2.Call<String> setProfile(@Body RequestBody data);

        @Headers(AUTHORIZATION_HEADER)
        @GET("/api/data/profile")
        retrofit2.Call<String> getProfile();

        @Headers(AUTHORIZATION_HEADER)
        @POST("/api/data/public-profile")
        retrofit2.Call<Void> storePublicProfile(@Body RequestBody data);

        @Headers(AUTHORIZATION_HEADER)
        @GET("/api/data/public-profile")
        retrofit2.Call<String> getPublicProfile(@Query("id") String userId);

        @FormUrlEncoded
        @Headers(AUTHORIZATION_HEADER)
        @POST("/api/invitationLinks/created")
        retrofit2.Call<Void> createdInvitationLink(@Field("operationId") String operationId);

        @FormUrlEncoded
        @Headers(AUTHORIZATION_HEADER)
        @POST("/api/invitationLinks/revoked")
        retrofit2.Call<Void> revokedInvitationLink(@Field("operationId") String operationId);

        @FormUrlEncoded
        @Headers(AUTHORIZATION_HEADER)
        @POST("/api/invitationLinks/accepted")
        retrofit2.Call<Void> acceptedInvitationLink(@Field("operationId") String operationId);
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

    private class JSONObject {

        private org.json.JSONObject jsonObject;

        public JSONObject() {
            jsonObject = new org.json.JSONObject();
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
}
