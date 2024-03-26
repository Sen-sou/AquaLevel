package com.watertank.aqualevel.networkService;

import android.app.Service;
import android.content.Intent;
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
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkClientService extends Service {
    private ArrayList<DataListener> dataListeners;
    private ArrayList<DataListener> directDataListeners;
    private ConcurrentLinkedQueue<String> responseList;
    ConcurrentLinkedQueue<String> requestList;
    // create a task in the main thread which sends data to all the listeners
    // get message from the main thread to send that message to the server
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
    private int connectionTimeout = 10000;
    private boolean processFlag = false;


    public class ClientServiceBinder extends Binder {
        public NetworkClientService getService() {
            return NetworkClientService.this;
        }
    }
    private final IBinder binder = new ClientServiceBinder();

    public NetworkClientService setDataListener(ArrayList<DataListener> listeners, ArrayList<DataListener> directListeners) {
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

    @Override
    public void onCreate() {
        super.onCreate();
        receiverThread = Executors.newSingleThreadExecutor();
        senderThread = Executors.newSingleThreadExecutor();
        responseList = new ConcurrentLinkedQueue<>();
        requestList = new ConcurrentLinkedQueue<>();
        mainHandler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                String received = (String) msg.obj;
                switch (msg.what) {
                    case 1:
                        Log.d("NETWORK CLIENT SERVICE", "this ran");
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
        String response = "";
        Message msg = new Message();
        msg.what = 1;
        while (connected) {
            try {
                response = in.readLine();
            } catch (IOException e) {
                if (e instanceof SocketTimeoutException) {
                    Log.d("READ_SERVER", "Socket timeout exception occurred");
                    connected = false;
                }
            }
            if (response != null && !response.isEmpty()) { // join strings of same command and separate different ones
                responseList.add(response);
                processFlag = true;
//                Log.d("READ_SERVER", "Reading Data");
            } else if (processFlag && (responseList.size() > 0)) {
//                Log.d("READ_SERVER", "Send for Process");
                mainHandler.sendMessage(msg);
                processFlag = false;
                response = "";
            }
        }
        senderThread.submit(this::connectToServer);
    }

    private void requestServer() {
        Iterator<String> iterator;
        while (connected) {
            if (!requestList.isEmpty()) {
                iterator = requestList.iterator();
                while (iterator.hasNext()) {
                    out.println(iterator.next());
                    iterator.remove();
                }
            }
        }
    }

    private void inform() { // Direct Response(in message), From Response List()
        if (responseList.size() == 0 || dataListeners.size() == 0) return;
        Iterator<String> iterator = responseList.iterator();
        String response;
        while (iterator.hasNext()) {
            response = iterator.next(); // can use hashmap, with command as keys
            for (int i = 1; i < dataListeners.size(); i++) {
                dataListeners.get(i).listen(response);
            }
            iterator.remove();
        }
    }
    private void directInform(String msg) {
        for (int i = 0; i < directDataListeners.size(); i++) {
            directDataListeners.get(i).listen(msg);
        }
    }

    private void connectToServer() { // Add retry button in UI | 
        int sleepTime = 1000;
        boolean streamConnection = false;
        connecting = true;
        Message status1 = new Message();
        Message status2 = new Message();
        status1.what = 2;
        status2.what = 2;

        status1.obj = "connectionStatus/1";
        mainHandler.sendMessage(status1);

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
        if (connected && streamConnection){
            Log.d("CLIENT", "Connected to the Server"); // Check Server Validity
            status2.obj = "connectionStatus/2";
        } else {
            Log.d("CLIENT", "Couldn't Connect to Server");
            status2.obj = "connectionStatus/0";
        }

        mainHandler.sendMessageDelayed(status2, 500);
        connecting = false;
    }

}
