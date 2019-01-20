package com.zero.coolweather;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.opengl.Visibility;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.zero.coolweather.gson.Forecast;
import com.zero.coolweather.gson.Suggestion;
import com.zero.coolweather.gson.Weather;
import com.zero.coolweather.util.HttpUtil;
import com.zero.coolweather.util.Utility;

import org.w3c.dom.Text;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView weatherLayout;

    private TextView titleCity;

    private TextView titleUpdateTime;

    private TextView degreeText;

    private TextView weatherInfoText;

    private LinearLayout forecastLayout;

    private TextView aqiText;

    private TextView pm25Text;

    private TextView comfortText;

    private TextView carWashText;

    private TextView sportText;

    private ImageView bingPicImg;

    public SwipeRefreshLayout swipeRefresh;

    private String mWeatherId;

    public DrawerLayout drawerLayout;

    private Button navButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= 21){
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                |View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }

        setContentView(R.layout.activity_weather);

        //初始化各控件
        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout)findViewById(R.id.forecase_layout);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);
        bingPicImg = (ImageView)findViewById(R.id.weather_pic_image);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton = (Button) findViewById(R.id.nav_button);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);
        String aqiString = prefs.getString("aqi",null);
        if (weatherString != null && aqiString != null) {
            //有缓存时直接解析天气
            Weather weather = new Weather();
            weather.weatherInfo = Utility.handleWeatherInfoResonse(weatherString);
            weather.aqiInfo = Utility.handleAQIInfoResonse(aqiString);
            mWeatherId = weather.weatherInfo.basic.weatherId;
            showWeatherInfo(weather,0);
        }else{
            //无缓存时去服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.VISIBLE);
            requestWeather(mWeatherId);
        }

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId);
            }
        });

        navButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        //加载必应每日一图
        loadBingPic();

    }

    public void loadBingPic(){
        String bingPic = "https://cn.bing.com/az/hprichbg/rb/DivingEmperors_ZH-CN8118506169_1920x1080.jpg";
        Glide.with(this).load(bingPic).into(bingPicImg);
    }

    /**
     * 处理并展示Weather实体类中的数据
     */
    private void showWeatherInfo(Weather weather,int updateMode) {
        if (updateMode == 1 || updateMode == 0) {

            String cityName = weather.weatherInfo.basic.cityName;
            String updateTime = "数据更新时间：" + weather.weatherInfo.update.updateTime;
            String degree = weather.weatherInfo.now.temperature + "℃";
            String weatherInfo = weather.weatherInfo.now.cond_txt;
            titleCity.setText(cityName);
            titleUpdateTime.setText(updateTime);
            degreeText.setText(degree);
            weatherInfoText.setText(weatherInfo);
            forecastLayout.removeAllViews();
            for (Forecast forecast : weather.weatherInfo.forecastList) {
                View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
                TextView dateText = (TextView) view.findViewById(R.id.date_text);
                TextView infoText = (TextView) view.findViewById(R.id.info_text);
                TextView maxText = (TextView) view.findViewById(R.id.max_text);
                TextView minText = (TextView) view.findViewById(R.id.min_text);
                dateText.setText(forecast.date);
                infoText.setText(forecast.cond_txt_d);
                maxText.setText(forecast.tmp_max);
                minText.setText(forecast.tmp_min);
                forecastLayout.addView(view);
            }

            for (Suggestion suggestion : weather.weatherInfo.lifestyleList) {
                if (suggestion.type.equals("comf")){
                    comfortText.setText("舒适度：" + suggestion.txt);
                } else if (suggestion.type.equals("cw")) {
                    carWashText.setText("洗车指数：" + suggestion.txt);
                } else if (suggestion.type.equals("sport")) {
                    sportText.setText("运动建议" + suggestion.txt);
                }
            }
        }

        if (updateMode == 2 || updateMode == 0) {
            if (weather.aqiInfo.aqi != null) {
                aqiText.setText(weather.aqiInfo.aqi.aqi);
                pm25Text.setText(weather.aqiInfo.aqi.pm25);
            }
        }

        weatherLayout.setVisibility(View.VISIBLE);
    }

    /**
     * 根据天气id请求城市天气信息
     */
    public void requestWeather(final String weatherId){

        String parameters = "location=" + weatherId + "&key=205adaf1dd184d2eaa2327b33bfcb467";
        String weatherUrl = "https://free-api.heweather.net/s6/weather?" + parameters;
        String AQIUrl = "https://free-api.heweather.net/s6/air/now?" + parameters;

        final Weather weather = new Weather();

        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                weather.weatherInfo = Utility.handleWeatherInfoResonse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather.weatherInfo != null && "ok".equals(weather.weatherInfo.status)){
                            SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            mWeatherId = weather.weatherInfo.basic.weatherId;
                            showWeatherInfo(weather,1);
                        }else{
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }
                        swipeRefresh.setRefreshing(false);
                    }
                });
            }
        });

        HttpUtil.sendOkHttpRequest(AQIUrl, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取空气信息失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                weather.aqiInfo = Utility.handleAQIInfoResonse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather.aqiInfo != null && "ok".equals(weather.aqiInfo.status)){
                            SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("aqi",responseText);
                            editor.apply();
                            showWeatherInfo(weather,2);
                        }else{
                            Toast.makeText(WeatherActivity.this, "获取空气信息失败", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }
}
