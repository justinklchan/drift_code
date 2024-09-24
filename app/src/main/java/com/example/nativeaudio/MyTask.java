package com.example.nativeaudio;

//import static com.example.nativeaudio.NativeAudio.recAndPlay;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.ConditionVariable;
import android.os.CountDownTimer;
import android.os.Environment;
import android.util.Log;
import android.widget.EditText;

import java.io.File;
import java.util.ArrayList;

import androidx.appcompat.widget.TintTypedArray;

public class MyTask extends AsyncTask<Void, Void, Void> {

    Activity av;
    Context cxt;
    boolean graySet = false;
    boolean replySet1 = false;
    boolean replySet2 = false;
    boolean replySet3 = false;
    boolean senderSet1 = false;
    boolean senderSet2 = false;
    boolean senderSet3 = false;

    int[] streams = new int[]{AudioManager.STREAM_MUSIC,
            AudioManager.STREAM_ACCESSIBILITY, AudioManager.STREAM_ALARM,
            AudioManager.STREAM_DTMF, AudioManager.STREAM_NOTIFICATION,
            AudioManager.STREAM_RING, AudioManager.STREAM_SYSTEM,
            AudioManager.STREAM_VOICE_CALL};

    public MyTask(Context cxt, Activity av) {
        this.cxt = cxt;
        this.av = av;
    }

    public short[] generateCalibrationSignal(short[] data, int begin_gap, int warmup_len, int gap_len, int warmdown_len) {
        int freq=500;

        int size = data.length+begin_gap+gap_len+warmup_len+warmdown_len;
        size = (size/Constants.bufferSize)*Constants.bufferSize+Constants.bufferSize;

        short[] combined = new short[size];
        for (int i = begin_gap; i < begin_gap+warmup_len; i++) {
            combined[i] = (short)(Math.sin(2 * Math.PI * freq * ((double)i / Constants.fs))*32767.0*Constants.vol);
        }
        int counter=0;
        for (int i = begin_gap+warmup_len+gap_len; i < begin_gap+warmup_len+gap_len+data.length; i++) {
            combined[i] = (short)(data[counter++]*Constants.vol);
        }
        for (int i = begin_gap+warmup_len+gap_len+data.length; i < begin_gap+warmup_len+gap_len+data.length+warmdown_len; i++) {
            combined[i] = (short)(Math.sin(2 * Math.PI * freq * ((double)i / Constants.fs))*100.0);
        }
        return combined;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();

        Constants.time_acc=new ArrayList<>();
        Constants.time_gyro=new ArrayList<>();
        Constants.time_acc_uncalib=new ArrayList<>();
        Constants.time_gyro_uncalib=new ArrayList<>();
        Constants.time_mag=new ArrayList<>();
        Constants.time_mag_uncalib=new ArrayList<>();
        Constants.time_pressure=new ArrayList<>();
        Constants.accx=new ArrayList<>();
        Constants.accy=new ArrayList<>();
        Constants.accz=new ArrayList<>();
        Constants.gyrox=new ArrayList<>();
        Constants.gyroy=new ArrayList<>();
        Constants.gyroz=new ArrayList<>();
        Constants.accx_uncalib=new ArrayList<>();
        Constants.accy_uncalib=new ArrayList<>();
        Constants.accz_uncalib=new ArrayList<>();
        Constants.gyrox_uncalib=new ArrayList<>();
        Constants.gyroy_uncalib=new ArrayList<>();
        Constants.gyroz_uncalib=new ArrayList<>();
        Constants.magx=new ArrayList<>();
        Constants.magy=new ArrayList<>();
        Constants.magz=new ArrayList<>();
        Constants.magx_uncalib=new ArrayList<>();
        Constants.magy_uncalib=new ArrayList<>();
        Constants.magz_uncalib=new ArrayList<>();
        Constants.pressure_data=new ArrayList<>();
        Constants.time_rot=new ArrayList<>();
        Constants.rotx=new ArrayList<>();
        Constants.roty=new ArrayList<>();
        Constants.rotz=new ArrayList<>();
        Constants.rot4=new ArrayList<>();
        Constants.time_linear_acc=new ArrayList<>();
        Constants.linearaccx=new ArrayList<>();
        Constants.linearaccy=new ArrayList<>();
        Constants.linearaccz=new ArrayList<>();
        Constants.recordImu=true;

        Constants.tt = System.currentTimeMillis();
        String t2 = Constants.tt+"";

        String dir = av.getExternalFilesDir(null).toString();
        File path = new File(dir+"/"+t2);
        path.mkdir();

//        Constants.debugPane.setText("");
//        Constants.tv.setText(t2.substring(t2.length()-4,t2.length()));
        Constants.toggleUI();

//        AudioManager man = (AudioManager)cxt.getSystemService(Context.AUDIO_SERVICE);
//        for (Integer i : streams) {
//            man.setStreamVolume(i,(int)(man.getStreamMaxVolume(i)),0);
//        }

//        int calc=(Constants.recTime+Constants.initSleep)+((Constants.recTime+5)*(Constants.rounds-1));
        int calc=(Constants.recTime+Constants.initSleep)*Constants.rounds;
//        Constants.timer = new CountDownTimer((int)calc*1000, 1000) {
//            public void onTick(long millisUntilFinished) {
//                Constants.tv2.setText("" + (millisUntilFinished / 1000));
//            }
//
//            public void onFinish() {
//            }
//        }.start();
//        Constants.clayout.setBackgroundColor(Color.argb(255, 255, 255, 255));
    }

