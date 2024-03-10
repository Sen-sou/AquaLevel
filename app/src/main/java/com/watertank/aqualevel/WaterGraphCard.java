package com.watertank.aqualevel;

import android.content.Context;
import android.view.View;
import android.widget.RadioGroup;

import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

public class WaterGraphCard {
    private final Context context;
    private final View card;
    private final FragmentManager fragmentManager;

    private MaterialButton dateButton, clockButton;
    private WaterGraphView waterGraphView;
    private RadioGroup graphTime;

    public WaterGraphCard(Context context, FragmentManager fragmentManager, View card) {
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.card = card;
    }

    public void init() {
        dateButton = card.findViewById(R.id.dateButton);
        clockButton = card.findViewById(R.id.clockButton);
        waterGraphView = card.findViewById(R.id.waterGraph);
        graphTime = card.findViewById(R.id.graphTimeRadio);

        clockButton.setVisibility(View.GONE);

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select a Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();
        datePicker.addOnPositiveButtonClickListener(
                selection -> dateButton.setText(datePicker.getHeaderText())
        );

        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select Clock time")
                .build();
        timePicker.addOnPositiveButtonClickListener(
                v -> clockButton.setText(((Integer)timePicker.getHour()).toString())
        );

        dateButton.setOnClickListener(v -> datePicker.show(fragmentManager, "MATERIAL_DATE_PICKER"));
        clockButton.setOnClickListener(v -> timePicker.show(fragmentManager, "MATERIAL_TIME_PICKER"));

        graphTime.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.dayBtn:
                    clockButton.setVisibility(View.GONE);
                    waterGraphView.setXAxisSize(24);
                    break;
                case R.id.hourBtn:
                    clockButton.setVisibility(View.VISIBLE);
                    waterGraphView.setXAxisSize(60);
                    break;
                default:
                    break;
            }
        });
    }

}
