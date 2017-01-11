package com.tresorit.zerokitsdk.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tresorit.zerokitsdk.ZerokitApplication;
import com.tresorit.zerokitsdk.component.DaggerEncryptTextComponent;
import com.tresorit.zerokitsdk.databinding.FragmentEncryptTextBinding;
import com.tresorit.zerokitsdk.viewmodel.EncryptTextViewModel;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;

public class EncryptTextFragment extends Fragment {

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    EncryptTextViewModel viewModel;

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    EventBus eventBus;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        DaggerEncryptTextComponent.builder().applicationComponent(ZerokitApplication.get(getActivity()).component()).build().inject(this);
        FragmentEncryptTextBinding binding = FragmentEncryptTextBinding.inflate(inflater, container, false);
        binding.setViewmodel(viewModel);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        eventBus.register(viewModel);
    }

    @Override
    public void onStop() {
        super.onStop();
        eventBus.unregister(viewModel);
    }
}
