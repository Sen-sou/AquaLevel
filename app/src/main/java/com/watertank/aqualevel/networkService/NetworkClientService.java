package com.watertank.aqualevel.networkService;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkClientService extends Service {
//    private ArrayList<DataListener> dataListeners, directDataListeners;
    private HashMap<String, DataListener> dataListeners, directDataListeners;
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

    private int lastReadByte = 0;
    private int logCriticalSize = 10;
    private int allowLog = 0;
    private int dataGetInterval = 1;


    public class ClientServiceBinder extends Binder {
        public NetworkClientService getService() {
            return NetworkClientService.this;
        }
    }
    private final IBinder binder = new ClientServiceBinder();

    public NetworkClientService setDataListener(HashMap<String, DataListener> listeners, HashMap<String, DataListener> directListeners) {
        this.dataListeners = listeners;
        this.directDataListeners = directListeners;
        return NetworkClientService.this;
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

    @Override
    public void onCreate() {
        super.onCreate();
        receiverThread = Executors.newSingleThreadExecutor();
        senderThread = Executors.newSingleThreadExecutor();
        responseList = new ConcurrentLinkedQueue<>();
        requestList = new ConcurrentLinkedQueue<>();

        // Access Variables
        SharedPreferences preferences = getApplicationContext().getSharedPreferences("Aqua_Client", Context.MODE_PRIVATE);
        lastReadByte = preferences.getInt("lastReadByte", 0);
        dataGetInterval = preferences.getInt("dataGetInterval", 1);
        allowLog = preferences.getBoolean("autoSyncState", false) ? 1 : 0;

        mainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                String received = (String) msg.obj;
                switch (msg.what) {
                    case 1:
//                        Log.d("NETWORK CLIENT SERVICE", "this ran in " + Thread.currentThread().getName());
                        post(() -> inform());
                        break;
                    case 2:
                        Log.d("NETWORK CLIENT SERVICE", "Direct Informed " + received);
                        post(() -> directInform(received));
                        break;
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        senderThread.submit(this::connectToServer);
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        shutDownThread(receiverThread);
        shutDownThread(senderThread);
        if (connected) {
            try {
                serverSocket.close();
                in.close();
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Log.d("In Destroy", "In here");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void shutDownThread(@NonNull ExecutorService executorService) {
        // problem here
        try {
            if (executorService.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                Log.d("EXECUTOR SERVICE", "shutDownThread: ShutDown");
                executorService.shutdown();
            } else executorService.shutdownNow();
        } catch (InterruptedException e) {
            Log.d("EXECUTOR SERVICE", "shutDownThread: Force Shutdown");
            executorService.shutdownNow();
            e.printStackTrace();
        }
    }

    private void readServer() {
        StringBuilder response = new StringBuilder(10000);
        while (connected) {
            try {
                response.append(in.readLine()).append("\n");
            } catch (IOException e) {
                if (e instanceof SocketTimeoutException) {
//                    Log.d("READ_SERVER", "Socket timeout exception occurred");
                    connected = false;
                }
            }
            if (response.toString().endsWith(">\n")) {
                responseList.add(response.toString());
                Log.d("CLIENT", "readServer: " + response);
                response.setLength(0);
                if (!processFlag) {
                    mainHandler.sendMessage(Message.obtain(mainHandler, 1));
                }
            }
        }
        senderThread.submit(this::connectToServer);
    }

    private void requestServer() {
        String request;
        while (connected) {
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
            if (dataListeners.containsKey(command)) dataListeners.get(command).listen(data);
        }
        processFlag = false;
    }
    private void directInform(String response) {
        if (response == null) return;
        String command, data;
        command = response.substring(response.indexOf("<") + 1, response.indexOf("/"));
        data = response.substring(response.indexOf("/") + 1, response.indexOf(">"));
        if (directDataListeners.containsKey(command)) directDataListeners.get(command).listen(data);
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
                }
                Log.d("CLIENT", "Couldn't Establish Server Communication Stream");
                e.printStackTrace();
            }
        }
        connecting = false;

        if (connected && streamConnection){
            Log.d("CLIENT", "Connected to the Serv`er"); // Check Server Validity
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
