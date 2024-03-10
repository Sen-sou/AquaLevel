package com.watertank.aqualevel;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

public class MainActivity extends AppCompatActivity {

    TextView toolbarText;
    MaterialButton dateButton, clockButton;
    RadioGroup graphTime;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        toolbarText = findViewById(R.id.toolbarText);
        SpannableString spannableString = new SpannableString("AQUALEVEL");
        spannableString.setSpan(new ForegroundColorSpan(0xFF018786),
                0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        toolbarText.setText(spannableString);

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select a date")
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

        FragmentManager fragmentManager = getSupportFragmentManager();

        dateButton = findViewById(R.id.dateButton);
        clockButton = findViewById(R.id.clockButton);
        clockButton.setVisibility(View.GONE);

        dateButton.setOnClickListener(v -> datePicker.show(fragmentManager, "MATERIAL_DATE_PICKER"));
        clockButton.setOnClickListener(v -> timePicker.show(fragmentManager, "MATERIAL_TIME_PICKER"));

        graphTime = findViewById(R.id.graphTimeRadio);
        graphTime.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.dayBtn:
                        clockButton.setVisibility(View.GONE);
                        break;
                    case R.id.hourBtn:
                        clockButton.setVisibility(View.VISIBLE);
                        break;
                    default:
                        break;
                }
            }
        });









    }
}