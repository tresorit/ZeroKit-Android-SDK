package com.tresorit.zerokitsdk.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tresorit.zerokitsdk.ZerokitApplication;
import com.tresorit.zerokitsdk.cache.ComponentControllerFragment;
import com.tresorit.zerokitsdk.component.DaggerLoginComponent;
import com.tresorit.zerokitsdk.component.LoginComponent;
import com.tresorit.zerokitsdk.databinding.FragmentLoginBinding;
import com.tresorit.zerokitsdk.viewmodel.LoginViewModel;

import javax.inject.Inject;

public class SignInFragment extends ComponentControllerFragment<LoginComponent> {

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    LoginViewModel loginViewModel;

    @Override
    protected LoginComponent onCreateNonConfigurationComponent() {
        return DaggerLoginComponent.builder().applicationComponent(ZerokitApplication.get(getActivity()).component()).build();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getComponent().inject(this);
        FragmentLoginBinding binding = FragmentLoginBinding.inflate(inflater, container, false);
        binding.setViewmodel(loginViewModel);
        return binding.getRoot();
    }


}
