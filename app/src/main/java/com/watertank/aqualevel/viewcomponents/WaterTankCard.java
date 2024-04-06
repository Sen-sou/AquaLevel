package com.watertank.aqualevel.viewcomponents;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.AnimationDrawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.TextView;


import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;
import com.watertank.aqualevel.R;
import com.watertank.aqualevel.networkService.DataListener;
import com.watertank.aqualevel.networkService.NetworkClientService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class WaterTankCard {

    private final Context context;
    private DataListener serverDataListeners, serverDirectDataListeners;
    private final View card;

    private NetworkClientService networkClientService;
    private MaterialButton serverConnectButton;
    private String serverStatusMessage;
    private final int NOT_CONNECTED = 0;
    private final int CONNECTING = 1;
    private final int CONNECTED = 2;
    AnimationDrawable serverConnecting;
    boolean statusShow;
    private WaterTankView waterTankView;
    private TextView waterPercentage;
    private MaterialCheckBox notifyLevelCheckBox;

    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEdit;
    private boolean notifyCheck;


    public WaterTankCard(Context context, View rootView, DataListener dataListeners,
                         DataListener directDataListeners) {
        this.context = context;
        this.card = rootView;
        this.serverDataListeners = dataListeners;
        this.serverDirectDataListeners = directDataListeners;
        this.preferences = context.getSharedPreferences("Aqua_Client", Context.MODE_PRIVATE);
        this.prefEdit = this.preferences.edit();
    }

    public void init() {
        serverConnectButton = card.findViewById(R.id.serverConnectionButton);
        waterTankView = card.findViewById(R.id.waterTank);
        waterPercentage = card.findViewById(R.id.waterPercentage);
        notifyLevelCheckBox = card.findViewById(R.id.notifyLevelChkBox);

        notifyCheck = preferences.getBoolean("notifyState", false);


        // Set Server Status
        serverStatusMessage = "Couldn't Connect to Server";
        statusShow = false;
        serverConnectButton.setOnClickListener(v -> {
            statusShow = !statusShow;
            if (statusShow) {
                serverConnectButton.setText(serverStatusMessage);
            }
            else {
                serverConnectButton.setText("");
                if (networkClientService != null ) networkClientService.retryConnect();
            }
        });
        serverConnecting = (AnimationDrawable) ResourcesCompat.getDrawable(
                context.getResources(), R.drawable.wifi_animation, context.getTheme());

        serverDirectDataListeners.add("connectionStatus",
                received -> setServerConnectionStatus(Integer.parseInt(received)));

        // Set Tank Level Listener
        serverDataListeners.add("sensorRead",
                received -> setWaterPercentage((81.0f - Float.parseFloat(received)) * 1.2345679f));

        // Set Notify Button
        notifyLevelCheckBox.setChecked(notifyCheck);
        notifyLevelCheckBox.addOnCheckedStateChangedListener((checkBox, state) -> {
            if (networkClientService != null && networkClientService.getConnectionStatus()){
                networkClientService.setAlertState(state == 1);
            }
            prefEdit.putBoolean("notifyState", (state == 1));
            prefEdit.apply();
        });

    }

    public void setNetworkServiceObject(NetworkClientService networkClientService) {
        this.networkClientService = networkClientService;
    }

    public void setServerConnectionStatus(int status) {
        switch (status) {
            case CONNECTED:
                serverStatusMessage = "Connected to Server";
                serverConnecting.stop();
                serverConnectButton.setIconResource(R.drawable.connected_icon);
                serverConnectButton.setIconTintResource(R.color.teal_700);
                break;
            case CONNECTING:
                serverStatusMessage = "Connecting to Server";
                serverConnectButton.setIcon(serverConnecting);
                serverConnectButton.setIconTintResource(R.color.cardtitle);
                serverConnecting.start();
                break;
            case NOT_CONNECTED:
                serverStatusMessage = "Couldn't Connect to  Server";
                serverConnecting.stop();
                serverConnectButton.setIconResource(R.drawable.notconnected_icon);
                serverConnectButton.setIconTintResource(R.color.cardtitle);
                break;
        }
        if (statusShow) serverConnectButton.setText(serverStatusMessage);
    }
    public void setWaterPercentage(Float percentage) {
        if (percentage < 0.0f) {
            waterTankView.setWaterLevel(0.0f);
            waterPercentage.setText("err");
            return;
        } else if (percentage > 100.0f) {
            waterTankView.setWaterLevel(0.0f);
            waterPercentage.setText("100+%");
        }
        waterTankView.setWaterLevel(percentage);
        waterPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", percentage));
    }


}
