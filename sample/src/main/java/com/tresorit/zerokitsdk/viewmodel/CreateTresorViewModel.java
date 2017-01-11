package com.tresorit.zerokitsdk.viewmodel;

import android.databinding.BaseObservable;
import android.databinding.ObservableField;
import android.view.View;

import com.tresorit.adminapi.AdminApi;
import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokit.observer.Action1;
import com.tresorit.zerokitsdk.message.CreateTresorFinishedMessage;

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

    @Inject
    public CreateTresorViewModel(Zerokit zerokit, AdminApi adminApi, EventBus eventBus) {
        this.zerokit = zerokit;
        this.adminApi = adminApi;
        this.eventBus = eventBus;

        this.inProgress = new ObservableField<>(false);
        this.tresorId = new ObservableField<>("");
        this.clickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createTresor();
            }
        };
    }

    @SuppressWarnings("WeakerAccess")
    void createTresor() {
        inProgress.set(true);
        this.zerokit.createTresor().subscribe(new Action1<String>() {
                                                  @Override
                                                  public void call(final String tresorId) {
                                                      adminApi.approveTresorCreation(tresorId).subscribe(new Action1<String>() {
                                                          @Override
                                                          public void call(String initUserRegistrationResponse) {
                                                              CreateTresorViewModel.this.inProgress.set(false);
                                                              CreateTresorViewModel.this.tresorId.set("Tresor Id: " + tresorId);
                                                              CreateTresorViewModel.this.eventBus.post(new CreateTresorFinishedMessage(tresorId));
                                                          }
                                                      });
                                                  }
                                              }
        );
    }
}
