package com.watertank.aqualevel;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.watertank.aqualevel.sensordataroom.SensorDataDatabase;
import com.watertank.aqualevel.viewcomponents.WaterGraphCard;
import com.watertank.aqualevel.viewcomponents.WaterTankCard;

public class MainActivity extends AppCompatActivity {
    MaterialButton dateButton, clockButton;
    RadioGroup graphTime;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setToolbarTextEffect();
        SensorDataDatabase db = Room.databaseBuilder(
                getApplicationContext(), SensorDataDatabase.class, "SensorData").build();

        WaterTankCard waterTankCard = new WaterTankCard(
                getApplicationContext(), findViewById(R.id.waterTankCard));
        waterTankCard.init();

        WaterGraphCard waterGraphCard = new WaterGraphCard(
                getApplicationContext(), getSupportFragmentManager(), findViewById(R.id.waterGraphCard), db);
        waterGraphCard.init();


    }

    public void setToolbarTextEffect() {
        TextView toolbarText = findViewById(R.id.toolbarText);
        SpannableString spannableString = new SpannableString("AQUALEVEL");
        spannableString.setSpan(new ForegroundColorSpan(0xFF018786),
                0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        toolbarText.setText(spannableString);
    }
}

