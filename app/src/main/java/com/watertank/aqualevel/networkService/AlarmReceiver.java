package com.watertank.aqualevel.networkService;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.provider.Settings;
import android.util.Log;

public class AlarmReceiver extends BroadcastReceiver {
    private static MediaPlayer mediaPlayer;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (mediaPlayer == null) mediaPlayer = MediaPlayer.create(context, Settings.System.DEFAULT_ALARM_ALERT_URI);
        if (intent.getAction().equals("NETWORK_SERVICE_ALARM_START")) {
            mediaPlayer.setLooping(true);
            mediaPlayer.start();
            Log.d("ALARM", "onReceive: Started");
        } else if (intent.getAction().equals("NETWORK_SERVICE_ALARM_STOP")){
            mediaPlayer.stop();
            mediaPlayer.prepareAsync();
            Log.d("ALARM", "onReceive: Stopped");
        }
    }
}
