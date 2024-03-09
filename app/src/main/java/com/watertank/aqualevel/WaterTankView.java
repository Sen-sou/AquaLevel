package com.watertank.aqualevel;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class WaterTankView extends View {

    private Bitmap tankFrame, tankInside;
    private Rect tankSrc, tankDst, waterRect;
    private Paint waterPaint;
    private float MAX_WATER_HEIGHT, waterLevel;


    public WaterTankView(Context context) {
        super(context);
    }

    public WaterTankView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        init();
    }

    protected void init() {
        // Import drawables and Set Rect
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        tankInside = BitmapFactory.decodeResource(getResources(), R.drawable.tankinside, options);
        tankFrame = BitmapFactory.decodeResource(getResources(), R.drawable.tankframe, options);
        tankSrc = new Rect(0, 0, 551, 551);
        tankDst = new Rect(0, 0, getWidth(), getHeight());

        // Setup Water Draw
        waterPaint = new Paint();
        waterPaint.setColor(Color.CYAN);
        waterPaint.setStyle(Paint.Style.FILL);
        waterPaint.setAlpha(32);
        waterRect = new Rect((int)(0.15245f * getWidth()),
                (int)(0.88566f * getHeight()),
                (int)(0.84573f * getWidth()),
                (int)(0.88566f * getHeight()));
        MAX_WATER_HEIGHT = (int)(0.76951f * getHeight());

        setWaterLevel(40.0f);
    }

    public boolean setWaterLevel(float percentage) {
        if (percentage >= 0 && percentage <= 100) {
            waterLevel = percentage;
            waterRect.top = waterRect.bottom - (int)((percentage / 100) * MAX_WATER_HEIGHT);
            invalidate();
            return false;
        }
        return true;
    }

    public float getWaterLevel() {
        return waterLevel;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawBitmap(tankInside, tankSrc, tankDst, null);
        canvas.drawRect(waterRect, waterPaint);
        canvas.drawBitmap(tankFrame, tankSrc, tankDst, null);
    }
}
