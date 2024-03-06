package com.watertank.aqualevel;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;

public class WaterGraphView extends View {

    private Paint paintText, paintLine, paintPath, paintPathFill;
    private ArrayList<Float> percentValues;
    private ArrayList<Integer> timeValues;
    private Path graphPath, graphPathFilled;
    private float[] pathExtreme;
    private Shader gradientShader;
    private final int[] colors = {Color.CYAN, Color.TRANSPARENT};
    private final float[] positions = {0.0f, 1.0f};

    public WaterGraphView(Context context) {
        super(context);
    }

    public WaterGraphView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        init();
    }

    protected void init() {
        // Initialize Paints and Paths
        paintText = new Paint();
        paintLine = new Paint();
        paintPath = new Paint();
        paintPathFill = new Paint();
        graphPath = new Path();
        graphPathFilled = new Path();

        // Set Text Paint Properties
        paintText.setTextSize(30);
        paintText.setColor(Color.GRAY);

        // Set Line Paint Properties
        paintLine.setPathEffect(new DashPathEffect(new float[]{3, 3}, 0));
        paintLine.setColor(Color.GRAY);
        paintLine.setStyle(Paint.Style.STROKE);
        paintLine.setAlpha(100);

        // Set Path Paint Properties
        paintPath.setColor(Color.CYAN);
        paintPath.setStyle(Paint.Style.STROKE);
        paintPath.setStrokeWidth(6);

        // Set Gradient Paint Properties
        gradientShader = new LinearGradient(
                0, 0,
                0, getHeight(),
                colors, positions, Shader.TileMode.CLAMP);
        paintPathFill.setShader(gradientShader);
        paintPathFill.setStyle(Paint.Style.FILL);
        paintPathFill.setAlpha(50);

        // Initialize Water Graph X and Y values
        pathExtreme = new float[4];
        timeValues = new ArrayList<>(Collections.nCopies(61, 5));
        percentValues = new ArrayList<>(Collections.nCopies(61, 50.0f));

        updatePath(graphPath, false);
        updatePath(graphPathFilled, true);
    }

    public void setAxisValues(ArrayList<Integer> timeValuesX, ArrayList<Float> percentValuesY) {
        timeValues = new ArrayList<>(timeValuesX);
        percentValues = new ArrayList<>(percentValuesY);
    }

    public float getGraphX(float inc, int index) {
        return (0.13f + (inc * (float)index)) * getWidth();
    }
    public float getGraphY(float percentage) {
        return (0.9f - (0.75f * (percentage / 100.0f) + 0.0145f)) * getHeight();
    }

    public void updatePath(Path path, boolean fill) {
        path.reset();
        int sizeX = timeValues.size();
        int sizeY = percentValues.size();
        if (sizeX < 2 || sizeY == 0) return;

        // Calculate and Set Path Values
        float yInc = 0.8f / (float)(sizeX - 1);
        path.moveTo(getGraphX(yInc, 0), getGraphY(percentValues.get(0)));
        pathExtreme[0] = getGraphX(yInc, 0);
        pathExtreme[1] = getGraphY(percentValues.get(0));
        int i;
        for (i = 1; i < sizeX; i++) {
            if (i < sizeY)
                path.lineTo(getGraphX(yInc, i), getGraphY(percentValues.get(i)));
            else break;
        }
        pathExtreme[2] = getGraphX(yInc, i-1);
        pathExtreme[3] = getGraphY(percentValues.get(i-1));

        if (fill) closePath(path);
    }

    public void closePath(Path path) {
        path.lineTo(pathExtreme[2], getGraphY(0.0f));
        path.lineTo(pathExtreme[0], getGraphY(0.0f));
        path.close();
    }

    public float getYIntervalInc() {
        switch (timeValues.size()) {
            case 25: return 3.0f;
            case 61: return 6.0f;
        }
        return 1;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // Y Axis Draw
        int control = 100; // Set for Y Axis Percentage Calculation
        // Draw Y Values and Corresponding Dotted Lines
        for (float percentage = 0.15f; percentage < 1.0f; percentage += 0.15f, control -= 20) {
            canvas.drawText(control + "%",
                    0.01f * getWidth(), percentage * getHeight(), paintText);
            canvas.drawLine(0.13f * getWidth(), (percentage - 0.0145f) * getHeight(),
                    0.93f * getWidth(), (percentage - 0.0145f) * getHeight(), paintLine);
        }

        // X Axis Draw
        control = 0; // Set for X Axis Time Calculation
        // Draw X Values
        for (float interval = 0.13f; interval <= 0.93001f;
             interval += (getYIntervalInc() / (float)(timeValues.size()-1) * 0.8f)) {
            canvas.drawText( timeValues.get(control) + "",
                    (interval - 0.0105f) * getWidth(), 0.95f * getHeight(), paintText);
            control += getYIntervalInc();
        }

        // Draw Graph
        canvas.drawPath(graphPath, paintPath);
        canvas.drawCircle(pathExtreme[0], pathExtreme[1], 0.005f * getWidth(), paintPath);
        canvas.drawCircle(pathExtreme[2], pathExtreme[3], 0.005f * getWidth(), paintPath);
        canvas.drawPath(graphPathFilled, paintPathFill);
    }
}
