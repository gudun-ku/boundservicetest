package com.beloushkin.android.learn.boundservicetest;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = AppCompatActivity.class.getSimpleName();

    private Messenger mService;
    private boolean mIsBound = false;

    private TextView tvTime;
    private Button btnReset;
    final Messenger mMessenger = new Messenger(new IncomingEventHandler());


    class IncomingEventHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case TimeCountService.MSG_NEW_TIME:
                    tvTime.setText("Time is: " + msg.obj.toString());
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            mService = new Messenger(service);
            Log.d(TAG, "onServiceConnected: ");
            try {
                Message msg = Message.obtain(null, TimeCountService.MSG_REGISTER_CLIENT);
                msg.replyTo = mMessenger;
                mService.send(msg);
            }
            catch (RemoteException e) {
                // In this case the service has crashed before we could even do anything with it
            }
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been unexpectedly disconnected - process crashed.
            mService = null;
            Log.d(TAG, "onServiceDisconnected: ");
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvTime = findViewById(R.id.tv_time);
        btnReset = findViewById(R.id.btn_reset);

        startService(new Intent(MainActivity.this, TimeCountService.class));

    }

    @Override
    protected void onStart() {
        super.onStart();
        //bind to TimeCountService
        checkIfServiceIsRunning();
    }

    @Override
    protected void onStop() {
        super.onStop();
        doUnbindService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            doUnbindService();
        }
        catch (Throwable t) {
            Log.e("MainActivity", "Failed to unbind from the service", t);
        }
    }

    private void checkIfServiceIsRunning() {
        //If the service is running when the activity starts, we want to automatically bind to it.
        if (TimeCountService.isRunning()) {
            doBindService();
        }
    }

    void doBindService() {
        bindService(new Intent(this, TimeCountService.class), mConnection, Context.BIND_AUTO_CREATE);
        mIsBound = true;
        Log.d(TAG, "doBindService: ");
        
    }
    void doUnbindService() {
        if (mIsBound) {
            // If we have received the service, and hence registered with it, then now is the time to unregister.
            if (mService != null) {
                try {
                    Message msg = Message.obtain(null, TimeCountService.MSG_UNREGISTER_CLIENT);
                    msg.replyTo = mMessenger;
                    mService.send(msg);
                }
                catch (RemoteException e) {
                    // There is nothing special we need to do if the service has crashed.
                }
            }
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
            Log.d(TAG, "doUnbindService: ");
        }
    }
}
