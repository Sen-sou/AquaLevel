package com.watertank.aqualevel.sensordataroom;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SensorDataDao {
    @Query("SELECT * FROM sensordata")
    List<SensorData> getAll();

    @Query("SELECT * FROM SensorData WHERE year = :startYear AND month = :startMonth AND day = :startDay ")
    List<SensorData> getDayData(int startYear, int startMonth, int startDay);

    @Query("SELECT * FROM SensorData WHERE year = :startYear AND month = :startMonth AND day = :startDay " +
            "AND hour = :startHour")
    List<SensorData> getHourData(int startYear, int startMonth, int startDay, int startHour);

    @Insert
    void insert(SensorData data);

    @Insert
    void insertAll(List<SensorData> dataList);

    @Delete
    void delete(SensorData data);
}