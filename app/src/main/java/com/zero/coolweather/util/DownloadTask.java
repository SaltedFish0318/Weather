package com.zero.coolweather.util;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.zero.coolweather.listener.DownloadListener;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class DownloadTask extends AsyncTask<String, Integer, Integer> {

    public static final int TYPE_SUCCESS = 0;
    public static final int TYPE_FAILED = 1;

    private String fileName = null;
    private String directory = null;

    private DownloadListener listener;

    //获取监听器
    public DownloadTask(DownloadListener listener){
        this.listener = listener;
    }

    /**
     * 在后台进行的线程操作
     * @param strings
     * @return
     */
    @Override
    protected Integer doInBackground(String... strings) {
        InputStream is = null;
        RandomAccessFile savedFile = null;
        File file = null;   //文件
        File fileDir = null;    //文件目录

        int result = TYPE_FAILED;

        try{
            String downloadUrl = strings[0];
            fileName = downloadUrl.substring(downloadUrl.lastIndexOf('/') + 1);
            directory = Environment.getExternalStorageDirectory().getPath() + "/BUGWeather/";
            fileDir = new File(directory);
            if(!fileDir.exists()){
                fileDir.mkdirs();
            }

            file = new File(directory + fileName);

            OkHttpClient client = new OkHttpClient();
            Request request = new Request.Builder().url(downloadUrl).build();
            Response response = client.newCall(request).execute();

            if(response != null){
                is = response.body().byteStream();
                savedFile = new RandomAccessFile(file,"rw");

                byte[] b = new byte[1024];
                int len = 0;
                int total = 0;

                //将内容逐行写入本地文件
                while((len = is.read(b)) != -1){
                    savedFile.write(b,0,len);
                    total += len;
                }
                response.body().close();
                Log.d("文件大小", total + "");
                Log.d("文件路劲", file.getAbsolutePath());
                result = TYPE_SUCCESS;
                return TYPE_SUCCESS;
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            try {
                if(is != null){
                    is.close();
                }
                if(savedFile != null){
                    savedFile.close();
                }
                if(result == TYPE_FAILED && file != null){
                    file.delete();
                }
            }catch (Exception ex){
                ex.printStackTrace();
            }
        }

        return TYPE_FAILED;
    }

    @Override
    protected void onPostExecute(Integer status) {
        switch (status){
            case TYPE_SUCCESS:
                listener.onSuccess(directory + fileName);
                break;

            case TYPE_FAILED:
                listener.onFailed();
                break;

            default:
                break;
        }
    }
}
