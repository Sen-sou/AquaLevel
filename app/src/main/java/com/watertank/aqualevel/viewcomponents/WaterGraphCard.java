package com.watertank.aqualevel.viewcomponents;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Parcel;
import android.util.Log;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.watertank.aqualevel.R;
import com.watertank.aqualevel.networkService.DataListener;
import com.watertank.aqualevel.networkService.NetworkClientService;
import com.watertank.aqualevel.sensordataroom.DatabaseExecutorService;
import com.watertank.aqualevel.sensordataroom.SensorData;
import com.watertank.aqualevel.sensordataroom.SensorDataDao;
import com.watertank.aqualevel.sensordataroom.SensorDataDatabase;
import com.watertank.aqualevel.sensordataroom.SensorDate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import io.reactivex.rxjava3.core.Flowable;

public class WaterGraphCard {
    private final Context context;
    private final View card;
    private final FragmentManager fragmentManager;
    private final DatabaseExecutorService databaseService;

    // Calendar Resources
    private MaterialDatePicker<Long> datePicker;
    private Long selectedDate = null;
    private MaterialTimePicker timePicker;
    private Integer selectedHour = null;
    private MaterialButton dateButton, clockButton;
    private CalendarConstraints calendarConstraints;
//    CalendarConstraints.DateValidator dateValidator;
    private CalendarConstraints.Builder calendarConstraintsBuilder;
    private Calendar startDate, endDate;
    private ArrayList<SensorDate> validDates;
    private HashMap<SensorDate, Boolean> validLookup;

    // Graph Resources
    private WaterGraphView waterGraphView;
    private RadioGroup graphTime;

    // Network Resources
    private NetworkClientService networkClientService;
    private HashMap<String, DataListener> serverDataListeners;
    private HashMap<String, DataListener> directDataListeners;
    private LinearProgressIndicator progressIndicator;

    public WaterGraphCard(Context context, FragmentManager fragmentManager, View card,
                          HashMap<String, DataListener> dataListeners, HashMap<String,
                          DataListener> directDataListeners, LinearProgressIndicator progress) {
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.card = card;
        this.databaseService = DatabaseExecutorService.getInstance(context);
        this.serverDataListeners = dataListeners;
        this.directDataListeners = directDataListeners;
        this.progressIndicator = progress;
    }

    public void init() {
        dateButton = card.findViewById(R.id.dateButton);
        clockButton = card.findViewById(R.id.clockButton);
        waterGraphView = card.findViewById(R.id.waterGraph);
        graphTime = card.findViewById(R.id.graphTimeRadio);

        clockButton.setVisibility(View.GONE);

        calendarConstraintsBuilder = new CalendarConstraints.Builder();
        validLookup = new HashMap<>();
        buildCalender();
        buildClock();
        graphTime.setOnCheckedChangeListener((group, checkedId) -> {
            switch (checkedId) {
                case R.id.dayBtn:
                    clockButton.setVisibility(View.GONE);
                    waterGraphView.setXAxisSize(24);
                    waterGraphView.clearGraph();
                    if (selectedDate != null) {
                        setGraph(true);
                    }
                    break;
                case R.id.hourBtn:
                    clockButton.setVisibility(View.VISIBLE);
                    waterGraphView.setXAxisSize(60);
                    waterGraphView.clearGraph();
                    if (selectedDate != null && selectedHour != null) {
                        setGraph(false);
                    }
                    break;
                default:
                    break;
            }
        });

        progressIndicator.hide();
        serverDataListeners.put("historySync", received -> {
            int sensorLogSize = Integer.parseInt(received.substring(0, received.indexOf("/")));
            received = received.substring(received.indexOf("/") + 1);
            int lastReadByte = Integer.parseInt(received.substring(0, received.indexOf("/")));
            received = received.substring(received.indexOf("/") + 1);
            databaseService.saveDataHistory(received);

            if (lastReadByte == 0) return;
            if (!progressIndicator.isShown()) {
                progressIndicator.setProgress(0);
                progressIndicator.show();
            }
            progressIndicator.setProgressCompat((int) (((float) lastReadByte / (float) sensorLogSize) * 100.0f), true);
            if (lastReadByte == sensorLogSize) {
                progressIndicator.hide();
                buildCalender();
            }
        });

        directDataListeners.put("invalidateGraph", received -> {
            if (received.equalsIgnoreCase("1")) {
                waterGraphView.clearGraph();
                buildCalender();
                selectedDate = null;
                selectedHour = null;
                dateButton.setText("Date");
                clockButton.setText("Time");
            }
        });

    }

