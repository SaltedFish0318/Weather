package com.zero.coolweather.listener;

import android.content.Context;

import java.io.File;

public interface DownloadListener {

    void onSuccess(String file);

    void onFailed();

}
