package com.tresorit.zerokitsdk.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tresorit.zerokitsdk.ZerokitApplication;
import com.tresorit.zerokitsdk.cache.ComponentControllerFragment;
import com.tresorit.zerokitsdk.component.DaggerRegistrationComponent;
import com.tresorit.zerokitsdk.component.RegistrationComponent;
import com.tresorit.zerokitsdk.databinding.FragmentRegistrationBinding;
import com.tresorit.zerokitsdk.viewmodel.RegistrationViewModel;

import javax.inject.Inject;

public class SignUpFragment extends ComponentControllerFragment<RegistrationComponent> {

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    RegistrationViewModel registrationViewModel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        getComponent().inject(this);
        FragmentRegistrationBinding binding = FragmentRegistrationBinding.inflate(inflater, container, false);
        binding.setViewmodel(registrationViewModel);
        return binding.getRoot();
    }


    @Override
    protected RegistrationComponent onCreateNonConfigurationComponent() {
        return DaggerRegistrationComponent.builder().applicationComponent(ZerokitApplication.get(getActivity()).component()).build();
    }
}
