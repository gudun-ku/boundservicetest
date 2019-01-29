package com.beloushkin.android.learn.boundservicetest;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.media.SoundPool;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class TimeCountService extends Service {

    public static final String TAG = TimeCountService.class.getSimpleName();

    public static final String TIME = "TIME";
    public static final int MSG_NEW_TIME = 123212;
    private int mInterval = 1000; // default value for time interval.
    static final int MSG_REGISTER_CLIENT = 1;
    static final int MSG_UNREGISTER_CLIENT = 2;
    static final int MSG_SET_FREQ = 3;
    private Messenger mClient;
    private static boolean isRunning = false;

    // Messenger object
    final Messenger mMessenger = new Messenger(new IncomingHandler());
    private ScheduledExecutorService mScheduledExecutorService;


    // Sound pool
    private SoundPool soundPool;
    private HashMap<Integer, Integer> soundPoolMap;
    private final int soundID = 1;



    private void playSound() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        float curVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        float maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        float leftVolume = curVolume / maxVolume;
        float rightVolume = curVolume / maxVolume;
        int priority = 1;
        int no_loop = 0;
        float normal_playback_rate = 1f;
        soundPool.play(soundID, leftVolume, rightVolume, priority, no_loop, normal_playback_rate);
    }

    public void ringtone(){
       new BeepHelper().beep(50);
    }


    class IncomingHandler extends Handler { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_REGISTER_CLIENT:
                    mClient = msg.replyTo;
                    break;
                case MSG_UNREGISTER_CLIENT:
                    mClient = null;
                    break;
                case MSG_SET_FREQ:
                    mInterval = msg.arg1;
                    stopTicking();
                    startTicking();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void sendMessageToUI(Long valueToSend) {
        if (mClient != null) {
            try {
                // Send data as an Integer
                mClient.send(Message.obtain(null , MSG_NEW_TIME,0,0, valueToSend));
            }
            catch (RemoteException e) {
                // The client is dead. Remove it from the list; we are going through the list from back to front so this is safe to do inside the loop.
                mClient = null;
            }
        }
    }

    public TimeCountService() {

    }

    public static boolean isRunning() {
        return  isRunning;
    }

    void startTicking() {
        mScheduledExecutorService = Executors.newScheduledThreadPool(1);
        mScheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                long currTime = System.currentTimeMillis();
                Log.d(TAG, "run: " + currTime);
                ringtone();
                sendMessageToUI(currTime);

            }
        }, 1000, mInterval, TimeUnit.MILLISECONDS);
    }

    void stopTicking() {
        if(!mScheduledExecutorService.isShutdown())
            mScheduledExecutorService.shutdownNow();
    }

    @Override
    public IBinder onBind(Intent intent) {
        isRunning = true;
        startTicking();
        return mMessenger.getBinder();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        stopTicking();
        isRunning = false;
        return super.onUnbind(intent);
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
       stopTicking();
       Log.d(TAG, "onDestroy: ");
    }
}
