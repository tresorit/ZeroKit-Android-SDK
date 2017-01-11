package com.tresorit.zerokitsdk.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tresorit.zerokitsdk.ZerokitApplication;
import com.tresorit.zerokitsdk.component.DaggerEncryptComponent;
import com.tresorit.zerokitsdk.databinding.FragmentEncryptBinding;
import com.tresorit.zerokitsdk.message.CreateTresorFinishedMessage;
import com.tresorit.zerokitsdk.module.EncryptModule;
import com.tresorit.zerokitsdk.viewmodel.EncryptViewModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import javax.inject.Inject;

public class EncryptFragment extends Fragment {

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    EncryptViewModel viewModel;

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    EventBus eventBus;

    @SuppressWarnings("WeakerAccess")
    FragmentEncryptBinding binding;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentEncryptBinding.inflate(inflater, container, false);
        DaggerEncryptComponent.builder().applicationComponent(ZerokitApplication.get(getActivity()).component()).encryptModule(new EncryptModule(getChildFragmentManager())).build().inject(this);
        binding.setViewmodel(viewModel);
        binding.pager.setPagingEnabled(false);
        return binding.getRoot();
    }

    @Override
    public void onStart() {
        super.onStart();
        eventBus.register(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        eventBus.unregister(this);
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onEvent(@SuppressWarnings("UnusedParameters") CreateTresorFinishedMessage message){
        binding.pager.setPagingEnabled(true);
        binding.pager.postDelayed(new Runnable() {
            @Override
            public void run() {
                binding.pager.setCurrentItem(1);
            }
        }, 500);
    }
}
