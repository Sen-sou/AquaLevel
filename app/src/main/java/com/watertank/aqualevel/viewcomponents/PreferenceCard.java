package com.watertank.aqualevel.viewcomponents;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.slider.RangeSlider;
import com.google.android.material.textview.MaterialTextView;
import com.watertank.aqualevel.R;
import com.watertank.aqualevel.networkService.NetworkClientService;
import com.watertank.aqualevel.sensordataroom.DatabaseExecutorService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PreferenceCard {

    private final Context context;
    private final View card;
    private final DatabaseExecutorService databaseService;

    // Components
    private MaterialSwitch syncSwitch, alarmSwitch;
    private MaterialButton updateButton, deleteButton;
    private MaterialAlertDialogBuilder dialogBuilder;
    private MaterialTextView intervalTxt;
    private PopupMenu intervalMenu;
    private RangeSlider safeRangeSlider;

    // Dependencies
    private NetworkClientService networkClientService;
    private SharedPreferences preferences;
    private SharedPreferences.Editor prefEdit;

    // User Preferences
    private int updateInterval;
    private int lastReadByte;
    private boolean alarmNotification;
    private boolean autoSyncHistory;
    private float safeMin;
    private float safeMax;


    public PreferenceCard(Context context, View card) {
        this.context = context;
        this.card = card;
        this.databaseService = DatabaseExecutorService.getInstance(context);
        this.preferences = context.getSharedPreferences("Aqua_Client", Context.MODE_PRIVATE);
        this.prefEdit = this.preferences.edit();
    }

    public void init() {
        intervalTxt = card.findViewById(R.id.intervalTxtView);
        syncSwitch = card.findViewById(R.id.syncSwitch);
        alarmSwitch = card.findViewById(R.id.alarmSwitch);
        updateButton = card.findViewById(R.id.updateBtn);
        deleteButton = card.findViewById(R.id.deleteBtn);
        safeRangeSlider = card.findViewById(R.id.safeLevelRange);

        // Get Saved Values
        updateInterval = preferences.getInt("dataGetInterval", 1);
        lastReadByte = preferences.getInt("lastReadByte", 0);
        alarmNotification = preferences.getBoolean("alarmNotifyState", false);
        autoSyncHistory = preferences.getBoolean("autoSyncState", false);
        safeMin = preferences.getFloat("safeLevelMin", 5.0f);
        safeMax = preferences.getFloat("safeLevelMax", 90.0f);

        // Interval Selector
        intervalTxt.setText(updateInterval + " SEC");
        intervalMenu = new PopupMenu(context,
                intervalTxt, Gravity.END, 0, R.style.AquaTheme_popup);
        intervalMenu.getMenuInflater().inflate(R.menu.interval_menu, intervalMenu.getMenu());
        intervalMenu.setOnMenuItemClickListener(item -> {
            intervalTxt.setText(item.getTitle());
            String titleText = item.getTitle().toString();
            int selected = Integer.parseInt(titleText.substring(0, titleText.indexOf(" ")));

            prefEdit.putInt("dataGetInterval", selected);
            prefEdit.apply();

            networkClientService.addRequest("<setConfig/dataGetInterval:" + selected + ">");
            networkClientService.setSocketTimeout((selected > 4) ? selected * 2000 : 5000);
            return true;
        });
        intervalTxt.setOnClickListener(v -> intervalMenu.show());

        // Auto Sync History
        syncSwitch.setChecked(autoSyncHistory);
        syncSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) networkClientService.addRequest("<setConfig/allowLog:1>");
            else networkClientService.addRequest("<setConfig/allowLog:0>");

            prefEdit.putBoolean("autoSyncState", isChecked);
            prefEdit.apply();
        });

        // Notify by Alarm
        alarmSwitch.setChecked(alarmNotification);
        alarmSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            networkClientService.setAlarmState(isChecked);
            prefEdit.putBoolean("alarmNotifyState", isChecked);
            prefEdit.apply();
        });

        // Sync History
        updateButton.setOnClickListener(v -> {
            networkClientService.addRequest("<setConfig/loadLog:1>");
        });

        // Clear History
        dialogBuilder = new MaterialAlertDialogBuilder(context, R.style.Theme_AquaLevel_dialog)
                .setTitle("ARE YOU SURE?")
                .setMessage("This will Clear All History").setBackground(AppCompatResources
                        .getDrawable(context, R.drawable.aqua_popup_background))
                .setPositiveButton("CONFIRM", (dialog, which) -> {
                    if (databaseService.resetTable(context)) {
                        Toast.makeText(context, "History Cleared", Toast.LENGTH_SHORT).show();
                        networkClientService.addRequest("<setConfig/lastReadByte:0>");
                        networkClientService.sendInMessage("<invalidateGraph/1>");
                        prefEdit.putInt("lastReadByte", 0);
                        prefEdit.apply();
                    }
                });
        deleteButton.setOnClickListener(v -> dialogBuilder.show());

        // Safe Level Range Select
        safeRangeSlider.setValues(Arrays.asList(safeMin, safeMax));
        safeRangeSlider.addOnSliderTouchListener(new RangeSlider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull RangeSlider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull RangeSlider slider) {
                List<Float> values = slider.getValues();
                safeMin = values.get(0);
                safeMax = values.get(1);
                networkClientService.setSafeMinMax(safeMin, safeMax);
                prefEdit.putFloat("safeLevelMin", safeMin);
                prefEdit.putFloat("safeLevelMax", safeMax);
                prefEdit.apply();
            }
        });

    }

    public void setNetworkServiceObject(NetworkClientService networkClientService) {
        this.networkClientService = networkClientService;
    }

}
