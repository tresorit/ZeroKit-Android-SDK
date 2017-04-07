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

import javax.inject.Inject;

public class CreateTresorViewModel extends BaseObservable {

    @SuppressWarnings("WeakerAccess")
    public final View.OnClickListener clickListener;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<Boolean> inProgress;
    @SuppressWarnings("WeakerAccess")
    public final ObservableField<String> tresorId;

    private final Zerokit zerokit;

    @SuppressWarnings("WeakerAccess")
    final AdminApi adminApi;
    @SuppressWarnings("WeakerAccess")
    final EventBus eventBus;

    @SuppressWarnings("WeakerAccess")
    final Action<ResponseZerokitError> errorResponseHandlerSdk;
    @SuppressWarnings("WeakerAccess")
    final Action<ResponseAdminApiError> errorResponseHandlerAdminapi;


    @Inject
    public CreateTresorViewModel(final Zerokit zerokit, final AdminApi adminApi, final EventBus eventBus) {
        this.zerokit = zerokit;
        this.adminApi = adminApi;
        this.eventBus = eventBus;


        this.inProgress = new ObservableField<>(false);
        this.tresorId = new ObservableField<>("");
        this.clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createTresor();
                inProgress.set(true);
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
    void createTresor() {
        inProgress.set(true);
        this.zerokit.createTresor().enqueue(new Action<String>() {
            @Override
            public void call(final String tresorId) {
                adminApi.createdTresor(tresorId).enqueue(new Action<Void>() {
                    @Override
                    public void call(Void res) {
                        CreateTresorViewModel.this.inProgress.set(false);
                        CreateTresorViewModel.this.tresorId.set("Tresor Id: " + tresorId);
                        CreateTresorViewModel.this.eventBus.post(new CreateTresorFinishedMessage(tresorId));
                    }
                }, errorResponseHandlerAdminapi);
            }
        }, errorResponseHandlerSdk);
    }
}
