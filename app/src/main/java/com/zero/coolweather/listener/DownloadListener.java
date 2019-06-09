package com.zero.coolweather.listener;

public interface DownloadListener {

    void onSuccess(String file);

    void onFailed();

}