    protected void buildCalender() {

        validDates = (ArrayList<SensorDate>) databaseService.getDates();
        validLookup.clear();
        startDate = Calendar.getInstance();
        endDate = Calendar.getInstance();

        if (validDates.size() > 0) {

            // Set Extreme dates
            startDate.set(validDates.get(0).getYear(),
                    validDates.get(0).getMonth() - 1,
                    validDates.get(0).getDay());
            endDate.set(validDates.get(validDates.size() - 1).getYear(),
                    validDates.get(validDates.size() - 1).getMonth() - 1,
                    validDates.get(validDates.size() - 1).getDay());

            // Set HashMap for Valid Dates
            for (SensorDate date :
                    validDates) {
                validLookup.put(date, true);
            }
        }

        // Zero set to non essential variables
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MILLISECOND, 0);
        endDate.set(Calendar.HOUR_OF_DAY, 23);
        endDate.set(Calendar.MINUTE, 59);
        endDate.set(Calendar.SECOND, 59);
        endDate.set(Calendar.MILLISECOND, 999);

        // Initialize Calender to check for valid dates
        Calendar calendarDate = Calendar.getInstance();
        CalendarConstraints.DateValidator dateValidator = new CalendarConstraints.DateValidator() {
            @Override
            public boolean isValid(long date) {
                // set Calender to date
                calendarDate.setTimeInMillis(date);
                // Valid date check
                return validLookup.get(new SensorDate(
                        calendarDate.get(Calendar.YEAR),
                        calendarDate.get(Calendar.MONTH) + 1,
                        calendarDate.get(Calendar.DAY_OF_MONTH))
                ) != null;
            }
            @Override
            public int describeContents() { return 0; }
            @Override
            public void writeToParcel(@NonNull Parcel dest, int flags) {}
        };

        Log.d("Calendar", "buildCalender: " + endDate.get(Calendar.MONTH));

        // Build Constraint
        calendarConstraintsBuilder
                .setOpenAt(endDate.getTimeInMillis())
                .setStart(startDate.getTimeInMillis())
                .setEnd(endDate.getTimeInMillis())
                .setValidator(dateValidator);
        calendarConstraints = calendarConstraintsBuilder.build();

        // Build Calendar
        datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select a Date")
                .setSelection(endDate.getTimeInMillis())
                .setCalendarConstraints(calendarConstraints)
                .setInputMode(MaterialDatePicker.INPUT_MODE_CALENDAR)
                .build();

        datePicker.addOnPositiveButtonClickListener(
                selection -> { // setting date without changing text
                    selectedDate = datePicker.getSelection();
                    dateButton.setText(datePicker.getHeaderText());
                    if (graphTime.getCheckedRadioButtonId() == R.id.dayBtn) {
                        setGraph(true);
                    } else if (selectedHour != null) {
                        setGraph(false);
                    }
                }
        );

        datePicker.addOnDismissListener(dialog -> dateButton.setEnabled(true));

