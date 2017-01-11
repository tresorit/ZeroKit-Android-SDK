package com.tresorit.zerokitsdk.viewmodel;

import android.content.SharedPreferences;
import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.text.TextUtils;
import android.view.View;

import com.tresorit.adminapi.AdminApi;
import com.tresorit.adminapi.response.ResponseAdminApiError;
import com.tresorit.adminapi.response.ResponseAdminApiInitUserRegistration;
import com.tresorit.zerokit.PasswordEditText;
import com.tresorit.zerokit.observer.Action1;
import com.tresorit.zerokit.response.ResponseZerokitError;
import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokit.response.ResponseZerokitRegister;
import com.tresorit.zerokitsdk.message.ShowMessageMessage;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;

public class RegistrationViewModel extends BaseObservable {

    @SuppressWarnings("WeakerAccess")
    public final ObservableField<String> userName;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<Boolean> inProgress;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<String> passwordError;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<String> passwordConfirmError;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<String> userNameError;

    @SuppressWarnings("WeakerAccess")
    public final PasswordEditText.PasswordExporter passwordExporter;
    @SuppressWarnings("WeakerAccess")
    public final PasswordEditText.PasswordExporter passwordExporterConfirm;

    @SuppressWarnings("WeakerAccess")
    final Zerokit zerokit;
    @SuppressWarnings("WeakerAccess")
    final AdminApi adminApi;
    @SuppressWarnings("WeakerAccess")
    final EventBus eventBus;
    @SuppressWarnings("WeakerAccess")
    final SharedPreferences sharedPreferences;

    @SuppressWarnings("WeakerAccess")
    final Action1<ResponseAdminApiError> errorResponseHandlerAdmin;
    @SuppressWarnings("WeakerAccess")
    final Action1<ResponseZerokitError> errorResponseHandlerSdk;

    @SuppressWarnings("WeakerAccess")
    public final View.OnClickListener clickListenerRegistration;
    @SuppressWarnings("WeakerAccess")
    public final View.OnFocusChangeListener focusChangeListener;


    @Inject
    public RegistrationViewModel(Zerokit zerokit, AdminApi adminApi, final EventBus eventBus, SharedPreferences sharedPreferences) {
        this.zerokit = zerokit;
        this.adminApi = adminApi;
        this.eventBus = eventBus;
        this.sharedPreferences = sharedPreferences;

        inProgress = new ObservableField<>(false);
        userName = new ObservableField<>("");
        passwordError = new ObservableField<>("");
        passwordConfirmError = new ObservableField<>("");
        userNameError = new ObservableField<>("");

        clickListenerRegistration = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegistration();
            }
        };
        errorResponseHandlerAdmin = new Action1<ResponseAdminApiError>() {
            @Override
            public void call(ResponseAdminApiError responseAdminApiError) {
                inProgress.set(false);
                eventBus.post(new ShowMessageMessage(responseAdminApiError.getErrorMessage()));
            }
        };
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
                userNameError.set("");
                passwordConfirmError.set("");
            }
        };

        passwordExporter = new PasswordEditText.PasswordExporter();
        passwordExporterConfirm = new PasswordEditText.PasswordExporter();
    }

    @SuppressWarnings("WeakerAccess")
    void attemptRegistration() {
        if (TextUtils.isEmpty(userName.get())) userNameError.set("Required");
        else if (passwordExporter.isEmpty()) passwordError.set("Required");
        else if (passwordExporterConfirm.isEmpty()) passwordConfirmError.set("Required");
        else if (!passwordExporter.isContentEqual(passwordExporterConfirm))
            passwordConfirmError.set("Does not match");
        else registration(userName.get(), passwordExporter);
    }

    private void registration(final String alias, final PasswordEditText.PasswordExporter passwordExporter) {
        inProgress.set(true);
        Action1<ResponseAdminApiInitUserRegistration> responseAdminApiInitUserRegistrationAction = new Action1<ResponseAdminApiInitUserRegistration>() {
            @Override
            public void call(final ResponseAdminApiInitUserRegistration initUserRegistrationResponse) {
                Action1<ResponseZerokitRegister> responseZerokitRegisterAction = new Action1<ResponseZerokitRegister>() {
                    @Override
                    public void call(ResponseZerokitRegister responseZerokitRegister) {
                        Action1<String> response = new Action1<String>() {
                            @Override
                            public void call(String s) {
                                sharedPreferences.edit().putString(alias, initUserRegistrationResponse.getUserId()).apply();
                                inProgress.set(false);
                                RegistrationViewModel.this.userName.set("");
                                passwordExporter.clear();
                                passwordExporterConfirm.clear();
                                eventBus.post(new ShowMessageMessage("Successful sign up"));
                            }
                        };
                        adminApi.validateUser(initUserRegistrationResponse.getUserId(), initUserRegistrationResponse.getRegSessionId(), initUserRegistrationResponse.getRegSessionVerifier(), responseZerokitRegister.getRegValidationVerifier(), alias).subscribe(response, errorResponseHandlerAdmin);
                    }
                };
                zerokit.register(initUserRegistrationResponse.getUserId(), initUserRegistrationResponse.getRegSessionId(), passwordExporter).subscribe(responseZerokitRegisterAction, errorResponseHandlerSdk);
            }
        };
        adminApi.initUserRegistration().subscribe(responseAdminApiInitUserRegistrationAction,
                errorResponseHandlerAdmin);
    }
}
