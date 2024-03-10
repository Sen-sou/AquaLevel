package com.watertank.aqualevel;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

public class MainActivity extends AppCompatActivity {
    MaterialButton dateButton, clockButton;
    RadioGroup graphTime;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setToolbarTextEffect();

        WaterTankCard waterTankCard = new WaterTankCard(getBaseContext(), findViewById(R.id.waterTankCard));
        waterTankCard.init();

    }

    public void setToolbarTextEffect() {
        TextView toolbarText = findViewById(R.id.toolbarText);
        SpannableString spannableString = new SpannableString("AQUALEVEL");
        spannableString.setSpan(new ForegroundColorSpan(0xFF018786),
                0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        toolbarText.setText(spannableString);
    }
}