        // Set Calendar Button
        dateButton.setOnClickListener(v -> {
            dateButton.setEnabled(false);
            datePicker.show(fragmentManager, "MATERIAL_DATE_PICKER");
        });

    }

    protected void buildClock() {
        Calendar currentTime = Calendar.getInstance();
        timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(currentTime.get(Calendar.HOUR_OF_DAY))
                .setMinute(currentTime.get(Calendar.MINUTE))
                .setTitleText("Select Clock time")
                .setInputMode(MaterialTimePicker.INPUT_MODE_CLOCK)
                .build();

        timePicker.addOnPositiveButtonClickListener(
                v -> {
                    selectedHour = timePicker.getHour();
                    clockButton.setText(selectedHour.toString());
                    if (selectedDate != null) {
                        setGraph(false);
                    }
                }
        );

        timePicker.addOnDismissListener(dialog -> clockButton.setEnabled(true));

        clockButton.setOnClickListener(v -> {
            clockButton.setEnabled(false);
            timePicker.show(fragmentManager, "MATERIAL_TIME_PICKER");
        });
    }

    Float getHourAveraged(List<SensorData> sensorData) {
        Float avg = 0.0f;
        for (SensorData data :
                sensorData) {
            avg += data.getData();
        }
        Log.d("TAG", "getDayDataList: Avg Value " + (avg / sensorData.size()));

        return (avg/sensorData.size());
    }

    ArrayList<Float> getDayDataList(ArrayList<SensorData> sensorData) {
        ArrayList<Float> percentageValues = new ArrayList<>();
        if (sensorData.size() == 0) return percentageValues;
        // Add Blanks for Missing time
        for (int i = 0; i < sensorData.get(0).getHour(); i++) {
            percentageValues.add(null);
        }
        // Calculate and Add Average Hour Data
        int startingIndex = 0;
        int hour = sensorData.get(0).getHour();
        SensorData data;
        int i;
        for (i = 0; i < sensorData.size(); i++) {
            data = sensorData.get(i);
            if (data.getHour() - hour > 0) {
                percentageValues.add(getHourAveraged(sensorData.subList(startingIndex, i)));
                startingIndex = i;
                for (;data.getHour() - hour > 1; hour++) {
                    percentageValues.add(null);
                }
                hour = data.getHour();
            }
        }
        percentageValues.add(getHourAveraged(sensorData.subList(startingIndex, i)));
        return percentageValues;
    }

    ArrayList<Float> getHourDataList(ArrayList<SensorData> sensorData) {
        ArrayList<Float> percentageValues = new ArrayList<>();
        if (sensorData.size() == 0) return percentageValues;
        // Add Minute Data
        SensorData data = sensorData.get(0);
        int minute = 0;
        for (int i = 0; i < sensorData.size(); i++) {
            data = sensorData.get(i);
            if (data.getMinute() - minute > 0) {
                // Add blanks for missing minutes
                for (;data.getMinute() - minute > 1; minute++) {
                    percentageValues.add(null);
                }
                percentageValues.add(data.getData());
                minute = data.getMinute();
            }
        }
        percentageValues.add(data.getData());
        return percentageValues;
    }


    void setGraph(boolean daySelect) {
        if (selectedDate == null) return;
        Calendar selection = Calendar.getInstance();
        selection.setTimeInMillis(selectedDate);

        ArrayList<SensorData> sensorData;
        ArrayList<Float> data;

        if (daySelect) {
            sensorData = new ArrayList<>(
                    databaseService.getDayData(
                            selection.get(Calendar.YEAR),
                            selection.get(Calendar.MONTH) + 1,
                            selection.get(Calendar.DAY_OF_MONTH)
                    )
            );
            data = getDayDataList(sensorData);
        }
        else {
            sensorData = new ArrayList<>(
                    databaseService.getHourData(
                            selection.get(Calendar.YEAR),
                            selection.get(Calendar.MONTH) + 1,
                            selection.get(Calendar.DAY_OF_MONTH),
                            selectedHour
                    )
            );
            data = getHourDataList(sensorData);
        }
        waterGraphView.setYAxisValues(data);
    }

    public void setNetworkServiceObject(NetworkClientService networkClientService) {
        this.networkClientService = networkClientService;
    }
}

