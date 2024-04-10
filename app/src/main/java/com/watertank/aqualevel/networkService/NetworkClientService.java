package com.watertank.aqualevel.networkService;

import static android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.ServiceCompat;

import com.watertank.aqualevel.MainActivity;
import com.watertank.aqualevel.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkClientService extends Service {

    private static final String CHANNEL_NAME = "Level Status Notification";
    private static final String CHANNEL_ID = "Aqua.notification";
    private static final int NOTIFICATION_ID = 1001;
    private static final int NOTIFICATION_ALERT_ID = 1002;
    public static final String START = "NETWORK_SERVICE_START";
    public static final String STOP = "NETWORK_SERVICE_STOP";
    public static final String RELINK = "NETWORK_SERVICE_RELINK";
    public static final String ALARM_START = "NETWORK_SERVICE_ALARM_START";
    public static final String ALARM_STOP = "NETWORK_SERVICE_ALARM_STOP";


    private DataListener dataListeners, directDataListeners;
    private ConcurrentLinkedQueue<String> responseList, requestList;
    private ExecutorService receiverThread, senderThread;
    private Handler mainHandler;

    // Network Resources
    private String serverAddress = "192.168.0.120";
    public int serverPort = 80;
    private Socket serverSocket;
    private BufferedReader in;
    private PrintWriter out;
    private volatile boolean connected = false;
    private boolean connecting = false;
    private int connectionTimeout = 15000;
    private volatile boolean processFlag = false;
    private volatile boolean stopFlag = false;

    // Client Access Variables
    private int lastReadByte = 0;
    private int logCriticalSize = 10;
    private int allowLog = 0;
    private int dataGetInterval = 1;

    private float safeMin;
    private float safeMax;

    // Notification Resources
    private NotificationManager notificationManager;
    private Notification statusNotification;
    private Notification alertNotification;
    private boolean alertState;
    private AlarmManager alarmManager;
    private boolean alarmState;
    private boolean alarmTriggerState = false;
    private PendingIntent alarmIntent;
    private RemoteViews notificationLayout, notificationLayoutExpanded, alertLayout;
    public static boolean mainAlive = true;



    public class ClientServiceBinder extends Binder {
        public NetworkClientService getService() {
            return NetworkClientService.this;
        }
    }
    private final IBinder binder = new ClientServiceBinder();

    public NetworkClientService setDataListener(DataListener listeners, DataListener directListeners) {
        listeners.addAll(this.dataListeners);
        this.dataListeners = listeners;
        directListeners.addAll(this.directDataListeners);
        this.directDataListeners = directListeners;
        return NetworkClientService.this;
    }

    public void setSafeMinMax(float min, float max) {
        this.safeMin = min;
        this.safeMax = max;
    }

    public List<Float> getSafeMinMax() {
        return Arrays.asList(safeMin, safeMax);
    }

    public void setAlertState(boolean state) {
        this.alertState = state;
    }

    public void setAlarmState(boolean state) {
        this.alarmState = state;
    }

    public NetworkClientService setConnectionTimeout(int timeout) {
        connectionTimeout = timeout;
        return NetworkClientService.this;
    }

    public NetworkClientService setSocketConnection(String address, int port) {
        serverAddress = address;
        serverPort = port;
        return NetworkClientService.this;
    }

    public boolean getConnectionStatus() {
        return connected;
    }

    public void retryConnect() {
        if (connected || connecting) return;
        senderThread.submit(this::connectToServer);
    }

    public void addRequest(String request) {
        requestList.add(request);
    }

    public void setSocketTimeout(int timeout) {
        if (!connected) return;
        this.connectionTimeout = timeout;
        try {
            serverSocket.setSoTimeout(connectionTimeout);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendInMessage(String message) {
        mainHandler.sendMessage(Message.obtain(mainHandler, 2, message));
    }



    // Service Methods

    @Override
    public void onCreate() {
        super.onCreate();
        receiverThread = Executors.newSingleThreadExecutor();
        senderThread = Executors.newSingleThreadExecutor();
        responseList = new ConcurrentLinkedQueue<>();
        requestList = new ConcurrentLinkedQueue<>();
        dataListeners = new DataListener();
        directDataListeners = new DataListener();

        // Access Variables
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("Aqua_Client", Context.MODE_PRIVATE);
        lastReadByte = preferences.getInt("lastReadByte", 0);
        dataGetInterval = preferences.getInt("dataGetInterval", 1);
        allowLog = preferences.getBoolean("autoSyncState", false) ? 1 : 0;
        safeMin = preferences.getFloat("safeLevelMin", 5.0f);
        safeMax = preferences.getFloat("safeLevelMax", 90.0f);
        alertState = preferences.getBoolean("notifyState", false);
        alarmState = preferences.getBoolean("alarmNotifyState", false);

        mainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                String received = (String) msg.obj;
                switch (msg.what) {
                    case 1:
                        post(() -> inform());
                        break;
                    case 2:
                        Log.d("NETWORK CLIENT SERVICE", "Direct Informed " + received);
                        post(() -> directInform(received));
                        break;
                }
            }
        };

        directDataListeners.add(
                "connectionStatus",
                received -> updateNotificationConnectedStatus(Integer.parseInt(received) == 2)
        );

        dataListeners.add("sensorRead",
                received -> {
                    float level = (81.0f - Float.parseFloat(received)) * 1.2345679f;
                    updateNotificationPercentage(level);
                    if (alertState) updateAlert(level);
                }
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d("NETWORK_SERVICE", "onStartCommand: Command: " + intent.getAction());

        if (intent.getAction().equals(START)) {
            startService();
            senderThread.submit(this::connectToServer);
        } else if (intent.getAction().equals(STOP) && !mainAlive) {
            stopService();
        }
        else if (intent.getAction().equals(RELINK)) {
            if (connecting) mainHandler.sendMessage(Message.obtain(mainHandler, 2, "<connectionStatus/1>"));
            else if (connected) mainHandler.sendMessage(Message.obtain(mainHandler, 2, "<connectionStatus/2>"));
            else mainHandler.sendMessage(Message.obtain(mainHandler, 2, "<connectionStatus/0>"));
            senderThread.submit(this::requestServer);
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startService() {

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
        );
        notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);

        notificationLayout = new RemoteViews(getPackageName(), R.layout.notification_small);
        notificationLayoutExpanded = new RemoteViews(getPackageName(), R.layout.notification_large);
        alertLayout = new RemoteViews(getPackageName(), R.layout.notification_alert);

        notificationLayout.setViewVisibility(R.id.notification_small_alert, View.GONE);
        notificationLayoutExpanded.setViewVisibility(R.id.notification_large_alert, View.GONE);

        setupViewIntents();

        Notification.Action dummyAction = new Notification.Action.Builder(
                R.drawable.aqua_notification, "",null).build();

        // Alarm
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmIntent = PendingIntent.getBroadcast(
                this,
                202,
                new Intent(this, AlarmReceiver.class)
                        .setAction(ALARM_START),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        Notification.Action dismissAction = new Notification.Action.Builder(
                R.drawable.aqua_notification,
                "DISMISS",
                PendingIntent.getBroadcast(
                        this,
                        201,
                        new Intent(this, AlarmReceiver.class)
                                .setAction(ALARM_STOP),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                )
        ).build();

        statusNotification = new Notification.Builder(this, CHANNEL_ID)
                .setOngoing(true)
                .setCustomContentView(notificationLayout)
                .setCustomBigContentView(notificationLayoutExpanded)
                .setSmallIcon(R.drawable.aqua_notification)
                .addAction(dummyAction)
                .setColor(Color.argb(255, 1, 135, 134))
                .setOnlyAlertOnce(true)
                .build();

        alertNotification = new Notification.Builder(this, CHANNEL_ID)
                .setAutoCancel(true)
                .setOnlyAlertOnce(true)
                .setCustomContentView(alertLayout)
                .setCustomBigContentView(alertLayout)
                .setCustomHeadsUpContentView(alertLayout)
                .setSmallIcon(R.drawable.aqua_notification)
                .setColor(Color.argb(255, 1, 135, 134))
                .setContentIntent(PendingIntent.getBroadcast(
                        this,
                        201,
                        new Intent(this, AlarmReceiver.class)
                                .setAction(ALARM_STOP),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                ))
                .build();

        startForeground(NOTIFICATION_ID, statusNotification, FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        Log.d("NETWORK_SERVICE", "onStartCommand ");
    }

    private void stopService() {
        Log.d("NETWORK_SERVICE", "stopService: ");
        stopForeground(STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    private void setupViewIntents() {

        notificationLayout.setOnClickPendingIntent(
                R.id.notification_small_settings,
                PendingIntent.getActivity(
                        this,
                        102,
                        new Intent(this, MainActivity.class)
                                .setAction(Intent.ACTION_MAIN)
                                .addCategory(Intent.CATEGORY_LAUNCHER),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                )
        );

        notificationLayout.setOnClickPendingIntent(
                R.id.notification_small_stop_button,
                PendingIntent.getService(
                        this,
                        101,
                        new Intent(this, NetworkClientService.class)
                                .setAction(STOP),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                )
        );

        notificationLayoutExpanded.setOnClickPendingIntent(
                R.id.notification_large_settings,
                PendingIntent.getActivity(
                        this,
                        102,
                        new Intent(this, MainActivity.class)
                                .setAction(Intent.ACTION_MAIN)
                                .addCategory(Intent.CATEGORY_LAUNCHER),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                )
        );

        notificationLayoutExpanded.setOnClickPendingIntent(
                R.id.notification_large_stop_button,
                PendingIntent.getService(
                        this,
                        101,
                        new Intent(this, NetworkClientService.class)
                                .setAction(STOP),
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                )
        );

    }

    private void updateNotificationPercentage(float percentage) {
        notificationLayout.setTextViewText(
                R.id.notification_small_percentage,
                String.format(Locale.getDefault(),
                        "%.1f%%",
                        percentage
                )
        );
        notificationLayoutExpanded.setTextViewText(
                R.id.notification_large_percentage,
                String.format(Locale.getDefault(),
                        "%.1f%%",
                        percentage
                )
        );
        notificationManager.notify(NOTIFICATION_ID, statusNotification);
    }

    private void updateNotificationConnectedStatus(boolean status) {
        if (status) {
            notificationLayout.setImageViewResource(
                    R.id.notification_small_connected, R.drawable.connected_service);
            notificationLayoutExpanded.setImageViewResource(
                    R.id.notification_large_connected, R.drawable.connected_service);
        }
        else {
            notificationLayout.setImageViewResource(
                    R.id.notification_small_connected, R.drawable.disconnected_service);
            notificationLayoutExpanded.setImageViewResource(
                    R.id.notification_large_connected, R.drawable.disconnected_service);
        }
        notificationManager.notify(NOTIFICATION_ID, statusNotification);
    }

    private void updateAlert(float level) {
        if (level < safeMin) {
            alertLayout.setTextViewText(R.id.notification_alert_content, "The Water is Below Safe Level");
        } else if (level > safeMax) {
            alertLayout.setTextViewText(R.id.notification_alert_content, "The Water is Above Safe Level");
        } else {
            alarmTriggerState = false;
            return;
        }
        notificationManager.notify(NOTIFICATION_ALERT_ID, alertNotification);
        if (alarmState && !alarmTriggerState) {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis() + 500,
                    alarmIntent
            );
            alarmTriggerState = true;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dataListeners.clear();
        directDataListeners.clear();
        if (connected) {
            try {
                stopFlag = true;
                serverSocket.close();
                in.close();
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        shutDownThread(receiverThread);
        if (connecting) senderThread.shutdownNow();
        else shutDownThread(senderThread);
        Log.d("NETWORK_SERVICE", "onDestroy: ");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }




    // Network Communication

    private void shutDownThread(@NonNull ExecutorService executorService) {
        try {
            executorService.shutdown();
            if (!executorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            e.printStackTrace();
        }
    }

    private void readServer() {
        StringBuilder response = new StringBuilder(10000);
        while (connected && !stopFlag) {
            try {
                response.append(in.readLine()).append("\n");
            } catch (IOException e) {
                if (e instanceof SocketTimeoutException) {
                    Log.d("READ_SERVER", "Socket timeout exception occurred");
                }
                connected = false;
            }
            if (response.toString().endsWith(">\n")) {
                responseList.add(response.toString());
//                Log.d("CLIENT", "readServer: " + response);
                response.setLength(0);
                if (!processFlag) {
                    mainHandler.sendMessage(Message.obtain(mainHandler, 1));
                }
            }
        }
        if (stopFlag) return;
        senderThread.submit(this::connectToServer);
    }

    private void requestServer() {
        String request;
        while (connected && mainAlive) {
            while (requestList.size() > 0) {
                request = requestList.poll();
                out.println(request);
            }
        }
    }

    private void inform() {
        processFlag = responseList.size() != 0 && dataListeners.size() != 0;
        String response, command, data;
        while (responseList.size() > 0) {
            response = responseList.poll();
            if (response == null) continue;
            // Parse Response
            command = response.substring(response.indexOf("<") + 1, response.indexOf("/"));
            data = response.substring(response.indexOf("/") + 1, response.indexOf(">"));
            if (dataListeners.containsKey(command)) {
                for (DataListener.Listener listener: dataListeners.getListeners(command)) {
                    listener.listen(data);
                }
            };
        }
        processFlag = false;
    }
    private void directInform(String response) {
        if (response == null) return;
        String command, data;
        command = response.substring(response.indexOf("<") + 1, response.indexOf("/"));
        data = response.substring(response.indexOf("/") + 1, response.indexOf(">"));
        if (directDataListeners.containsKey(command)) {
            for (DataListener.Listener listener: directDataListeners.getListeners(command)) {
                listener.listen(data);
            }
        };
    }

    private void connectToServer() { // Add retry button in UI | 
        int sleepTime = 1000;
        boolean streamConnection = false;
        connecting = true;

        mainHandler.sendMessage(Message.obtain(mainHandler, 2, "<connectionStatus/1>"));

        // Check for previous Socket Connection
        if (serverSocket != null) {
            try {
                serverSocket.close();
                in.close();
                out.close();
                connected = false;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        // Connection Try Loop
        while (!connected || !streamConnection) {
            // Establish Connection to Server
            if (!connected) {
                try {
                    serverSocket = new Socket(serverAddress, serverPort);
                    serverSocket.setSoTimeout(connectionTimeout);
                    connected = true;
                    sleepTime = 1000;
                } catch (IOException e) {
                    try {
                        Thread.sleep(sleepTime);
                        if (sleepTime <= 1500) sleepTime += 100;
                        else break;
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                        return;
                    }
                    Log.d("CLIENT", "Trouble Connecting to Server.... Retrying:");
                    continue;
                }
            }
            // Create Communication Stream
            try {
                if (serverSocket == null) break;
                in = new BufferedReader(new InputStreamReader(serverSocket.getInputStream()));
                out = new PrintWriter(serverSocket.getOutputStream(), true);
                senderThread.submit(this::requestServer);
                receiverThread.submit(this::readServer);
                streamConnection = true;
            } catch (IOException e) {
                streamConnection = false;
                try {
                    Thread.sleep(sleepTime);
                    if (sleepTime <= 1500) sleepTime += 100;
                    else break;
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                    return;
                }
                Log.d("CLIENT", "Couldn't Establish Server Communication Stream");
                e.printStackTrace();
            }
        }
        connecting = false;

        if (connected && streamConnection){
            Log.d("CLIENT", "Connected to the Server"); // Check Server Validity
            addRequest("<setConfig/"
                    + "dataGetInterval:" + dataGetInterval
                    + ":allowLog:" + allowLog
                    + ":logCriticalSize:" + logCriticalSize
                    + ":lastReadByte:" + lastReadByte
                    + ">");
            mainHandler.sendMessage(Message.obtain(mainHandler, 2, "<connectionStatus/2>"));
        } else {
            Log.d("CLIENT", "Couldn't Connect to Server");
            mainHandler.sendMessage(Message.obtain(mainHandler, 2, "<connectionStatus/0>"));
        }
    }
}
