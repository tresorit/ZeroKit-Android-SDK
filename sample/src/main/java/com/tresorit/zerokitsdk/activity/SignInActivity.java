package com.tresorit.zerokitsdk.activity;

import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.ViewTreeObserver;

import com.tresorit.zerokitsdk.R;
import com.tresorit.zerokitsdk.ZerokitApplication;
import com.tresorit.zerokitsdk.cache.ComponentControllerActivity;
import com.tresorit.zerokitsdk.component.DaggerSignInComponent;
import com.tresorit.zerokitsdk.component.SignInComponent;
import com.tresorit.zerokitsdk.databinding.ActivitySigninBinding;
import com.tresorit.zerokitsdk.message.LoginFinisedMessage;
import com.tresorit.zerokitsdk.message.ShowMessageMessage;
import com.tresorit.zerokitsdk.viewmodel.SignInViewModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import javax.inject.Inject;

import static com.tresorit.zerokitsdk.util.Util.dpToPx;

public class SignInActivity extends ComponentControllerActivity<SignInComponent> {

    private static final int REQ_DEFAULT = 0;

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    SignInViewModel viewModel;

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    EventBus eventBus;

    @SuppressWarnings("WeakerAccess")
    ActivitySigninBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getComponent().inject(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_signin);
        binding.setViewmodel(viewModel);
        binding.container.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.bottomBar.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.bottomBar.setVisibility(binding.container.getRootView().getHeight() - binding.container.getHeight() > dpToPx(SignInActivity.this, 200) ? View.GONE : View.VISIBLE);
                    }
                });
            }
        });
    }

    @Override
    protected SignInComponent onCreateNonConfigurationComponent() {
        return DaggerSignInComponent.builder().applicationComponent(ZerokitApplication.get(this).component()).build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_DEFAULT:
                finish();
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        eventBus.register(this);
    }

    @Override
    protected void onStop() {
        eventBus.unregister(this);
        super.onStop();
    }


    @Subscribe
    @SuppressWarnings("unused")
    public void onEvent(ShowMessageMessage message) {
        showMessage(message.getMessage());
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onEvent(@SuppressWarnings("UnusedParameters") LoginFinisedMessage message) {
        startActivityForResult(new Intent(this, MainActivity.class), REQ_DEFAULT);
    }

    private void showMessage(String message) {
        Snackbar.make(binding.container, message, Snackbar.LENGTH_LONG).show();
    }

}
