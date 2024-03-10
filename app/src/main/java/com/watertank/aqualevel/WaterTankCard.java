package com.watertank.aqualevel;

import android.content.Context;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.checkbox.MaterialCheckBox;

import java.util.Locale;

public class WaterTankCard {

    private final Context context;
    private final View card;


    private MaterialButton serverConnectButton;
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
    }

    public void setWaterPercentage(Float percentage) {
        waterTankView.setWaterLevel(percentage);
        waterPercentage.setText(String.format(Locale.getDefault(), "%.2f", percentage));
    }

}
