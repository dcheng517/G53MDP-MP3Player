package com.example.mp3player;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.io.File;


public class MP3Service extends Service {

    MP3Player Mp3;
    Intent noti;
    NotificationManager notiMan;
    NotificationCompat.Builder notiBuild;

    private mp3Binder binder = new mp3Binder();

    private static final String TAG = "Mp3Log";
    private boolean isBound = true;

    public class mp3Binder extends Binder
    {
        public void load(String filePath)
        {
            Mp3.load(filePath);
            updateNoti();
            Log.i(TAG, filePath);
        }

        public void play()
        {
            Mp3.play();
            updateNoti();
            Log.i(TAG, "play");
        }

        public void pause()
        {
            Mp3.pause();
            updateNoti();
            Log.i(TAG, "pause");
        }

        public void stop()
        {
            Mp3.stop();
            updateNoti();
            Log.i(TAG, "stop");
        }

        public int next(File[] list, int idx)
        {
            int allSongs = list.length;
            int ret = 0;

            // if current song is last song
            if((idx+1) > (allSongs-1))
                ret = 0;
            else
                ret = idx+1;

            return ret;
        }

        public int prev(File[] list, int idx)
        {
            int allSongs = list.length; //3, idx = 2
            int ret = 0;

            // if current song is first song
            if((idx-1) < 0)
                ret = allSongs - 1;
            else
                ret = idx-1;

            return ret;
        }

        public int getDuration() {
            return Mp3.getDuration();
        }

        public int getProgress() {
            return Mp3.getProgress();
        }

        public MP3Player.MP3PlayerState getState() {
            return Mp3.getState();
        }

//        seek to time
        public void seek(int sec) {
            Mp3.seek(sec);
        }
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
        final IntentFilter filter = new IntentFilter();
        Mp3 = new MP3Player();

        notiBuilder();

    }

//    Notification method
    public void notiBuilder()
    {
        noti = new Intent(this, MainActivity.class);
        notiMan = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);

//        Notification tap to open activity
        Intent mainActivity = new Intent(this, MainActivity.class);
        PendingIntent getActivity = PendingIntent.getActivity(this, 0, mainActivity, 0);

//        Intent to kill service
        Intent closeIntent = new Intent(this, stopReceiver.class);
        PendingIntent closePendingIntent = PendingIntent.getBroadcast(this, 0, closeIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            notiBuild = new NotificationCompat.Builder(this)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setSmallIcon(R.mipmap.ic_launcher_round)
                    .setContentTitle("MP3Player")
                    .setContentText(binder.getState().toString())
                    .setContentIntent(getActivity);

//            add kill service action button on notification if MainActivity is destroyed
        if(!isBound)
            notiBuild.addAction(R.drawable.close, getString(R.string.close), closePendingIntent);

        notiMan.notify(1, notiBuild.build());
        startForeground(1, notiBuild.build()); // notification can't be dismissed
    }

    // update current status of media player
    private void updateNoti()
    {
        String text = binder.getState().toString();
        notiBuild.setContentText(text);
        notiMan.notify(1, notiBuild.build());
        startForeground(1, notiBuild.build()); // notification can't be dismissed
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.i(TAG, "Service onBind");
        isBound = true;
        notiBuilder();
        return binder;
    }

    @Override
    public boolean onUnbind(Intent intent)
    {
        Log.i(TAG, "onUnbind");
        super.onDestroy();
        isBound = false;
        notiBuilder();
        return true;
    }

    @Override
    public void onRebind(Intent intent)
    {
        Log.i(TAG, "onRebind");
        super.onRebind(intent);
        isBound = true;
        notiBuilder();
    }

    @Override
    public void onDestroy()
    {
        Log.i(TAG, "Service onDestroy");
    }


}

