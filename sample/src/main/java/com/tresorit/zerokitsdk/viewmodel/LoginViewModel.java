package com.tresorit.zerokitsdk.viewmodel;

import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.text.TextUtils;
import android.view.View;

import com.tresorit.zerokit.AdminApi;
import com.tresorit.zerokit.PasswordEditText;
import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokit.call.Action;
import com.tresorit.zerokit.response.IdentityTokens;
import com.tresorit.zerokit.response.ResponseAdminApiError;
import com.tresorit.zerokit.response.ResponseAdminApiLoginByCode;
import com.tresorit.zerokit.response.ResponseZerokitError;
import com.tresorit.zerokit.response.ResponseZerokitLogin;
import com.tresorit.zerokitsdk.message.LoginFinisedMessage;
import com.tresorit.zerokitsdk.message.ShowMessageMessage;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;

public class LoginViewModel extends BaseObservable {

    @SuppressWarnings("WeakerAccess")
    public final ObservableField<String> userName;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<String> passwordError;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<String> usernameError;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<Boolean> inProgress;

    @SuppressWarnings("WeakerAccess")
    public final View.OnClickListener clickListenerLogin;
    @SuppressWarnings("WeakerAccess")
    public final View.OnFocusChangeListener focusChangeListener;

    @SuppressWarnings("WeakerAccess")
    public final PasswordEditText.PasswordExporter passwordExporter;

    @SuppressWarnings("WeakerAccess")
    final Zerokit zerokit;
    @SuppressWarnings("WeakerAccess")
    final AdminApi adminApi;

    @SuppressWarnings("WeakerAccess")
    final EventBus eventBus;

    @SuppressWarnings("WeakerAccess")
    final Action<ResponseZerokitError> errorResponseHandlerSdk;
    @SuppressWarnings("WeakerAccess")
    final Action<ResponseAdminApiError> errorResponseHandlerAdminapi;

    @Inject
    public LoginViewModel(Zerokit zerokit, AdminApi adminApi, final EventBus eventBus) {
        this.zerokit = zerokit;
        this.adminApi = adminApi;
        this.eventBus = eventBus;

        this.passwordExporter = new PasswordEditText.PasswordExporter();

        this.clickListenerLogin = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        };

        userName = new ObservableField<>("");
        passwordError = new ObservableField<>("");
        usernameError = new ObservableField<>("");
        inProgress = new ObservableField<>(false);
        errorResponseHandlerSdk = new Action<ResponseZerokitError>() {
            @Override
            public void call(ResponseZerokitError errorResponse) {
                inProgress.set(false);
                eventBus.post(new ShowMessageMessage(errorResponse.toString()));
            }
        };
        errorResponseHandlerAdminapi = new Action<ResponseAdminApiError>() {
            @Override
            public void call(ResponseAdminApiError errorResponse) {
                inProgress.set(false);
                eventBus.post(new ShowMessageMessage(errorResponse.toString()));
            }
        };
        focusChangeListener = new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                passwordError.set("");
                usernameError.set("");
            }
        };
    }

    @SuppressWarnings("WeakerAccess")
    void attemptLogin() {
        if (TextUtils.isEmpty(userName.get())) usernameError.set("Required");
        else if (passwordExporter.isEmpty()) passwordError.set("Required");
        else login(userName.get(), passwordExporter);
    }

    private void login(String username, final PasswordEditText.PasswordExporter passwordExporter) {
        inProgress.set(true);
        adminApi.getUserId(username).enqueue(new Action<String>() {
            @Override
            public void call(String userId) {
                zerokit.login(userId, passwordExporter).enqueue(new Action<ResponseZerokitLogin>() {
                    @Override
                    public void call(ResponseZerokitLogin responseLogin) {
                        zerokit.getIdentityTokens(adminApi.getClientId()).enqueue(new Action<IdentityTokens>() {
                            @Override
                            public void call(IdentityTokens identityTokens) {
                                adminApi.login(identityTokens.getAuthorizationCode()).enqueue(new Action<ResponseAdminApiLoginByCode>() {
                                    @Override
                                    public void call(ResponseAdminApiLoginByCode responseAdminApiLoginByCode) {
                                        adminApi.setToken(responseAdminApiLoginByCode.getId());
                                        inProgress.set(false);
                                        eventBus.post(new LoginFinisedMessage());
                                    }
                                }, errorResponseHandlerAdminapi);
                            }
                        }, errorResponseHandlerSdk);
                    }
                }, errorResponseHandlerSdk);
            }
        }, errorResponseHandlerAdminapi);
    }
}
