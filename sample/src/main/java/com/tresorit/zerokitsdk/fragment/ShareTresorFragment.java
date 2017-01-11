package com.tresorit.zerokitsdk.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tresorit.zerokitsdk.ZerokitApplication;
import com.tresorit.zerokitsdk.component.DaggerShareTresorComponent;
import com.tresorit.zerokitsdk.databinding.FragmentShareTresorBinding;
import com.tresorit.zerokitsdk.viewmodel.ShareTresorViewModel;

import org.greenrobot.eventbus.EventBus;

import javax.inject.Inject;

public class ShareTresorFragment extends Fragment {

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    ShareTresorViewModel viewModel;

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    EventBus eventBus;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        DaggerShareTresorComponent.builder().applicationComponent(ZerokitApplication.get(getActivity()).component()).build().inject(this);
        FragmentShareTresorBinding binding = FragmentShareTresorBinding.inflate(inflater, container, false);
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
