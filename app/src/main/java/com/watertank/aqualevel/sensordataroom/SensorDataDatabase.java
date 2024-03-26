package com.watertank.aqualevel.sensordataroom;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;

@Database(entities = {SensorData.class}, version = 1, exportSchema = false)
public abstract class SensorDataDatabase extends RoomDatabase {
    public abstract SensorDataDao sensorDataDao();
    private static volatile SensorDataDatabase INSTANCE;

    public static SensorDataDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (SensorDataDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    SensorDataDatabase.class,
                                    "SensorDatabase")
                                    .build();
                }
            }
        }
        return INSTANCE;
    }

    public static void truncateTable(Context context, String tablename) {
        SQLiteDatabase database = SQLiteDatabase.openOrCreateDatabase(
                context.getDatabasePath(INSTANCE.getOpenHelper().getDatabaseName()), null
        );

        if (database != null) {
            database.execSQL(String.format("DELETE FROM %s;", tablename));
            database.execSQL("UPDATE sqlite_sequence SET seq = 0 WHERE name = ?;", new String[]{tablename});
        }
    }

}
