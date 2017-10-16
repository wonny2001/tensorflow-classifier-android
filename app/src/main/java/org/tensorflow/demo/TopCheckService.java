package org.tensorflow.demo;

import android.app.ActivityManager;
import android.app.Service;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;

import java.util.List;
import java.util.SortedMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

public class TopCheckService extends Service {
    private int mnMilliSecond = 1000;
    private int mnExitDelay = 1500;

    CountDownTimer timer;

    private int value = 0;

    public TopCheckService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
//            getTopActivity(getApplicationContext());
        int delay = mnExitDelay * mnMilliSecond;

        //타이머 설정
        timer = new CountDownTimer(delay, 1000) {
            @Override
            public void onFinish() {
                Log.e("TW", "onFinish()");
//                finish();
                stopSelf();
            }

            @Override
            public void onTick(long millisUntilFinished) {
                value++;
                Log.v("TW", Integer.toString(value));
//                Log.e("TW", "getTopActivity(getApplicationContext()) : "+getTopActivity(getApplication()));
            }
        };

        //타이머 시작
        timer.start();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}

