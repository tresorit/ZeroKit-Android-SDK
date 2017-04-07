package com.tresorit.zerokitsdk.viewmodel;

import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.view.View;

import com.tresorit.zerokit.AdminApi;
import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokit.call.Action;
import com.tresorit.zerokit.response.ResponseAdminApiError;
import com.tresorit.zerokit.response.ResponseZerokitError;
import com.tresorit.zerokitsdk.message.CreateTresorFinishedMessage;
import com.tresorit.zerokitsdk.message.ShowMessageMessage;

import org.greenrobot.eventbus.EventBus;
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

    @SuppressWarnings("WeakerAccess")
    final Action<ResponseZerokitError> errorResponseHandlerSdk;
    @SuppressWarnings("WeakerAccess")
    final Action<ResponseAdminApiError> errorResponseHandlerAdminapi;

    final Zerokit zerokit;

    @SuppressWarnings("WeakerAccess")
    final AdminApi adminApi;

    @SuppressWarnings("WeakerAccess")
    String tresorId;

    @Inject
    public ShareTresorViewModel(Zerokit zerokit, AdminApi adminApi, final EventBus eventBus) {
        this.zerokit = zerokit;
        this.adminApi = adminApi;

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
    }

    @SuppressWarnings("WeakerAccess")
    void shareTresor(final String tresorId, final String userName) {
        inProgress.set(true);
        sharedWithUserId.set("");
        this.adminApi.getUserId(userName).enqueue(new Action<String>() {
            @Override
            public void call(String userId) {
                zerokit.shareTresor(tresorId, userId).enqueue(new Action<String>() {
                    @Override
                    public void call(String shareId) {
                        adminApi.sharedTresor(shareId).enqueue(new Action<Void>() {
                            @Override
                            public void call(Void result) {
                                sharedWithUserId.set("Shared with: " + userName);
                                inProgress.set(false);
                            }
                        }, errorResponseHandlerAdminapi);
                    }
                }, errorResponseHandlerSdk);
            }
        }, errorResponseHandlerAdminapi);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onEvent(CreateTresorFinishedMessage message) {
        tresorId = message.getTresorId();
        textSummary.set("Tresor ID: " + message.getTresorId());
        sharedWithUserId.set("");
    }

}
