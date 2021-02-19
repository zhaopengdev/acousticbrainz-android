package org.metabrainz.essentiaandroid;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.metabrainz.acousticbrainz.AcousticBrainzClient;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Objects;
import java.util.Timer;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.schedulers.Schedulers;



public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private long begin;
    private static final int REQUEST_CODE = 5;
    private TextView info;
    private TextView content;
    private int type = 0;

    private final String test1 = "LogicDemo144.mp3";
    private final String test2 = "testTonal1";

    private int hopSize = 256;

    private Score scoreToPlay;

    private ScorePlayer mScorePlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mScorePlayer = new ScorePlayer(this);
        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        content  = findViewById(R.id.content);
        findViewById(R.id.start).setOnClickListener(this);
        findViewById(R.id.copy).setOnClickListener(this);
        findViewById(R.id.play).setOnClickListener(this);
        File file = new File(getToComputeFile());
        if(file.exists() && file.length() > 0){
            tv.setText("文件已就绪，大小:"+file.length());
            content.setText("请点击开始");
        }else{
            tv.setText("文件不存在，请先拷贝");
        }
        info = tv;


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE) {


        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initiateWorkRequest() {
        begin = System.currentTimeMillis();
        info.setText("拷贝完成");
    }

    private void startCopy(){
        Completable.fromAction(() -> {
                        try {
                            File directory = getExternalFilesDir(null);
                            String fileName = test2+".mp3";
                            InputStream in = getAssets().open(fileName);
                            getFile(in,directory.getAbsolutePath() + File.separator + fileName);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        })
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::initiateWorkRequest);
    }

    public static void getFile(InputStream is,String fileName) throws IOException{
        BufferedInputStream in=null;
        BufferedOutputStream out=null;
        in=new BufferedInputStream(is);
        out=new BufferedOutputStream(new FileOutputStream(fileName));
        int len=-1;
        byte[] b=new byte[1024];
        while((len=in.read(b))!=-1){
            out.write(b,0,len);
        }
        in.close();
        out.close();
        Log.d("essentia","copy file file:"+fileName);
    }

    private void startCompute(int type){
        try {

            final File file = new File(getToComputeFile());
            final boolean exists = file.exists();
            final String inputPath = file.getAbsolutePath();

            if(!exists){
                //Log.e("Essentia Android", "Input Path: " + inputPath);
                info.setText("发生错误：音频文件不存在");
                return;
            }else{
                Log.d("essentia","文件存在：finalInputPath:"+inputPath);
            }
            String outputPath = getOutPutJson();
//            File outFile = new File(outputPath);
//            outFile.deleteOnExit();
            Log.d("Essentia Android", "essentia Input Path: " + inputPath);
            Log.d("Essentia Android", "essentia Output Path: " + outputPath);
            String finalInputPath = inputPath;
            if(type == 1) {
                Observable
                        .fromCallable(() -> AcousticBrainzClient.extractData(finalInputPath, outputPath))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::essentiaTaskCompleted);
            }else {
                Observable
                        .fromCallable(() -> AcousticBrainzClient.extractPitch(finalInputPath, outputPath,hopSize))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::essentiaTaskCompleted);
//                        .dispose();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getOutPutJson(){
        File directory = getExternalFilesDir(null);
        String fileName = test2;
        String outputPath = directory.getAbsolutePath() + File.separator + fileName + ".json";
        return outputPath;
    }

    private String getToComputeFile(){
        String fileName = test2;
        File directory = getExternalFilesDir(null);
        return directory.getAbsolutePath() + File.separator+fileName+".mp3";
    }

    public  String readFile(String filePath) throws IOException {

        File file = new File(filePath);
        if(!file.exists()){
            //this.info.setText("文件不存在");
            return null;
        }
        /**
         * 读出城市列表文件
         */
            FileInputStream is = null;
            StringBuilder stringBuilder = null;
            try {
                if (file.length() != 0) {
                    /**
                     * 文件有内容才去读文件
                     */
                    is = new FileInputStream(file);
                    InputStreamReader streamReader = new InputStreamReader(is);
                    BufferedReader reader = new BufferedReader(streamReader);
                    String line;
                    stringBuilder = new StringBuilder();
                    while ((line = reader.readLine()) != null) {
                        // stringBuilder.append(line);
                        stringBuilder.append(line);
                    }
                    //this.content.setText(stringBuilder);
                    reader.close();
                    is.close();
                    return stringBuilder.toString();
                } else {
                   //this.info.setText("文件不存在");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

    }


    private void essentiaTaskCompleted(int result) {
        long time = System.currentTimeMillis() - begin;
        time = time / (1000 * 60);
        Log.d("Essentia Android", "essentia Result Code: " + result);
        ((TextView) findViewById(R.id.sample_text)).setText("Essentia Task Completed in " + time + " mins"+",result:"+result);
        try {
            final String jsonResult = readFile(getOutPutJson());
//            this.content.setText(jsonResult == null ? "发生错误" : jsonResult);
            //EssentiaUtils.INSTANCE.parseEssentiaResult(jsonResult);
            this.scoreToPlay = EssentiaUtils.INSTANCE.computeScore(jsonResult);
            this.content.setText(this.scoreToPlay.getLog());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String getFileName(Uri uri) {
        String result = null;
        if (Objects.requireNonNull(uri.getScheme()).equals("content")) {
            Cursor cursor = getContentResolver()
                    .query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst())
                result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
        }

        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.start){
            content.setText("请等待");
            startCompute(type);
        }else if(v.getId() == R.id.copy){
            startCopy();
        }else if(v.getId() == R.id.play){
            if(this.scoreToPlay == null){
                info.setText("错误，没有要播放的内容");
                return;
            }
            playScore(this.scoreToPlay);
        }
    }

    private void playScore(Score score){
        mScorePlayer.play(score);
    }
//    String getFilePath(Context context, Uri uri) {
//        return FileUri.INSTANCE.getFilePathByUri(context,uri);
//    }
}