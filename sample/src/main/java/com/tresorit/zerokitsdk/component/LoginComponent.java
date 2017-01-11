package com.tresorit.zerokitsdk.component;

import com.tresorit.zerokitsdk.fragment.SignInFragment;
import com.tresorit.zerokitsdk.module.LoginModule;
import com.tresorit.zerokitsdk.scopes.ActivityScope;
import com.tresorit.zerokitsdk.viewmodel.LoginViewModel;

import dagger.Component;

@ActivityScope
@Component(
        modules = {
                LoginModule.class
        },
        dependencies = {
                ApplicationComponent.class
        }
)
public interface LoginComponent {
    void inject(SignInFragment fragment);
    LoginViewModel viewModel();
}
