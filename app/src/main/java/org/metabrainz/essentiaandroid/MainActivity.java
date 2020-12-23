package org.metabrainz.essentiaandroid;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.metabrainz.acousticbrainz.AcousticBrainzClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

import ando.file.core.FileUri;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

import static android.content.Intent.URI_ANDROID_APP_SCHEME;
import static org.metabrainz.essentiaandroid.EssentiaJava.essentiaMusicExtractor;

public class MainActivity extends AppCompatActivity {

    private long begin;
    private static final int REQUEST_CODE = 5;
    private TextView info;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = findViewById(R.id.sample_text);
        tv.setText("Hello, World!");
        info = tv;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == REQUEST_CODE) {
            try {

//                Uri uri = data.getData();
//                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
//                    uri =  Uri.parse(data.toUri(URI_ANDROID_APP_SCHEME));
//                }else{
//                    uri =  Uri.parse(data.toURI());
//                }


                String fileName = getFileName(data.getData());
                File tempFile = new File(getExternalCacheDir(), fileName);
                File directory = getExternalFilesDir(null);
                String inputPath = tempFile.getAbsolutePath();
                if (!inputPath.endsWith(".mp3"))
                    inputPath = inputPath + ".mp3";
//                final String inputPath = getFilePath(this,uri)+"/"+fileName;
                
                final File file = new File(inputPath);
                final boolean exists = file.exists();

                String outputPath = directory.getAbsolutePath() + File.separator + fileName + ".json";
                Log.d("Essentia Android", "essentia Input Path: " + inputPath);
                Log.d("Essentia Android", "essentia Output Path: " + outputPath);

//                Completable.fromAction(() -> {
//                            InputStream inputStream = getContentResolver().openInputStream(data.getData());
//
//                            byte[] buffer = new byte[inputStream.available()];
//                            inputStream.read(buffer);
//                            inputStream.close();
//
//                            OutputStream outputStream = new FileOutputStream(tempFile);
//                            outputStream.write(buffer);
//                            outputStream.close();
//
//                        })
//                        .subscribeOn(Schedulers.io())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(this::initiateWorkRequest);
//                        .dispose();

                if(!exists){
                    //Log.e("Essentia Android", "Input Path: " + inputPath);
                    info.setText("发生错误：音频文件不存在");
                    return;
                }else{
                    Log.d("essentia","finalInputPath:"+inputPath);
                }
                String finalInputPath = inputPath;
//                Observable
//                        .fromCallable(() -> EssentiaJava.essentiaMusicExtractor(finalInputPath, outputPath))
//                        .subscribeOn(Schedulers.newThread())
//                        .observeOn(AndroidSchedulers.mainThread())
//                        .subscribe(this::essentiaTaskCompleted);
                Observable
                        .fromCallable(() -> AcousticBrainzClient.extractPitch(finalInputPath, outputPath))
                        .subscribeOn(Schedulers.newThread())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::essentiaTaskCompleted);
//                        .dispose();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void initiateWorkRequest() {
        begin = System.currentTimeMillis();
        info.setText("拷贝完成");


    }

    private void essentiaTaskCompleted(int result) {
        long time = System.currentTimeMillis() - begin;
        time = time / (1000 * 60);
        Log.d("Essentia Android", "essentia Result Code: " + result);
        ((TextView) findViewById(R.id.sample_text)).setText("Essentia Task Completed in " + time + " mins"+",result:"+result);
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

    String getFilePath(Context context, Uri uri) {
        return FileUri.INSTANCE.getFilePathByUri(context,uri);
    }
}