package com.example.mp3player;

import android.Manifest;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    MP3Service.mp3Binder binder;
    private ServiceConnection mp3Conn = null;

    private ListView lv;

    private ImageButton playPause;

    private SeekBar progBar;

    private TextView start;
    private TextView end;

    private File musicDir;
    private File[] list;
    private File selectedFromList;

    private static final String TAG = "Mp3Log";
    private final int REQUEST_PERMISSION_READ_EXTERNAL_STORAGE = 1;

    private int currIdx;
    private int idx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//        Check if runtime permission enabled, if not request it
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE},
                    REQUEST_PERMISSION_READ_EXTERNAL_STORAGE);

        } else {
            Toast.makeText(MainActivity.this,
                    "Permission (already) Granted!",
                    Toast.LENGTH_SHORT).show();
        }

        // starting the service
        Intent mp3Service = new Intent(this, MP3Service.class);
        startService(mp3Service);

        playPause = (ImageButton) findViewById(R.id.playPause);

        progBar = (SeekBar) findViewById(R.id.progBar);

        start = (TextView) findViewById(R.id.start);
        end = (TextView) findViewById(R.id.end);

        // connect to the service && binder
        if (mp3Conn != null) unbindService(mp3Conn);
        mp3Conn = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                binder = (MP3Service.mp3Binder) service;

                if (String.valueOf(binder.getState()).equals("STOPPED"))
                    playPause.setImageResource(R.drawable.play);
                else if (String.valueOf(binder.getState()).equals("PLAYING") || String.valueOf(binder.getState()).equals("PAUSED"))
                {
                    progBar.setProgress(binder.getProgress());
                    progBar.setMax(binder.getDuration());
                    start.setText(convertTime(binder.getProgress()));
                    end.setText(convertTime(binder.getDuration()));
                    progBar.postDelayed(progUpdate, 1000);

                    if(String.valueOf(binder.getState()).equals("PLAYING"))
                        playPause.setImageResource(R.drawable.pause);
                    else if (String.valueOf(binder.getState()).equals("PAUSED"))
                        playPause.setImageResource(R.drawable.play);
                }

                Log.i(TAG, "Connected");
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {
                Log.i(TAG, "Disconnected");
            }
        };
        bindService(mp3Service, mp3Conn, Service.BIND_AUTO_CREATE);

        playListViewAdapter();

        progSeekBar();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_PERMISSION_READ_EXTERNAL_STORAGE:
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this, "Permission Granted!", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(MainActivity.this, "Permission Denied!", Toast.LENGTH_SHORT).show();
                }
        }
    }

//    List methods - loading songs
    private void playListViewAdapter()
    {
        lv = (ListView) findViewById(R.id.playList);

        musicDir = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Music//");
        list = musicDir.listFiles();
        lv.setAdapter(new ArrayAdapter<File>(this,android.R.layout.simple_list_item_1, list));
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener()
        {
            public void onItemClick(AdapterView<?> myAdapter, View myView, int myItemInt, long mylng)
            {
                selectedFromList =(File) (lv.getItemAtPosition(myItemInt));
                currIdx = myItemInt;

                binder.stop(); // stop player to not load multiple tracks
                binder.load(selectedFromList.getAbsolutePath());
                toastNoti();

                playPause.setImageResource(R.drawable.pause);

                start.setText(convertTime(0));
                end.setText(convertTime(binder.getDuration()));

                progBar.setProgress(0);
                progBar.setMax(binder.getDuration());
                progBar.postDelayed(progUpdate, 1000);
            }
        });
    }

//    Seekbar methods
    private void progSeekBar()
    {
        progBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                start.setText(convertTime(progress));
                if(fromUser) // only seek if user input
                {
                    binder.seek(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

//    Progress seekbar updater
    private Runnable progUpdate = new Runnable() {
        @Override
        public void run() {
            if(progBar != null)
            {
                if(binder.getDuration() != 0)
                {
                    progBar.setProgress(binder.getProgress());
                }

                if((String.valueOf(binder.getState()).equals("PLAYING")) || (String.valueOf(binder.getState()).equals("PAUSED")))
                {
                    progBar.postDelayed(progUpdate, 1000);
                }
            }
        }
    };

//    Convert ms to HH:MM:SS
    private String convertTime(int msec)
    {
        String finalStr = "";
        String secStr;

        int hrs = (int) (msec/(1000*60*60));
        int min = (int) (msec % (1000 * 60 * 60)) / (1000 * 60);
        int sec = (int) ((msec % (1000 * 60 * 60)) % (1000 * 60) / 1000);

        if (hrs > 0)
            finalStr = hrs + ":";

        secStr = String.format("%02d", sec); // show 2 numbers for single digits

        finalStr = finalStr + min + ":" + secStr;

        return finalStr;
    }

//    Get song name
    public String songName(String filePath)
    {
        int idxSlash = filePath.lastIndexOf("/"); // remove path name
        int idxDot = filePath.lastIndexOf("."); // remove file type

        return filePath.substring(idxSlash+1, idxDot);
    }

//    show toast notification of current song playing
    public void toastNoti()
    {
        String name = songName(list[currIdx].getAbsolutePath());
        Toast toast = Toast.makeText(this, "Playing " + name, Toast.LENGTH_SHORT );
        toast.show();
    }

//    Stop button
    public void onClickStop(View view)
    {
        binder.stop();
        playPause.setImageResource(R.drawable.play);
        progBar.setProgress(0);
        Toast toast = Toast.makeText(this, "Stopped", Toast.LENGTH_SHORT );
        toast.show();
    }

//    Play/Pause button
    public void onClickPlayPause(View view)
    {
        if (String.valueOf(binder.getState()).equals("PAUSED"))
        {
            binder.play();
            playPause.setImageResource(R.drawable.pause); // Change res of the button to pause

            toastNoti();

        }else if (String.valueOf(binder.getState()).equals("PLAYING"))
        {
            binder.pause();
            playPause.setImageResource(R.drawable.play); // Change res of the button to play

            Toast toast = Toast.makeText(this, "Paused", Toast.LENGTH_SHORT );
            toast.show();
        }
    }

//    Next button
    public void onClickNext(View view)
    {
        if(binder != null)
        {
            idx = binder.next(list, currIdx);

            binder.stop();
            binder.load(list[idx].getAbsolutePath());
            currIdx = idx;
            toastNoti();
        }
    }

//    Previous button
    public void onClickPrev(View view)
    {
        if(binder != null)
        {
            idx = binder.prev(list, currIdx);

            if(binder.getProgress() < 10000) // if song is <10seconds played, go prev song
            {
                binder.stop();
                binder.load(list[idx].getAbsolutePath());
                currIdx = idx;
                toastNoti();
            }
            else if(binder.getProgress() > 10000) // if song is > 10seconds in, restart track
            {
                binder.stop();
                Log.i(TAG, ""+binder.getProgress());
                binder.load(list[currIdx].getAbsolutePath());
            }
        }
    }

    @Override
    protected void onDestroy()
    {
        unbindService(mp3Conn);
        super.onDestroy();
        Log.i(TAG, "onDestroy");
    }

}
