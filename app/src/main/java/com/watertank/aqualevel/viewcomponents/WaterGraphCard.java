package com.watertank.aqualevel.viewcomponents;

import android.content.Context;
import android.os.Parcel;
import android.view.View;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.CalendarConstraints;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.watertank.aqualevel.R;
import com.watertank.aqualevel.sensordataroom.SensorDataDatabase;

import java.util.Calendar;
import java.util.Date;

public class WaterGraphCard {
    private final Context context;
    private final View card;
    private final FragmentManager fragmentManager;

    private MaterialButton dateButton, clockButton;
    CalendarConstraints calendarConstraints;
    CalendarConstraints.DateValidator dateValidator;
    CalendarConstraints.Builder calendarConstraintsBuilder;
    Calendar startDate, endDate;
    private WaterGraphView waterGraphView;
    private final SensorDataDatabase database;

    public WaterGraphCard(Context context, FragmentManager fragmentManager, View card, SensorDataDatabase database) {
        this.context = context;
        this.fragmentManager = fragmentManager;
        this.card = card;
        this.database = database;
    }

    public void init() {
        dateButton = card.findViewById(R.id.dateButton);
        clockButton = card.findViewById(R.id.clockButton);
        waterGraphView = card.findViewById(R.id.waterGraph);
        RadioGroup graphTime = card.findViewById(R.id.graphTimeRadio);

        clockButton.setVisibility(View.GONE);

        calendarConstraintsBuilder = new CalendarConstraints.Builder();
        startDate = Calendar.getInstance();
        endDate = Calendar.getInstance();
        buildCalender(new Date(), new Date());
        buildClock();

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

    protected void buildCalender(Date start, Date end) {
        startDate.setTime(start);
        startDate.set(Calendar.HOUR_OF_DAY, 0);
        startDate.set(Calendar.MINUTE, 0);
        startDate.set(Calendar.SECOND, 0);
        startDate.set(Calendar.MILLISECOND, 0);

        endDate.setTime(end);
        endDate.set(Calendar.HOUR_OF_DAY, 23);
        endDate.set(Calendar.MINUTE, 59);
        endDate.set(Calendar.SECOND, 59);
        endDate.set(Calendar.MILLISECOND, 999);

//        dateValidator = DateValidatorPointBackward.before(endDate.getTimeInMillis());

        dateValidator = new CalendarConstraints.DateValidator() {
            @Override
            public boolean isValid(long date) {
                return date >= startDate.getTimeInMillis() && date <= endDate.getTimeInMillis();
            }
            @Override
            public int describeContents() { return 0; }
            @Override
            public void writeToParcel(@NonNull Parcel dest, int flags) {}
        };

        calendarConstraintsBuilder
                .setOpenAt(endDate.getTimeInMillis())
                .setStart(startDate.getTimeInMillis())
                .setEnd(endDate.getTimeInMillis())
                .setValidator(dateValidator);
        calendarConstraints = calendarConstraintsBuilder.build();

        MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                .setTitleText("Select a Date")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .setCalendarConstraints(calendarConstraints)
                .build();
        datePicker.addOnPositiveButtonClickListener(
                selection -> dateButton.setText(datePicker.getHeaderText())
        );

        dateButton.setOnClickListener(v -> datePicker.show(fragmentManager, "MATERIAL_DATE_PICKER"));
    }

    protected void buildClock() {
        MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_12H)
                .setHour(12)
                .setMinute(0)
                .setTitleText("Select Clock time")
                .build();
        timePicker.addOnPositiveButtonClickListener(
                v -> clockButton.setText(((Integer)timePicker.getHour()).toString())
        );

        clockButton.setOnClickListener(v -> timePicker.show(fragmentManager, "MATERIAL_TIME_PICKER"));
    }

}

