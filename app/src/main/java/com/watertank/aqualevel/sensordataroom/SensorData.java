package com.watertank.aqualevel.sensordataroom;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class SensorData {
    @PrimaryKey
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
    public int data;
}
