package com.beloushkin.android.learn.boundservicetest;


import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.Looper;

public class BeepHelper {

    private final ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, 100);

    public void beep(int duration) {

        toneG.startTone(ToneGenerator.TONE_DTMF_S, duration);
        final Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                toneG.release();
            }
        }, Long.valueOf(duration + 50));
    }
}
