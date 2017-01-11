package com.tresorit.zerokitsdk.fragment;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.tresorit.zerokitsdk.ZerokitApplication;
import com.tresorit.zerokitsdk.component.DaggerCreateTresorComponent;
import com.tresorit.zerokitsdk.databinding.FragmentCreateTresorBinding;
import com.tresorit.zerokitsdk.viewmodel.CreateTresorViewModel;

import javax.inject.Inject;


public class CreateTresorFragment extends Fragment {

    @SuppressWarnings("WeakerAccess")
    @Inject
    CreateTresorViewModel viewModel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        DaggerCreateTresorComponent.builder().applicationComponent(ZerokitApplication.get(getActivity()).component()).build().inject(this);
        FragmentCreateTresorBinding binding = FragmentCreateTresorBinding.inflate(inflater, container, false);
        binding.setViewmodel(viewModel);
        return binding.getRoot();
    }
}
