package com.tresorit.zerokitsdk.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tresorit.zerokitsdk.ZerokitApplication;
import com.tresorit.zerokitsdk.component.DaggerDecryptComponent;
import com.tresorit.zerokitsdk.databinding.FragmentDecryptBinding;
import com.tresorit.zerokitsdk.viewmodel.DecryptViewModel;

import javax.inject.Inject;

public class DecryptFragment extends Fragment {

    @SuppressWarnings("WeakerAccess")
    @Inject
    DecryptViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        DaggerDecryptComponent.builder().applicationComponent(ZerokitApplication.get(getActivity()).component()).build().inject(this);
        FragmentDecryptBinding binding = FragmentDecryptBinding.inflate(inflater, container, false);
        binding.setViewmodel(viewModel);
        return binding.getRoot();
    }
}
