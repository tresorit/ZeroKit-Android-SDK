package com.tresorit.zerokitsdk.viewmodel;

import android.content.SharedPreferences;
import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.text.TextUtils;
import android.view.View;

import com.tresorit.zerokit.PasswordEditText;
import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokit.observer.Action1;
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

    private final Zerokit zerokit;

    @SuppressWarnings("WeakerAccess")
    final EventBus eventBus;

    private final SharedPreferences sharedPreferences;

    private final Action1<ResponseZerokitError> errorResponseHandlerSdk;

    @Inject
    public LoginViewModel(Zerokit zerokit, final EventBus eventBus, SharedPreferences sharedPreferences) {
        this.zerokit = zerokit;
        this.eventBus = eventBus;
        this.sharedPreferences = sharedPreferences;

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
        errorResponseHandlerSdk = new Action1<ResponseZerokitError>() {
            @Override
            public void call(ResponseZerokitError errorResponse) {
                inProgress.set(false);
                eventBus.post(new ShowMessageMessage(errorResponse.getDescription()));
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

    private void login(String username, PasswordEditText.PasswordExporter passwordExporter) {
        inProgress.set(true);
        String userId = sharedPreferences.getString(username, "");
        if (TextUtils.isEmpty(userId)) {
            inProgress.set(false);
            eventBus.post(new ShowMessageMessage("No user id found for this user alias"));
        } else {
            zerokit.login(userId, passwordExporter).subscribe(new Action1<ResponseZerokitLogin>() {
                @Override
                public void call(ResponseZerokitLogin responseLogin) {
                    inProgress.set(false);
                    eventBus.post(new LoginFinisedMessage());
                }
            }, errorResponseHandlerSdk);
        }
    }
}
