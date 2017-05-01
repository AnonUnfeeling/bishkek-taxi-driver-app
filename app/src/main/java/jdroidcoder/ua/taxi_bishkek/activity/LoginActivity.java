package jdroidcoder.ua.taxi_bishkek.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import butterknife.ButterKnife;
import butterknife.OnClick;
import jdroidcoder.ua.taxi_bishkek.R;
import jdroidcoder.ua.taxi_bishkek.events.ErrorMessageEvent;
import jdroidcoder.ua.taxi_bishkek.events.MoveNextEvent;
import jdroidcoder.ua.taxi_bishkek.events.TypePhoneEvent;
import jdroidcoder.ua.taxi_bishkek.model.UserProfileDto;
import jdroidcoder.ua.taxi_bishkek.network.NetworkService;

public class LoginActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener {

    private final static int RC_SIGN_IN = 101;
    private GoogleApiClient googleApiClient;
    private NetworkService networkService;
    private UserProfileDto userProfileDto = new UserProfileDto();
    private String email;
    private boolean isSend = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_activity);
        ButterKnife.bind(this);
        EventBus.getDefault().register(this);
        networkService = new NetworkService();
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_PHONE_STATE},
                    123);
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        googleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @OnClick(R.id.sign_in_button)
    public void enter() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            handleSignInResult(result);
        }
    }

    @Override
    protected void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            if (!isSend) {
                GoogleSignInAccount acct = result.getSignInAccount();
                userProfileDto.setFirstName(acct.getGivenName());
                userProfileDto.setLastName(acct.getFamilyName());
                email = acct.getEmail();
                networkService.register(acct.getEmail(), acct.getId());
            } else {
                isSend = false;
            }
        } else {
            Toast.makeText(this, getString(R.string.internet_error), Toast.LENGTH_LONG).show();
        }
    }

    @Subscribe
    public void onMessageEvent(ErrorMessageEvent event) {
        Toast.makeText(this, event.getMessage(), Toast.LENGTH_LONG).show();
    }

    @Subscribe
    public void onMoveToNext(MoveNextEvent event) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                networkService.getOrders();
                networkService.getAllAcceptOrders(UserProfileDto.User.getPhone());
            }
        }).start();
        startActivity(new Intent(this, OrdersActivity.class));
        finish();
    }

    @Subscribe
    public void onTypeEvent(TypePhoneEvent event) {
//        final View view = LayoutInflater.from(this).inflate(R.layout.alert_style, null);
//        final AlertDialog alertDialog = new AlertDialog.Builder(this)
//                .setView(view)
//                .create();
//        final EditText phoneET = (EditText) view.findViewById(R.id.phone);
//        phoneET.setTextColor(getResources().getColor(android.R.color.white));
//        phoneET.setText(UserProfileDto.User.getPhone());
//        view.findViewById(R.id.save).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                EditText phoneET = (EditText) view.findViewById(R.id.phone);
//                if (!TextUtils.isEmpty(phoneET.getText().toString())) {
        TelephonyManager tMgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        String mPhoneNumber = tMgr.getLine1Number();
        System.out.println(mPhoneNumber);
        userProfileDto.setPhone(mPhoneNumber);
        networkService.setDataToProfile(email, userProfileDto.getFirstName(),
                userProfileDto.getLastName(), userProfileDto.getPhone());
//                    alertDialog.dismiss();
//    }
//            }
//        });
//
//        alertDialog.show();
    }
}
