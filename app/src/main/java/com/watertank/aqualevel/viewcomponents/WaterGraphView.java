package com.watertank.aqualevel.viewcomponents;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import io.reactivex.rxjava3.core.Flowable;

public class WaterGraphView extends View {

    private Paint paintText, paintLine, paintPath, paintPathFill;
    private ArrayList<Float> percentValues;
    private int xAxisSize;
//    private Path graphPath, graphPathFilled;
    private ArrayList<Path> graphPath, graphPathFilled;
    private float[] pathExtreme;
    private final int[] colors = {Color.CYAN, Color.TRANSPARENT};
    private final float[] positions = {0.0f, 1.0f};

    private HashMap<Integer, Float> timeYIntervalMem;

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

    public void init() {
        // Initialize Paints and Paths
        paintText = new Paint();
        paintLine = new Paint();
        paintPath = new Paint();
        paintPathFill = new Paint();

        graphPath = new ArrayList<>();
        graphPathFilled = new ArrayList<>();

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
        Shader gradientShader = new LinearGradient(
                0, 0,
                0, getHeight(),
                colors, positions, Shader.TileMode.CLAMP);
        paintPathFill.setShader(gradientShader);
        paintPathFill.setStyle(Paint.Style.FILL);
        paintPathFill.setAlpha(50);

        // Initialize Water Graph X and Y values
        pathExtreme = new float[4];
        xAxisSize = 24;
        percentValues = new ArrayList<>();
//        Random random = new Random();
//        for (int i = 0; i < xAxisSize; i++) percentValues.add(random.nextFloat() * 100.0f);
        timeYIntervalMem = new HashMap<>();
//        updatePath(graphPath, false);
//        updatePath(graphPathFilled, true);
    }

    public void setXAxisSize(int xAxisSize) {
        this.xAxisSize = xAxisSize;
        invalidate();
    }

    public void setYAxisValues(ArrayList<Float> percentValuesY) {
        percentValues = new ArrayList<>(percentValuesY);
        updatePath(graphPath, false);
        updatePath(graphPathFilled, true);
        invalidate();
    }

    public void clearGraph() {
        percentValues.clear();
        invalidate();
    }

    protected float getGraphX(float inc, int index) {
        return (0.13f + (inc * (float)index)) * getWidth();
    }
    protected float getGraphY(float percentage) {
        return (0.9f - (0.75f * (percentage / 100.0f) + 0.0145f)) * getHeight();
    }

    protected void updatePath(ArrayList<Path> pathList, boolean fill) {
        pathList.clear();
        int xStart;
        int sizeY = percentValues.size();
        if (xAxisSize == 0 || sizeY == 0) return;

        // get Starting pos
        for (xStart = 0; xStart < sizeY; xStart++) {
            if (percentValues.get(xStart) != null) break;
        }

        // Calculate and Set Path Values
        Path currPath = new Path();
        float xInc = 0.8f / (float)xAxisSize;
        float startPointX, endPointX;
        pathList.add(currPath);
        pathExtreme[0] = getGraphX(xInc, xStart);
        pathExtreme[1] = getGraphY(percentValues.get(xStart));
        currPath.moveTo(pathExtreme[0], pathExtreme[1]);
        startPointX = pathExtreme[0];
        endPointX = pathExtreme[0];
        boolean newFlag = false;

        // Set Path values
        for (++xStart; xStart < xAxisSize && xStart < sizeY; xStart++) {
            if (percentValues.get(xStart) == null) { // blank path for null values
                newFlag = true;
            } else if (newFlag) { // create new Path
                if (fill) closePath(currPath, startPointX, endPointX);
                currPath = new Path();
                currPath.moveTo(getGraphX(xInc, xStart), getGraphY(percentValues.get(xStart)));
                startPointX = getGraphX(xInc, xStart);
                pathList.add(currPath);
                newFlag = false;
            }
            else { // extend Path
                currPath.lineTo(getGraphX(xInc, xStart), getGraphY(percentValues.get(xStart)));
                endPointX = getGraphX(xInc, xStart);
            }
        }
        pathExtreme[2] = getGraphX(xInc, xStart-1);
        pathExtreme[3] = getGraphY(percentValues.get(xStart-1));
        if (fill) closePath(currPath, startPointX, endPointX);
    }

    protected void closePath(Path path, float start, float end) {
        path.lineTo(end, getGraphY(0.0f));
        path.lineTo(start, getGraphY(0.0f));
        path.close();
    }

    protected float getYIntervalInc() {
        // Check and return for available values
        if (timeYIntervalMem.containsKey(xAxisSize))
            return timeYIntervalMem.get(xAxisSize);
        // Calculating required interval Factor
        for (float i = (float)Math.ceil(xAxisSize / 10.0f);
             i <= Math.floor(xAxisSize) / 2.0; i++) {
            if (xAxisSize % i == 0.00f) {
                timeYIntervalMem.put(xAxisSize, i);
                return i;
            }
        }
        return (float)xAxisSize;
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
             interval += (getYIntervalInc() / (float)xAxisSize * 0.8f)) {
            canvas.drawText( control + "",
                    (interval - 0.0105f) * getWidth(), 0.95f * getHeight(), paintText);
            control += getYIntervalInc();
            if (control == xAxisSize) control = 0;
        }

        // Draw Graph
        if (percentValues.size() != 0) {
            for (Path path : graphPath) {
                canvas.drawPath(path, paintPath);
            }
            canvas.drawCircle(pathExtreme[0], pathExtreme[1], 0.005f * getWidth(), paintPath);
            canvas.drawCircle(pathExtreme[2], pathExtreme[3], 0.005f * getWidth(), paintPath);
            for (Path path : graphPathFilled){
                canvas.drawPath(path, paintPathFill);
            }
        }

    }
}
