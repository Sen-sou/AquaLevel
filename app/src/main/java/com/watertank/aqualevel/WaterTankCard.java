package com.watertank.aqualevel;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.Locale;

public class WaterTankCard {

    private final Context context;
    private final View card;


    private MaterialButton serverConnectButton;
    private String serverStatusMessage;
    boolean statusShow;
    private WaterTankView waterTankView;
    private TextView waterPercentage;
    private MaterialCheckBox notifyLevelCheckBox;


    public WaterTankCard(Context context, View rootView) {
        this.context = context;
        this.card = rootView;
    }

    public void init() {
        serverConnectButton = card.findViewById(R.id.serverConnectionButton);
        waterTankView = card.findViewById(R.id.waterTank);
        waterPercentage = card.findViewById(R.id.waterPercentage);
        notifyLevelCheckBox = card.findViewById(R.id.notifyLevelChkBox);

        serverStatusMessage = "Disconnected from Server";
        statusShow = true;
        serverConnectButton.setOnClickListener(v -> {
            if (statusShow) serverConnectButton.setText(serverStatusMessage);
            else serverConnectButton.setText("");
            statusShow = !statusShow;
        });

        notifyLevelCheckBox.addOnCheckedStateChangedListener((checkBox, state) -> {

        });
    }

    public void setServerConnectionStatus(boolean status) {
        if (status) {
            serverStatusMessage = "Connected to Server";
            serverConnectButton.setIconResource(R.drawable.connected_icon);
            serverConnectButton.setIconTintResource(R.color.teal_700);
        } else {
            serverStatusMessage = "Disconnected from Server";
            serverConnectButton.setIconResource(R.drawable.notconnected_icon);
            serverConnectButton.setIconTintResource(R.color.cardtitle);
        }
    }
    public void setWaterPercentage(Float percentage) {
        waterTankView.setWaterLevel(percentage);
        waterPercentage.setText(String.format(Locale.getDefault(), "%.1f%%", percentage));
    }

}
