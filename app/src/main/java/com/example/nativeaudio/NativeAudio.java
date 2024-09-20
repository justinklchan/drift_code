/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.nativeaudio;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.os.Build;
import android.os.Bundle;
//import android.support.annotation.NonNull;
//import android.support.v4.app.ActivityCompat;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.widget.NestedScrollView;

import java.util.Date;

public class NativeAudio extends AppCompatActivity {

    String[] perms = new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    //static final String TAG = "NativeAudio";
    private static final int AUDIO_ECHO_REQUEST = 0;

    static final int CLIP_NONE = 0;
    static final int CLIP_HELLO = 1;
    static final int CLIP_ANDROID = 2;
    static final int CLIP_SAWTOOTH = 3;
    static final int CLIP_PLAYBACK = 4;

    static String URI;
    static AssetManager assetManager;

    static boolean isPlayingAsset = false;
    static boolean isPlayingUri = false;

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    static Activity av;
    /** Called when the activity is first created. */
    @Override
    @TargetApi(17)
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);

        av = NativeAudio.this;
        ActivityCompat.requestPermissions(this,
                perms,
                1234);
        int minbuffersize = AudioRecord.getMinBufferSize(
                48000,
                AudioFormat.CHANNEL_IN_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        getSupportActionBar().hide();
        NativeAudio.createEngine();
        NativeAudio.createBufferQueueAudioPlayer(Constants.fs, Constants.bufferSize);
        int micInterface = 0;
        if (Build.MODEL.equals("IN2020")) {
            micInterface=1;
        }
        Log.e("interface",micInterface+"");
        NativeAudio.createAudioRecorder(micInterface);

//        NativeAudio.init();

        Constants.recButton=(Button) findViewById(R.id.record);
        Constants.stopButton=(Button) findViewById(R.id.button2);
        Constants.clayout = (ConstraintLayout)findViewById(R.id.clayout);

        Constants.init(this);

        assetManager = getAssets();
        Constants.recButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                Constants.stop=false;
                int status = ActivityCompat.checkSelfPermission(NativeAudio.this,
                        Manifest.permission.RECORD_AUDIO);
                if (status != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            NativeAudio.this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            AUDIO_ECHO_REQUEST);
                    return;
                }
                closeKeyboard();
                recordAudio();
            }
        });
        Constants.stopButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                Constants.recordImu=false;
                forcewrite();
                FileOperations.writeSensorsToDisk(NativeAudio.this,Constants.tt+"");
                try {
                    if (Constants.recTime==1800) {
                        Thread.sleep(60000);
                    }
                    else {
                        Thread.sleep(3000);
                    }
                }
                catch(Exception e) {
                    Log.e("asdf",e.toString());
                }

                double[] out = NativeAudio.getDistance(Constants.reply);

                if (out.length==5) {
                    Constants.distOut.setText(String.format("%d\n%d\n%d\n%d\n%d", (int)out[0], (int)out[1], (int)out[2], (int)out[3], (int)out[4]));
                }
                else if (out.length==7) {
                    Constants.distOut.setText(String.format("%.02f\n%.02f\n%.02f\n%d\n%d\n%d\n%d\n",
                            out[0], out[1], out[2], (int)out[3], (int)out[4], (int)out[5], (int)out[6]));
                }

                Constants.stop=true;
//                stopit();
                NativeAudio.reset();
                Constants.task.cancel(true);
                Constants.timer.cancel();
                Constants.tv2.setText("0");
                Constants.toggleUI();

                Log.e("debug2","change bg color");
                (NativeAudio.av).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Constants.clayout.setBackgroundColor(Color.argb(255, 255, 255, 255));
                    }
                });
            }
        });
    }

    // Single out recording for run-permission needs
    private void recordAudio() {
        int status = ActivityCompat.checkSelfPermission(NativeAudio.this,
                Manifest.permission.RECORD_AUDIO);
        if (status != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    NativeAudio.this,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    AUDIO_ECHO_REQUEST);
            return;
        }

        Constants.task = new MyTask(NativeAudio.this,NativeAudio.this).execute();
    }

    /** Called when the activity is about to be destroyed. */
    @Override
    protected void onPause() {
        super.onPause();
    }

    /** Called when the activity is about to be destroyed. */
    @Override
    protected void onDestroy() {
        shutdown();
        super.onDestroy();
    }

    public void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    /** Native methods, implemented in jni folder */
    public static native void createEngine();
    public static native void createBufferQueueAudioPlayer(int sampleRate, int samplesPerBuf);
    public static native boolean createAudioRecorder(int micInterface);
    public static native void stopit();
    public static native void reset();
    public static native void forcewrite();
    public static native void shutdown();
    public static native long nowms2();
//    public static native void calibrate(short[] data, short[] refData, int bufferSize_spk, int bufferSize_mic, int recordTime,
//                                        String topfilename, String bottomfilename,
//                                        String meta_filename, int initialOffset, int warmdown_len, int preamble_len,
//                                        boolean water,
//                                        boolean reply, boolean naiser, int sendDelay,float xcorrthresh, float minPeakDistance,
//                                        int fs,
//                                        double[] naiserTx1, double[] naiserTx2, int Ns, int N0, boolean CP, int N_FSK,
//                                        float naiserThresh, float naiserShoulder,
//                                        int win_size, int bias, int seekback, double pthresh, int round, int filenum, boolean runxcorr, int initialDelay,
//                                        String mic_ts_fname, String speaker_ts_fname,int bigBufferSize,int bigBufferTimes, int numSym, int calibWait);
    public static native void calibrate(int fs, int bufferSize_spk, int bufferSize_mic, int recordTime);

    public static native void testxcorr(double[] data, double[] refData, double[] refData2, int N0, boolean CP);
    public static native double[] getDistance(boolean reply);
    public static native long[] getTimestamps(int recordTime, int fs, int bufferSize_mic);
    public static native double[] getVal();
    public static native boolean responderDone();
    public static native boolean replySet();
    public static native int getXcorrCount();
    public static native int[] getReplyIndexes();
    public static native int getQueuedSpeakerSegments();
    public static native int getNumChirps();

    /** Load jni .so on initialization */
    static {
        System.loadLibrary("native-audio-jni");
    }
}
