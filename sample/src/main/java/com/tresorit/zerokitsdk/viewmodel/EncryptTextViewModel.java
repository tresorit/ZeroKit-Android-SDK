package com.tresorit.zerokitsdk.viewmodel;

import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.view.View;

import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokit.call.Action;
import com.tresorit.zerokit.response.ResponseZerokitError;
import com.tresorit.zerokitsdk.message.CopyEncryptedTextMessage;
import com.tresorit.zerokitsdk.message.CreateTresorFinishedMessage;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import javax.inject.Inject;

public class EncryptTextViewModel extends BaseObservable {

    @SuppressWarnings("WeakerAccess")
    public final View.OnClickListener clickListenerEncrypt;
    @SuppressWarnings("WeakerAccess")
    public final View.OnClickListener clickListenerDecrypt;
    @SuppressWarnings("WeakerAccess")
    public final View.OnClickListener clickListenerCopy;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<Boolean> inProgressEncrypt;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<Boolean> inProgressDecrypt;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<Boolean> encryptClicked;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<String> textOriginal;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<String> textEncrypted;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<String> textDecrypted;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<String> textSummary;

    private final Zerokit zerokit;

    @SuppressWarnings("WeakerAccess")
    String tresorId;

    @Inject
    public EncryptTextViewModel(Zerokit zerokit, final EventBus eventBus) {
        this.zerokit = zerokit;

        this.inProgressEncrypt = new ObservableField<>(false);
        this.inProgressDecrypt = new ObservableField<>(false);
        this.encryptClicked = new ObservableField<>(false);
        this.textOriginal = new ObservableField<>();
        this.textEncrypted = new ObservableField<>();
        this.textSummary = new ObservableField<>();
        this.textDecrypted = new ObservableField<>();
        this.clickListenerEncrypt = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                encryptClicked.set(true);
                encrypt(tresorId, textOriginal.get());
            }
        };
        this.clickListenerDecrypt = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decrypt(textEncrypted.get());
            }
        };
        this.clickListenerCopy = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                eventBus.post(new CopyEncryptedTextMessage(textEncrypted.get()));
            }
        };
    }

    @SuppressWarnings("WeakerAccess")
    void encrypt(String tresorId, String text) {
        inProgressEncrypt.set(true);
        zerokit.encrypt(tresorId, text).enqueue(new Action<String>() {
            @Override
            public void call(String encryptedText) {
                inProgressEncrypt.set(false);
                textEncrypted.set(encryptedText);
            }
        }, new Action<ResponseZerokitError>() {
            @Override
            public void call(ResponseZerokitError responseZerokitError) {
                inProgressEncrypt.set(false);
            }
        });
    }

    @SuppressWarnings("WeakerAccess")
    void decrypt(String cipherText) {
        inProgressDecrypt.set(true);
        zerokit.decrypt(cipherText).enqueue(new Action<String>() {
            @Override
            public void call(String decryptedText) {
                inProgressDecrypt.set(false);
                textDecrypted.set(decryptedText);
            }
        }, new Action<ResponseZerokitError>() {
            @Override
            public void call(ResponseZerokitError responseError) {
                inProgressDecrypt.set(false);
                textDecrypted.set("");
            }
        });
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onEvent(CreateTresorFinishedMessage message) {
        tresorId = message.getTresorId();
        textSummary.set("Tresor ID: " + message.getTresorId());
    }

}
