package com.watertank.aqualevel.sensordataroom;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {SensorData.class}, version = 1)
public abstract class SensorDataDatabase extends RoomDatabase {
    public abstract SensorDataDao sensorDataDao();
}
