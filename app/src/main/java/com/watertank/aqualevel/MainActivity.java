package com.watertank.aqualevel;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.widget.TextView;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.watertank.aqualevel.networkService.DataListener;
import com.watertank.aqualevel.networkService.NetworkClientService;
import com.watertank.aqualevel.sensordataroom.DatabaseExecutorService;
import com.watertank.aqualevel.viewcomponents.PreferenceCard;
import com.watertank.aqualevel.viewcomponents.WaterGraphCard;
import com.watertank.aqualevel.viewcomponents.WaterTankCard;

import java.util.HashMap;

public class MainActivity extends AppCompatActivity { // MainActivity.this vs getApplicationContext

    private WaterTankCard waterTankCard;
    private WaterGraphCard waterGraphCard;
    private PreferenceCard preferenceCard;
    private NetworkClientService networkClientService;
    private DataListener serverDataListeners, serverDirectDataListeners;
    private ServiceConnection serviceConnection;

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d("MAIN_APP", "onCreate: ");

        NetworkClientService.mainAlive = true;

        String[] perms = {Manifest.permission.POST_NOTIFICATIONS};
        ActivityCompat.requestPermissions(this, perms,10);

        // Set Top bar Text Effect
        setToolbarTextEffect();
        getWindow().setNavigationBarColor(getResources().getColor(R.color.mainBackground, getTheme()));

        DatabaseExecutorService.getInstance(MainActivity.this);

        serverDataListeners = new DataListener();
        serverDirectDataListeners = new DataListener();

        waterTankCard = new WaterTankCard(
                getApplicationContext(),
                findViewById(R.id.waterTankCard),
                serverDataListeners,
                serverDirectDataListeners
        );
        waterTankCard.init();

        waterGraphCard = new WaterGraphCard(
                getApplicationContext(),
                getSupportFragmentManager(),
                findViewById(R.id.waterGraphCard),
                serverDataListeners,
                serverDirectDataListeners,
                findViewById(R.id.linearProgressBar)
        );
        waterGraphCard.init();

        preferenceCard = new PreferenceCard(MainActivity.this, findViewById(R.id.preferenceCard));
        preferenceCard.init();

        Intent serviceIntent = new Intent(getApplicationContext(), NetworkClientService.class);
        if (!networkServiceRunning()) serviceIntent.setAction(NetworkClientService.START);
        else serviceIntent.setAction(NetworkClientService.RELINK);
        serviceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                networkClientService = ((NetworkClientService.ClientServiceBinder) service).getService()
                        .setDataListener(serverDataListeners, serverDirectDataListeners)
                        .setSocketConnection("192.168.0.120", 80)
                        .setConnectionTimeout(10000);
                preferenceCard.setNetworkServiceObject(networkClientService);
                waterTankCard.setNetworkServiceObject(networkClientService);
                waterGraphCard.setNetworkServiceObject(networkClientService);
            }
            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.d("CLIENT SERVICE CONNECTION", "Serivce Disconnected ");
            }
        };
        bindService(serviceIntent, serviceConnection, BIND_AUTO_CREATE);
        startForegroundService(serviceIntent);

        // System.currentTimeMillis();

    }

    private boolean networkServiceRunning() {
        ActivityManager activityManager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE );
        for (ActivityManager.RunningServiceInfo serviceInfo: activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (NetworkClientService.class.getName().equals(serviceInfo.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void setToolbarTextEffect() {
        TextView toolbarText = findViewById(R.id.toolbarText);
        SpannableString spannableString = new SpannableString("AQUALEVEL");
        spannableString.setSpan(new ForegroundColorSpan(0xFF018786),
                0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        toolbarText.setText(spannableString);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DatabaseExecutorService.shutDownService();
        NetworkClientService.mainAlive = false;
        unbindService(serviceConnection);
        Log.d("CLIENT APP DESTROYED", "onDestroy: ");
    }
}

