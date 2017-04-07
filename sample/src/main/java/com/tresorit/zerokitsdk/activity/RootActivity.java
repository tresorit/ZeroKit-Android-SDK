package com.tresorit.zerokitsdk.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokit.call.Action;
import com.tresorit.zerokit.response.ResponseZerokitError;
import com.tresorit.zerokitsdk.ZerokitApplication;
import com.tresorit.zerokitsdk.component.DaggerRootComponent;

import javax.inject.Inject;

public class RootActivity extends AppCompatActivity {

    private static final int REQ_DEFAULT = 0;

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    Zerokit zerokit;

    private boolean start = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerRootComponent.builder().applicationComponent(ZerokitApplication.get(this).component()).build().inject(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        start = true;
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!start) finish();
        else {
            start = false;
            zerokit.whoAmI().enqueue(new Action<String>() {
                @Override
                public void call(String response) {
                    if ("null".equals(response))
                        startActivityForResult(new Intent(RootActivity.this, SignInActivity.class), REQ_DEFAULT);
                    else
                        startActivityForResult(new Intent(RootActivity.this, MainActivity.class), REQ_DEFAULT);
                }
            }, new Action<ResponseZerokitError>() {
                @Override
                public void call(ResponseZerokitError responseZerokitError) {
                    Toast.makeText(RootActivity.this, responseZerokitError.getDescription(), Toast.LENGTH_SHORT).show();
                    startActivityForResult(new Intent(RootActivity.this, SignInActivity.class), REQ_DEFAULT);
                }
            });

        }
    }
}