package com.tresorit.zerokitsdk.viewmodel;

import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.view.View;

import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokit.observer.Action1;
import com.tresorit.zerokit.response.ResponseZerokitError;

import javax.inject.Inject;

public class DecryptViewModel extends BaseObservable {

    @SuppressWarnings("WeakerAccess")
    public final View.OnClickListener clickListenerDecrypt;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<Boolean> inProgressDecrypt;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<String> textEncrypted;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<String> textDecrypted;

    private final Zerokit zerokit;

    @Inject
    public DecryptViewModel(Zerokit zerokit) {
        this.zerokit = zerokit;

        this.inProgressDecrypt = new ObservableField<>(false);
        this.textEncrypted = new ObservableField<>();
        this.textDecrypted = new ObservableField<>();
        this.clickListenerDecrypt = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                decrypt(textEncrypted.get());
            }
        };
    }


    @SuppressWarnings("WeakerAccess")
    void decrypt(String cipherText){
        inProgressDecrypt.set(true);
        zerokit.decrypt(cipherText).subscribe(new Action1<String>() {
            @Override
            public void call(String decryptedText) {
                inProgressDecrypt.set(false);
                textDecrypted.set(decryptedText);
            }
        }, new Action1<ResponseZerokitError>() {
            @Override
            public void call(ResponseZerokitError responseError) {
                inProgressDecrypt.set(false);
                textDecrypted.set("");
            }
        });
    }

}
