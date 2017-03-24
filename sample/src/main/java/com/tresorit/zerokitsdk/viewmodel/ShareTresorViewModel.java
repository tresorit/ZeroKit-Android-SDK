package com.tresorit.zerokitsdk.viewmodel;

import android.content.SharedPreferences;
import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.view.View;

import com.tresorit.adminapi.AdminApi;
import com.tresorit.adminapi.response.ResponseAdminApiError;
import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokit.call.Action;
import com.tresorit.zerokit.response.ResponseZerokitError;
import com.tresorit.zerokitsdk.message.CreateTresorFinishedMessage;

import org.greenrobot.eventbus.Subscribe;

import javax.inject.Inject;

public class ShareTresorViewModel extends BaseObservable {

    @SuppressWarnings("WeakerAccess")
    public final View.OnClickListener clickListener;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<Boolean> inProgress;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<String> userId;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<String> textSummary;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<String> sharedWithUserId;

    private final Zerokit zerokit;

    @SuppressWarnings("WeakerAccess")
    final AdminApi adminApi;
    private final SharedPreferences sharedPreferences;

    @SuppressWarnings("WeakerAccess")
    String tresorId;

    @Inject
    public ShareTresorViewModel(Zerokit zerokit, AdminApi adminApi, SharedPreferences sharedPreferences) {
        this.zerokit = zerokit;
        this.adminApi = adminApi;
        this.sharedPreferences = sharedPreferences;

        this.inProgress = new ObservableField<>(false);
        this.userId = new ObservableField<>();
        this.textSummary = new ObservableField<>();
        this.sharedWithUserId = new ObservableField<>("");
        this.clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shareTresor(tresorId, userId.get());
            }
        };
    }

    @SuppressWarnings("WeakerAccess")
    void shareTresor(String tresorId, final String userIdOrUserName) {
        inProgress.set(true);
        sharedWithUserId.set("");
        String userId = sharedPreferences.getString(userIdOrUserName, userIdOrUserName);
        this.zerokit.shareTresor(tresorId, userId).enqueue(new Action<String>() {
            @Override
            public void call(String shareId) {
                adminApi.approveShare(shareId).enqueue(new Action<String>() {
                    @Override
                    public void call(String result) {
                        sharedWithUserId.set("Shared with: " + userIdOrUserName);
                        inProgress.set(false);
                    }
                }, new Action<ResponseAdminApiError>() {
                    @Override
                    public void call(ResponseAdminApiError errorResponse) {
                        inProgress.set(false);
                    }
                });
            }
        }, new Action<ResponseZerokitError>() {
            @Override
            public void call(ResponseZerokitError responseError) {
                inProgress.set(false);
            }
        });
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onEvent(CreateTresorFinishedMessage message) {
        tresorId = message.getTresorId();
        textSummary.set("Tresor ID: " + message.getTresorId());
        sharedWithUserId.set("");
    }

}
