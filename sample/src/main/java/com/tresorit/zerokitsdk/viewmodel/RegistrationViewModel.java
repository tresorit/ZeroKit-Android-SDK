package com.tresorit.zerokitsdk.viewmodel;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.databinding.ObservableInt;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.text.TextUtils;
import android.view.View;

import com.tresorit.zerokit.AdminApi;
import com.tresorit.zerokit.PasswordEditText;
import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokit.call.Action;
import com.tresorit.zerokit.response.ResponseAdminApiError;
import com.tresorit.zerokit.response.ResponseAdminApiInitUserRegistration;
import com.tresorit.zerokit.response.ResponseZerokitError;
import com.tresorit.zerokit.response.ResponseZerokitPasswordStrength;
import com.tresorit.zerokit.response.ResponseZerokitRegister;
import com.tresorit.zerokit.util.JSONObject;
import com.tresorit.zerokitsdk.R;
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
    public final ObservableField<Drawable> seekbarColor;

    @SuppressWarnings("WeakerAccess")
    public final ObservableInt passwordStrength;

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
    final Action<ResponseAdminApiError> errorResponseHandlerAdmin;
    @SuppressWarnings("WeakerAccess")
    final Action<ResponseZerokitError> errorResponseHandlerSdk;

    @SuppressWarnings("WeakerAccess")
    public final View.OnClickListener clickListenerRegistration;
    @SuppressWarnings("WeakerAccess")
    public final View.OnFocusChangeListener focusChangeListener;

    final int colorRes[];

    @Inject
    public RegistrationViewModel(final Zerokit zerokit, AdminApi adminApi, final EventBus eventBus, SharedPreferences sharedPreferences, final Resources resources) {
        this.zerokit = zerokit;
        this.adminApi = adminApi;
        this.eventBus = eventBus;
        this.sharedPreferences = sharedPreferences;

        inProgress = new ObservableField<>(false);
        userName = new ObservableField<>("");
        passwordError = new ObservableField<>("");
        passwordConfirmError = new ObservableField<>("");
        userNameError = new ObservableField<>("");
        passwordStrength = new ObservableInt();
        seekbarColor = new ObservableField<>(resources.getDrawable(R.drawable.progress));
        colorRes = new int[]{R.color.red, R.color.deep_orange, R.color.orange, R.color.light_green, R.color.green};

        clickListenerRegistration = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptRegistration();
            }
        };
        errorResponseHandlerAdmin = new Action<ResponseAdminApiError>() {
            @Override
            public void call(ResponseAdminApiError responseAdminApiError) {
                inProgress.set(false);
                eventBus.post(new ShowMessageMessage(responseAdminApiError.getMessage()));
            }
        };
        errorResponseHandlerSdk = new Action<ResponseZerokitError>() {
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

        passwordExporter.setOnChangeListener(new PasswordEditText.OnChangeListener() {
            @Override
            public void onChanged() {
                zerokit.getPasswordStrength(passwordExporter).enqueue(new Action<ResponseZerokitPasswordStrength>() {
                    @Override
                    public void call(ResponseZerokitPasswordStrength responseZerokitPasswordStrength) {
                        int score = responseZerokitPasswordStrength.getScore();
                        passwordStrength.set((score + 1) * 20);
                        ((LayerDrawable) seekbarColor.get()).findDrawableByLayerId(android.R.id.progress).setColorFilter(resources.getColor(colorRes[score]), PorterDuff.Mode.SRC_IN);
                    }
                });
            }
        });
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
        adminApi.initReg(alias, new JSONObject()
                .put("autoValidate", true)
                .put("canCreateTresor", true)
                .put("alias", alias)
                .toString()).enqueue(new Action<ResponseAdminApiInitUserRegistration>() {
            @Override
            public void call(final ResponseAdminApiInitUserRegistration initUserRegistrationResponse) {
                zerokit.register(initUserRegistrationResponse.getUserId(), initUserRegistrationResponse.getRegSessionId(), passwordExporter).enqueue(new Action<ResponseZerokitRegister>() {
                    @Override
                    public void call(ResponseZerokitRegister responseZerokitRegister) {
                        adminApi.finishReg(initUserRegistrationResponse.getUserId(), responseZerokitRegister.getRegValidationVerifier()).enqueue(new Action<Void>() {
                            @Override
                            public void call(Void s) {
                                sharedPreferences.edit().putString(alias, initUserRegistrationResponse.getUserId()).apply();
                                inProgress.set(false);
                                RegistrationViewModel.this.userName.set("");
                                passwordExporter.clear();
                                passwordExporterConfirm.clear();
                                eventBus.post(new ShowMessageMessage("Successful sign up"));
                            }
                        }, errorResponseHandlerAdmin);
                    }
                }, errorResponseHandlerSdk);
            }
        }, errorResponseHandlerAdmin);
    }
}
