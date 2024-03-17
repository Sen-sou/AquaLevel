package com.watertank.aqualevel.sensordataroom;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SensorDate {
    private int year;
    private int month;
    private int day;

    public SensorDate() {
        this.year = 0;
        this.month = 0;
        this.day = 0;
    }

    public SensorDate(@NonNull SensorDate date) {
        this.year = date.year;
        this.month = date.month;
        this.day = date.day;
    }

    public SensorDate(int year, int month, int day) {
        this.year = year;
        this.month = month;
        this.day = day;
    }

    @Override
    public int hashCode() {
        return (this.year + "" + this.month + "" + this.day).hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        SensorDate date = new SensorDate();
        try {
            date = (SensorDate) obj;
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (date.getYear() == this.year && date.getMonth() == this.month && date.getDay() == this.day) {
            return true;
        } else return false;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }
}
