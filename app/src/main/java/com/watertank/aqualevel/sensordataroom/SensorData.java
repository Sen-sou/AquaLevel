package com.watertank.aqualevel.sensordataroom;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class SensorData {
    @PrimaryKey(autoGenerate = true)
    public int uid;

    @ColumnInfo(name = "minute")
    public int minute;

    @ColumnInfo(name = "hour")
    public int hour;

    @ColumnInfo(name = "day")
    public int day;

    @ColumnInfo(name = "month")
    public int month;

    @ColumnInfo(name = "year")
    public int year;

    @ColumnInfo(name = "data")
    public float data;

    public SensorData() {
        this.minute = 0;
        this.hour = 0;
        this.day = 0;
        this.month = 0;
        this.year = 0;
        this.data = 0.0f;
    }
    public SensorData(int minute, int hour, int day, int month, int year, float data) {
        this.minute = minute;
        this.hour = hour;
        this.day = day;
        this.month = month;
        this.year = year;
        this.data = data;
    }

    public int getUid() {
        return uid;
    }

    public int getMinute() {
        return minute;
    }

    public int getHour() {
        return hour;
    }

    public int getDay() {
        return day;
    }

    public int getMonth() {
        return month;
    }

    public int getYear() {
        return year;
    }

    public float getData() {
        return data;
    }

    public void setMinute(int minute) {
        this.minute = minute;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setData(float data) {
        this.data = data;
    }
}
