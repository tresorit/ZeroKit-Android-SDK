package com.tresorit.zerokitsdk.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.design.widget.Snackbar;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;

import com.tresorit.zerokit.Zerokit;
import com.tresorit.zerokit.observer.Action1;
import com.tresorit.zerokit.response.ResponseZerokitError;
import com.tresorit.zerokitsdk.R;
import com.tresorit.zerokitsdk.ZerokitApplication;
import com.tresorit.zerokitsdk.component.DaggerMainComponent;
import com.tresorit.zerokitsdk.databinding.ActivityMainBinding;
import com.tresorit.zerokitsdk.fragment.DecryptFragment;
import com.tresorit.zerokitsdk.fragment.EncryptFragment;
import com.tresorit.zerokitsdk.message.CopyEncryptedTextMessage;
import com.tresorit.zerokitsdk.message.TabSelectMessage;
import com.tresorit.zerokitsdk.viewmodel.MainViewModel;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import javax.inject.Inject;

import static com.tresorit.zerokitsdk.util.Util.dpToPx;

public class MainActivity extends AppCompatActivity {

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    MainViewModel viewModel;

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    EventBus eventBus;

    @SuppressWarnings({"WeakerAccess", "CanBeFinal"})
    @Inject
    Zerokit zerokit;

    private final EncryptFragment encryptFragment = new EncryptFragment();
    private final DecryptFragment decryptFragment = new DecryptFragment();


    @SuppressWarnings("WeakerAccess")
    ActivityMainBinding binding;
    @SuppressWarnings("WeakerAccess")
    String userId;

    private enum Mode {
        Encrypt, Decrypt
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DaggerMainComponent.builder().applicationComponent(ZerokitApplication.get(this).component()).build().inject(this);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        binding.setViewmodel(viewModel);
        whoAmI();
        binding.container.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                binding.bottomBar.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.bottomBar.setVisibility(binding.container.getRootView().getHeight() - binding.container.getHeight() > dpToPx(MainActivity.this, 200) ? View.GONE : View.VISIBLE);
                    }
                });
            }
        });
        if (savedInstanceState == null)
            showFragment(Mode.Encrypt);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                logout();
                break;

            case R.id.copyUserId:
                copyUserIdToClipboard(userId);
                break;
        }
        return super.onOptionsItemSelected(item);
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
    public void onEvent(TabSelectMessage message) {
        switch (message.getTabId()) {
            case R.id.tab_encrypt:
                showFragment(Mode.Encrypt);
                break;
            case R.id.tab_decrypt:
                showFragment(Mode.Decrypt);
                break;
        }
    }

    @Subscribe
    @SuppressWarnings("unused")
    public void onEvent(CopyEncryptedTextMessage message) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Activity.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Encrypted text", message.getEncryptedMessage());
        clipboard.setPrimaryClip(clip);
        showMessage("Encrypted text copied to clipboard");
    }


    private void showFragment(Mode mode) {
        switch (mode) {
            case Decrypt:
                getSupportFragmentManager().beginTransaction().replace(R.id.contentContainer, decryptFragment).commit();
                break;
            case Encrypt:
                getSupportFragmentManager().beginTransaction().replace(R.id.contentContainer, encryptFragment).commit();
                break;
        }
    }

    private void whoAmI() {
        zerokit.whoAmI().subscribe(new Action1<String>() {
            @Override
            public void call(String userId) {
                MainActivity.this.userId = userId;
                ActionBar supportActionBar = MainActivity.this.getSupportActionBar();
                if (supportActionBar != null) supportActionBar.setSubtitle("User Id: " + userId);
            }
        });
    }

    private void copyUserIdToClipboard(String userId) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("User Id", userId);
        clipboard.setPrimaryClip(clip);
        showMessage("Copied User Id: " + userId);
    }

    private void showMessage(String message) {
        Snackbar.make(binding.container, message, Snackbar.LENGTH_LONG).show();
    }

    private void logout() {
        final ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Logging out...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        zerokit.logout().subscribe(new Action1<String>() {
            @Override
            public void call(String s) {
                progressDialog.dismiss();
                Intent intent = new Intent(MainActivity.this, RootActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                MainActivity.this.startActivity(intent);
            }
        }, new Action1<ResponseZerokitError>() {
            @Override
            public void call(ResponseZerokitError s) {
                progressDialog.dismiss();
            }
        });
    }
}