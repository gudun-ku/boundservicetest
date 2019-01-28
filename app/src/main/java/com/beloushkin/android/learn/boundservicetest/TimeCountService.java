package com.beloushkin.android.learn.boundservicetest;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

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
    static final int MSG_SET_RATE = 3;
    private Messenger mClient;
    private static boolean isRunning = false;

    final Messenger mMessenger = new Messenger(new IncomingHandler());

    private ScheduledExecutorService mScheduledExecutorService;

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
                case MSG_SET_RATE:
                    mInterval = msg.arg1;
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

    @Override
    public IBinder onBind(Intent intent) {
       return mMessenger.getBinder();
    }

    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate: ");
        mScheduledExecutorService = Executors.newScheduledThreadPool(1);
        isRunning = true;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        mScheduledExecutorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                long currTime = System.currentTimeMillis();
                Log.d(TAG, "run: " + currTime);
                sendMessageToUI(currTime);

            }
        }, 1000, 1000, TimeUnit.MILLISECONDS);

        return START_STICKY;
    }


    @Override
    public void onDestroy() {
        mScheduledExecutorService.shutdownNow();
        Log.d(TAG, "onDestroy: ");
        isRunning = false;
    }
}
