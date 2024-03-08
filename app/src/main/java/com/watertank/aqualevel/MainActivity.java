package com.watertank.aqualevel;

import androidx.appcompat.app.AppCompatActivity;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    TextView toolbarText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbarText = findViewById(R.id.toolbarText);
        SpannableString spannableString = new SpannableString("AQUALEVEL");
        spannableString.setSpan(new ForegroundColorSpan(0xFF018786),
                0, 4, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        toolbarText.setText(spannableString);
    }
}