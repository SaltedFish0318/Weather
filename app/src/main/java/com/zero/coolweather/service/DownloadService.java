package com.zero.coolweather.service;

import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.zero.coolweather.listener.DownloadListener;
import com.zero.coolweather.util.DownloadTask;

import java.io.File;

public class DownloadService extends Service {

    private DownloadTask downloadTask;

    private String downloadUrl;

    //下载监听器
    private DownloadListener listener = new DownloadListener() {
        @Override
        public void onSuccess(String file) {
            //下载成功
            downloadTask = null;
            Toast.makeText(DownloadService.this, "下载成功！", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            Uri uri = Uri.fromFile(new File(file));
            intent.setData(uri);
            getApplicationContext().sendBroadcast(intent);//这个广播的目的就是更新图库
        }

        @Override
        public void onFailed() {
            //下载失败
            downloadTask = null;
            Toast.makeText(DownloadService.this, "下载失败！", Toast.LENGTH_SHORT).show();
        }
    };

    private DownloadBinder mBinder = new DownloadBinder();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class DownloadBinder extends Binder{

        /**
         * 开始下载
         * @param url
         */
        public void startDownload(String url){
            Log.d("开始下载",url);
            if(downloadTask == null){
                downloadUrl = url;
                downloadTask = new DownloadTask(listener);
                downloadTask.execute(downloadUrl);
                Toast.makeText(DownloadService.this, "正在下载！", Toast.LENGTH_SHORT).show();
            }
        }

    }
}
