package com.zero.coolweather.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;

import com.zero.coolweather.gson.Weather;
import com.zero.coolweather.util.HttpUtil;
import com.zero.coolweather.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class AutoUpdateService extends Service {
    public AutoUpdateService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        updateWeather();
        updateBingpic();

        AlarmManager manager = (AlarmManager)getSystemService(ALARM_SERVICE);
        int anHour = 8 * 60 * 60 * 1000; //这是8小时的毫秒数
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent i = new Intent(this, AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getActivities(this, 0, new Intent[]{i}, 0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,triggerAtTime,pi);
        return super.onStartCommand(intent, flags, startId);
    }

    /**
     *
     */
    private void updateBingpic() {
        final String requestBingPic = "http:guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(AutoUpdateService.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
            }
        });
    }

    /**
     * 更新天气信息
     */
    private void updateWeather() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        String aqiString = prefs.getString("aqi", null);
        if (weatherString != null && aqiString != null) {
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString,aqiString);
            String weatherId = weather.weatherInfo.basic.weatherId;
            String parent_city = weather.weatherInfo.basic.parentCity;

            String parametersWeather = "location=" + weatherId + "&key=205adaf1dd184d2eaa2327b33bfcb467";
            String weatherUrl = "https://free-api.heweather.net/s6/weather?" + parametersWeather;
            String parametersWeatherAQI = "location=" + parent_city + "&key=205adaf1dd184d2eaa2327b33bfcb467";
            String AQIUrl = "https://free-api.heweather.net/s6/air/now?" + parametersWeatherAQI;

            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    Weather.WeatherInfo weatherInfo = Utility.handleWeatherInfoResponse(responseText);
                    if (weatherInfo != null && weatherInfo.status.equals("ok")) {
                        SharedPreferences.Editor editor = PreferenceManager
                                .getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("weather", responseText);
                        editor.apply();
                    }

                }
            });

            HttpUtil.sendOkHttpRequest(AQIUrl, new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    e.printStackTrace();
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    Weather.AQIInfo aqiInfo = Utility.handleAQIInfoResponse(responseText);
                    if (aqiInfo != null && aqiInfo.status.equals("ok")) {
                        SharedPreferences.Editor editor = PreferenceManager
                                .getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("aqi", responseText);
                        editor.apply();
                    }
                }
            });
        }
    }
}
