package com.watertank.aqualevel;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.watertank.aqualevel.sensordataroom.DatabaseExecutorService;
import com.watertank.aqualevel.sensordataroom.SensorData;
import com.watertank.aqualevel.sensordataroom.SensorDataDao;
import com.watertank.aqualevel.sensordataroom.SensorDataDatabase;
import com.watertank.aqualevel.viewcomponents.WaterGraphCard;
import com.watertank.aqualevel.viewcomponents.WaterTankCard;

import java.util.concurrent.ExecutorService;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class MainActivity extends AppCompatActivity {
    MaterialButton dateButton, clockButton, buttonIncrease, buttonDecrease;
    WaterTankCard waterTankCard;
    WaterGraphCard waterGraphCard;
    RadioGroup graphTime;
    float percentage = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        DatabaseExecutorService databaseService = DatabaseExecutorService.getInstance(getApplicationContext());

        setToolbarTextEffect();
        getWindow().setNavigationBarColor(getResources().getColor(R.color.mainBackground, getTheme()));

        waterTankCard = new WaterTankCard(
                getApplicationContext(),
                findViewById(R.id.waterTankCard)
        );
        waterTankCard.init();

        waterGraphCard = new WaterGraphCard(
                getApplicationContext(),
                getSupportFragmentManager(),
                findViewById(R.id.waterGraphCard),
                SensorDataDatabase.getInstance(getApplicationContext())
        );
        waterGraphCard.init();


        SensorData sensorData = new SensorData();

        buttonIncrease = findViewById(R.id.increaseBtn);
        buttonIncrease.setOnClickListener(v -> {
            percentage += 10.0f;
            waterTankCard.setWaterPercentage(percentage);
            sensorData.setData(percentage);
            databaseService.insert(sensorData);
        });
        buttonDecrease = findViewById(R.id.decreaseBtn);
        buttonDecrease.setOnClickListener(v -> {
            percentage -= 10.0f;
            waterTankCard.setWaterPercentage(percentage);
            databaseService.resetTable(getApplicationContext());
        });

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
    }
}

