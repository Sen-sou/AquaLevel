package com.watertank.aqualevel.sensordataroom;

import android.content.Context;
import android.se.omapi.Session;
import android.util.Log;

import androidx.room.Room;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class DatabaseExecutorService {
    private static volatile DatabaseExecutorService INSTANCE;
    private static volatile ExecutorService executorService;
    private static volatile SensorDataDao dao;
    private ArrayList<SensorData> dataList;

    private DatabaseExecutorService(Context context) {
        executorService = Executors.newSingleThreadExecutor();
        dao = SensorDataDatabase.getInstance(context.getApplicationContext()).sensorDataDao();
        dataList = new ArrayList<>();
    }

    public static DatabaseExecutorService getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (DatabaseExecutorService.class) {
                if (INSTANCE == null) {
                    INSTANCE = new DatabaseExecutorService(context);
                }
            }
        }
        return INSTANCE;
    }


    private boolean runExceptionCheck(Future<Boolean> future) {
        try {
            return future.get(200, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            return false;
        }
    }

    private List<SensorData> runGetExceptionCheck(Future<List<SensorData>> future) {
        try {
            return future.get(200, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<SensorData> getAll() {
        Callable<List<SensorData>> getAllTask = () -> dao.getAll();
        return runGetExceptionCheck(executorService.submit(getAllTask));
    }

    public List<SensorDate> getDates() {
        Callable<List<SensorDate>> getDatesTask = () -> dao.getDates();
        Future<List<SensorDate>> future = executorService.submit(getDatesTask);
        try {
            return future.get(200, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<SensorData> getDayData(int startYear, int startMonth, int startDay) {
        Callable<List<SensorData>> getDayDataTask
                = () -> dao.getDayData(startYear, startMonth, startDay);
        return runGetExceptionCheck(executorService.submit(getDayDataTask));
    }

    public List<SensorData> getHourData(int startYear, int startMonth, int startDay, int startHour) {
        Callable<List<SensorData>> getHourDataTask
                = () -> dao.getHourData(startYear, startMonth, startDay, startHour);
        return runGetExceptionCheck(executorService.submit(getHourDataTask));
    }

    public boolean insert(SensorData sensorData) {
        Callable<Boolean> insertTask = () -> {
            dao.insert(sensorData);
            return true;
        };
        return runExceptionCheck(executorService.submit(insertTask));
    }

    public boolean saveDataHistory(String history) { // check for empty string
        Callable<Boolean> insertAllTask = () -> {
            Log.d("Database service", "saveDataHistory: This ran");
            dataList.clear();
            String[] historyData = history.split("\n");
            for (String data : historyData) {
                if (data.equalsIgnoreCase("mm:hh:dD:MM:YYYY:0000")) continue;
//                Log.d("Database Service", "saveDataHistory: " + data);
                String[] extract = data.split(":");
                dataList.add(new SensorData(
                        Integer.parseInt(extract[0]),
                        Integer.parseInt(extract[1]),
                        Integer.parseInt(extract[2]),
                        Integer.parseInt(extract[3]),
                        Integer.parseInt(extract[4]),
                        Float.parseFloat(extract[5])
                ));
            }
            dao.insertAll(dataList);
            return true;
        };
        return runExceptionCheck(executorService.submit(insertAllTask));
    }

    public boolean delete(SensorData sensorData) {
        Callable<Boolean> deleteTask = () -> {
            dao.delete(sensorData);
            return true;
        };
        return runExceptionCheck(executorService.submit(deleteTask));
    }

    public boolean deleteAll() {
        Callable<Boolean> deleteAllTask = () -> {
            dao.deleteAll();
            return true;
        };
        return runExceptionCheck(executorService.submit(deleteAllTask));
    }

    public boolean resetTable(Context context) {
        Callable<Boolean> resetTableTask = () -> {
            SensorDataDatabase.truncateTable(context, "SensorData");
            return true;
        };
        return runExceptionCheck(executorService.submit(resetTableTask));
    }

    public static void shutDownService() {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            e.printStackTrace();
        } finally {
            executorService = null;
            dao = null;
            INSTANCE = null;
            Log.d("MAIN_APP", "shutDownService: ");
        }
    }
}