    @Override
    protected void onPostExecute(Void unused) {
        super.onPostExecute(unused);
        Log.e("debug2","post execute");
        NativeAudio.stopit();
        Constants.toggleUI();
//        Constants.timer.cancel();
//        Constants.tv2.setText("0");
//        Utils.log("------------------------------");
//        Constants.clayout.setBackgroundColor(Color.argb(255, 255, 255, 255));
        Constants.recordImu=false;
        Constants.tv.setText(Constants.tt+"");
//        FileOperations.writeSensorsToDisk(av,Constants.tt+"");
    }

    public void work() {
//        int begin_gap = 6000;
//        int warmup_len = 2000; //don't make this too large otherwise it will trigger xcorr
//        int gap_len=2000;
//        int warmdown_len=0;
//        short[] data = generateCalibrationSignal(Constants.sig, begin_gap, warmup_len, gap_len,warmdown_len);

        String dir = cxt.getExternalFilesDir(null).toString();
//        String dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS).getAbsolutePath();
//        FileOperations.writetofile_str(av,Constants.user_id+"\n"+Constants.reply,Constants.tt+"/"+Constants.tt+"_role.txt");

//                try {
//                    Thread.sleep(Constants.initSleep * 1000);
//                } catch (Exception e) {
//                    Log.e("asdf_sleep", e.toString());
//                }
            String topfilename = dir  + "/" + Constants.tt + "-top.txt";
            String bottomfilename = dir  + "/" + Constants.tt + "-bottom.txt";
//            String meta_filename = dir  + "/"+Constants.tt+ "/" + Constants.tt + "-" + Constants.fileID + "-meta.txt";
//            String mic_ts_filename = dir  + "/"+Constants.tt+ "/" + Constants.tt + "-" + Constants.fileID + "-mic_ts.txt";
//            String speaker_ts_filename = dir  + "/"+Constants.tt+ "/" + Constants.tt + "-" + Constants.fileID + "-speaker_ts.txt";
//
//            int initialOffset = begin_gap + warmup_len + gap_len;
//            int init_delay = (int)(Constants.replyDelay * Constants.fs);
//            int reply_delay = (int) (Constants.replyDelay * Constants.fs);
//            if(Constants.reply){
//                reply_delay = (int) ((Constants.replyDelay + ((double)Constants.user_id)*0.32) * Constants.fs);
//            }
//            if (!Constants.stop) {
//                short[] sig = new short[Constants.sig.length];
//                short[] sig2 = new short[Constants.sig.length];
//                int counter=0;
//                for (Short s : Constants.sig) {
//                    sig2[counter++]=(short)(s*Constants.vol);
//                }
//                counter=0;
//                for (Short s : Constants.leader_sig) {
//                    sig[counter++]=(short)(s*Constants.vol);
//                }
                // replyDelay for the sender dictates the period between transmitting chirps
//                NativeAudio.calibrate(sig2, sig2, Constants.bufferSize_spk, Constants.bufferSize, Constants.recTime, topfilename, bottomfilename, meta_filename,
//                        initialOffset, warmdown_len, Constants.sig.length, Constants.water,
//                        Constants.reply, Constants.naiser, reply_delay, Constants.xcorrthresh,
//                        Constants.minPeakDistance, Constants.fs, Constants.leader_pre1, Constants.leader_pre2, Constants.Ns, Constants.N0, Constants.CP, Constants.N_FSK,
//                        Constants.naiserThresh, Constants.naiserShoulder, Constants.win_size, Constants.bias,
//                        Constants.seekback, Constants.pthresh, 0, Constants.fileID, Constants.runxcorr, init_delay,
//                        mic_ts_filename,speaker_ts_filename,Constants.bigBufferSize,Constants.bigBufferTimes,Constants.numsym,Constants.calibWait);
        Constants.bufferSize=Integer.parseInt(Constants.et14.getText().toString());
        Constants.recTime=Integer.parseInt(Constants.et13.getText().toString());
        NativeAudio.calibrate(Constants.fs, Constants.bufferSize_spk, Constants.bufferSize, Constants.recTime,
            Constants.bigBufferSize, Constants.bigBufferTimes,topfilename, bottomfilename);

        try {
            Thread.sleep((int) ((Constants.recTime+1) * 1000));
        }catch(Exception e) {

        }
        long[] timestamps = NativeAudio.getTimestamps(Constants.recTime,Constants.fs,Constants.bufferSize);
        FileOperations.writetofile(av, timestamps,System.currentTimeMillis()+".txt");
        Log.e("asdf","asdf");
    }

    @Override
    protected Void doInBackground(Void... voids) {
        Log.e("asdf","start");

        work();

        return null;
    }
}
