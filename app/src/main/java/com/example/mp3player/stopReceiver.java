package com.example.mp3player;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

// class for notification kill service button
public class stopReceiver extends BroadcastReceiver
{
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Intent service = new Intent(context, MP3Service.class);
        context.stopService(service);

    }
}