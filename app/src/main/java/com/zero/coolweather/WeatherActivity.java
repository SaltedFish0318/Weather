package com.zero.coolweather;

import android.Manifest;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.opengl.Visibility;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.renderscript.ScriptGroup;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewParent;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.zero.coolweather.db.MyCity;
import com.zero.coolweather.gson.Forecast;
import com.zero.coolweather.gson.HourlyForecast;
import com.zero.coolweather.gson.Suggestion;
import com.zero.coolweather.gson.Weather;
import com.zero.coolweather.service.AutoUpdateService;
import com.zero.coolweather.service.DownloadService;
import com.zero.coolweather.util.HttpUtil;
import com.zero.coolweather.util.Utility;

import org.litepal.crud.DataSupport;
import org.w3c.dom.Text;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity implements View.OnClickListener {

    private DownloadService.DownloadBinder downloadBinder;

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            downloadBinder = (DownloadService.DownloadBinder) service;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    private ScrollView weatherLayout;

    //顶部标题
    private TextView titleCity;

    //更新时间
    private TextView titleUpdateTime;

    //温度
    private TextView degreeText;

    //天气信息
    private TextView weatherInfoText;

    private ImageView weatherIcon;

    //体感温度
    private TextView feelTmpText;

    private LinearLayout forecastLayout;

    private LinearLayout hourlyForecastLayout;

    private LinearLayout suggestionLayout;

    //空气质量
    private TextView airQualityTitle;

    //aqi信息
    private TextView aqiText;

    //pm2.5
    private TextView pm25Text;

    //bing背景
    private ImageView bingPicImg;

    //下拉刷新
    public SwipeRefreshLayout swipeRefresh;

    //城市天气id
    private String mWeatherId;

    private String mParentCity;

    //抽屉
    public DrawerLayout drawerLayout;

    //导航按钮
    private Button navButton;

    //更多按钮
    private Button titleMenuButton;

    //选择区域的碎片
    private ChooseAreaFragment chooseAreaFragment;

    private RelativeLayout statusBarAddHigh;

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
        chooseAreaFragment = (ChooseAreaFragment)getSupportFragmentManager()
                .findFragmentById(R.id.choose_area_fragment);

        swipeRefresh = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh);
        swipeRefresh.setColorSchemeColors(getResources().getColor(R.color.colorPrimary));
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        feelTmpText = (TextView)findViewById(R.id.feel_tmp_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        forecastLayout = (LinearLayout)findViewById(R.id.forecast_layout);
        hourlyForecastLayout = (LinearLayout)findViewById(R.id.hourly_forecast_layout);
        suggestionLayout = (LinearLayout)findViewById(R.id.suggestion_LL);
        statusBarAddHigh = (RelativeLayout) findViewById(R.id.status_bar_add_high);
        airQualityTitle = (TextView) findViewById(R.id.air_quality);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        bingPicImg = (ImageView)findViewById(R.id.weather_pic_image);
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        navButton = (Button) findViewById(R.id.nav_button);
        titleMenuButton = (Button) findViewById(R.id.title_menu);
        weatherIcon = (ImageView) findViewById(R.id.weather_icon);

        //从内存中查找hash信息
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);
        String aqiString = prefs.getString("aqi",null);
        if (weatherString != null && aqiString != null) {
            //有缓存时直接解析天气
            Weather weather = new Weather();
            weather.weatherInfo = Utility.handleWeatherInfoResponse(weatherString);
            weather.aqiInfo = Utility.handleAQIInfoResponse(aqiString);
            Log.d("天气json",weatherString);
            mWeatherId = weather.weatherInfo.basic.weatherId;
            mParentCity = weather.weatherInfo.basic.parentCity;
            chooseAreaFragment.myPosition = mWeatherId;
            showWeatherInfo(weather,0);
        }else{
            //无缓存时去服务器查询天气
            mWeatherId = getIntent().getStringExtra("weather_id");
            mParentCity = getIntent().getStringExtra("parent_city");
            weatherLayout.setVisibility(View.VISIBLE);
            requestWeather(mWeatherId,mParentCity);
        }

        swipeRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(mWeatherId,mParentCity);
            }
        });

        navButton.setOnClickListener(this);
        titleMenuButton.setOnClickListener(this);

        //加载必应每日一图
        String bingPic = prefs.getString("bing_pic", null);
        if(bingPic != null){
            Glide.with(this).load(bingPic).into(bingPicImg);
        }else{
            loadBingPic(1);
        }

        //启动服务
        Intent intent = new Intent(this, DownloadService.class);
        startService(intent);
        bindService(intent, connection, BIND_AUTO_CREATE);

        Intent intentAUS = new Intent(this,AutoUpdateService.class);
        startService(intentAUS);

        statusBarAddHigh.setVisibility(View.VISIBLE);
    }

    public void loadBingPic(final int type){
        String requestBingPic = "http://guolin.tech/api/bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String bingPic = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager
                        .getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",bingPic);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(type == 1)
                            Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
                        else{
                            saveBingPicToLocal();
                        }
                    }
                });
            }
        });
    }

    private String getIconUrl(String cond_code){

        String url = "https://cdn.heweather.com/cond_icon/" + cond_code;

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH-mm-ss");
        String nowTime = simpleDateFormat.format(new Date());

        if((nowTime.compareTo("06-00-00")<0 || nowTime.compareTo("18-00-00")>0)
                && (cond_code.equals("100") || cond_code.equals("103")
                || cond_code.equals("104") || cond_code.equals("301")
                || cond_code.equals("407"))){

            Log.d("处于夜间：","true");
            url += "n";
        }

        return url + ".png";
    }

    private String getIconUrl(String cond_code, boolean isNight){
        String url = "https://cdn.heweather.com/cond_icon/" + cond_code;

        if(isNight){
            if(cond_code.equals("100") || cond_code.equals("103")
                    || cond_code.equals("104") || cond_code.equals("301")
                    || cond_code.equals("407")){

                url += "n";
            }
        }

        return url + ".png";
    }

    private String getIconUrl(String cond_code, String time){
        String url = "https://cdn.heweather.com/cond_icon/" + cond_code;

        if((time.compareTo("06-00-00")<0 || time.compareTo("18-00-00")>0) &&
                cond_code.equals("100") || cond_code.equals("103")
                || cond_code.equals("104") || cond_code.equals("301")
                || cond_code.equals("407")){

            url += "n";
        }

        return url + ".png";
    }

    /**
     * 处理并展示Weather实体类中的数据
     */
    private void showWeatherInfo(Weather weather,int updateMode) {
        if (updateMode == 1 || updateMode == 0) {


            if((DataSupport.where("cityName=?" , weather.weatherInfo.basic.cityName)
                    .find(MyCity.class)).size() == 0){
                MyCity myCity = new MyCity();
                myCity.setCityName(weather.weatherInfo.basic.cityName);
                myCity.setWeatherId(weather.weatherInfo.basic.weatherId);
                myCity.setParentCity(weather.weatherInfo.basic.parentCity);
                myCity.save();

                chooseAreaFragment.loadMyCityListView();
            }

            Log.d("天气代码1：",weather.weatherInfo.now.cond_code);

            Glide.with(WeatherActivity.this).load(getIconUrl(weather.weatherInfo.now.cond_code)).into(weatherIcon);
            String cityName = weather.weatherInfo.basic.cityName;
            String updateTime = "数据更新时间：" + weather.weatherInfo.update.updateTime;
            String degree = weather.weatherInfo.now.temperature + "℃";
            String feelTmp = weather.weatherInfo.now.fellTmp + "℃";
            String weatherInfo = weather.weatherInfo.now.cond_txt;
            titleCity.setText(cityName);
            titleUpdateTime.setText(updateTime);
            degreeText.setText(degree);
            feelTmpText.setText(feelTmp);
            weatherInfoText.setText(weatherInfo);

            forecastLayout.removeAllViews();
            int day = 0;
            for (Forecast forecast : weather.weatherInfo.forecastList) {
                View view = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
                TextView dateText = (TextView) view.findViewById(R.id.date_text);
//                TextView infoText = (TextView) view.findViewById(R.id.info_text);
                TextView maxText = (TextView) view.findViewById(R.id.max_text);
                TextView minText = (TextView) view.findViewById(R.id.min_text);
                ImageView infoIconD = (ImageView) view.findViewById(R.id.info_icon_d);
                ImageView infoIconN = (ImageView) view.findViewById(R.id.info_icon_n);

                String infoIconDUrl = "https://cdn.heweather.com/cond_icon/" + forecast.cond_code_d + ".png";
                Glide.with(WeatherActivity.this).load(getIconUrl(forecast.cond_code_d,false)).into(infoIconD);

                Log.d("天气代码2：",forecast.cond_code_n);
                Glide.with(WeatherActivity.this).load(getIconUrl(forecast.cond_code_n,true)).into(infoIconN);

                String date = "";
                switch (day){
                    case 0:
                        date = "今天 ";
                        break;

                    case 1:
                        date = "明天 ";
                        break;

                    case 2:
                        date = "后天 ";
                        break;

                    default:
                        date = forecast.date;
                        break;
                }
                day++;
                dateText.setText(date);
                maxText.setText(forecast.tmp_max);
                minText.setText(forecast.tmp_min);
                forecastLayout.addView(view);
            }

            if(weather.weatherInfo.hourlyForecastList != null){
                hourlyForecastLayout.removeAllViews();
                for(HourlyForecast hourlyForecast : weather.weatherInfo.hourlyForecastList){
                    View view = LayoutInflater.from(this).inflate(R.layout.hour_forecast_item,hourlyForecastLayout,false);
                    TextView tmpText = (TextView) view.findViewById(R.id.hourly_tmp_txt);
                    ImageView weatherIcon = (ImageView) view.findViewById(R.id.hourly_cond_icon);
                    TextView timeText = (TextView) view.findViewById(R.id.hourly_time_txt);

                    String time = hourlyForecast.time.substring(hourlyForecast.time.indexOf(' ')+1);

                    Glide.with(WeatherActivity.this).load(getIconUrl(hourlyForecast.cond_code,time)).into(weatherIcon);
                    tmpText.setText(hourlyForecast.tmp);
                    timeText.setText(time);
                    hourlyForecastLayout.addView(view);
                }
            }

            if(weather.weatherInfo.lifestyleList != null) {
                suggestionLayout.removeAllViews();
                for (Suggestion suggestion : weather.weatherInfo.lifestyleList) {
                    View view = LayoutInflater.from(this).inflate(R.layout.suggestion_item, suggestionLayout, false);
                    TextView suggestionTitleText = (TextView) view.findViewById(R.id.suggestion_title_text);
                    TextView suggestionText = (TextView) view.findViewById(R.id.suggestion_text);
                    String type = "";
                    if (suggestion.type.equals("comf")) {
                        type = "舒适度指数";
                    } else if (suggestion.type.equals("drsg")) {
                        type = "穿衣指数";
                    } else if (suggestion.type.equals("flu")) {
                        type = "感冒指数";
                    } else if (suggestion.type.equals("sport")){
                        type = "运动指数";
                    } else if (suggestion.type.equals("trav")){
                        type = "旅游指数";
                    } else if (suggestion.type.equals("uv")){
                        type = "紫外线指数";
                    } else if (suggestion.type.equals("cw")){
                        type = "洗车指数";
                    } else if (suggestion.type.equals("air")){
                        type = "空气污染扩散条件指数";
                    }

                    suggestionTitleText.setText(type + "：" + suggestion.brf);
                    suggestionText.setText("    " + suggestion.txt);
                    suggestionLayout.addView(view);
                }
            }
        }

        if (updateMode == 2 || updateMode == 0) {
            if (weather.aqiInfo.aqi != null) {
                aqiText.setText(weather.aqiInfo.aqi.aqi);
                pm25Text.setText(weather.aqiInfo.aqi.pm25);
                airQualityTitle.setText(weather.aqiInfo.aqi.qlty);
            }
        }

        weatherLayout.setVisibility(View.VISIBLE);
        Intent intent = new Intent(this, AutoUpdateService.class);
        startService(intent);
    }

    private void requestWeatherInfo(final Weather weather,String weatherUrl){
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
                weather.weatherInfo = Utility.handleWeatherInfoResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather.weatherInfo != null && "ok".equals(weather.weatherInfo.status)){
                            SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather",responseText);
                            editor.apply();
                            mWeatherId = weather.weatherInfo.basic.weatherId;
                            mParentCity = weather.weatherInfo.basic.parentCity;
                            showWeatherInfo(weather,1);
                        }else if(weather.weatherInfo != null && "no data for this location".equals(weather.weatherInfo.status)){
                            Toast.makeText(WeatherActivity.this, "该城市/地区没有数据", Toast.LENGTH_SHORT).show();
                        }else if(weather.weatherInfo != null && "no more requests".equals(weather.weatherInfo.status)){
                            Toast.makeText(WeatherActivity.this, "当日API访问次数已达上限", Toast.LENGTH_SHORT).show();
                        }else if(weather.weatherInfo != null && "dead".equals(weather.weatherInfo.status)){
                            Toast.makeText(WeatherActivity.this, "请求无响应/超时", Toast.LENGTH_SHORT).show();
                        }else{
                            Log.d("天气信息",responseText);
                            Toast.makeText(WeatherActivity.this, "未知天气解析错误", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    private void requestWeatherAQI(final Weather weather, final String AQIUrl){
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
                weather.aqiInfo = Utility.handleAQIInfoResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather.aqiInfo != null && "ok".equals(weather.aqiInfo.status)){
                            SharedPreferences.Editor editor = PreferenceManager
                                    .getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("aqi",responseText);
                            editor.apply();
                            showWeatherInfo(weather,2);
                        }else if(weather.aqiInfo != null && "no data for this location".equals(weather.aqiInfo.status)){
                            Toast.makeText(WeatherActivity.this, "该城市/地区没有数据", Toast.LENGTH_SHORT).show();
                        }else if(weather.aqiInfo != null && "no more requests".equals(weather.aqiInfo.status)){

                        }else if(weather.aqiInfo != null && "dead".equals(weather.aqiInfo.status)){
                            Toast.makeText(WeatherActivity.this, "请求无响应/超时", Toast.LENGTH_SHORT).show();
                        }else{
                            Log.d("天气信息",responseText);
                            Toast.makeText(WeatherActivity.this, "未知空气解析错误", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    /**
     * 根据天气id请求城市天气信息
     */
    public void requestWeather(final String weatherId,final String parent_city){

        //Log.d("父城市", parent_city);
        String parametersWeather = "location=" + weatherId + "&key=205adaf1dd184d2eaa2327b33bfcb467";
        String weatherUrl = "https://free-api.heweather.net/s6/weather?" + parametersWeather;
        String parametersWeatherAQI = "location=" + parent_city + "&key=205adaf1dd184d2eaa2327b33bfcb467";
        String AQIUrl = "https://free-api.heweather.net/s6/air/now?" + parametersWeatherAQI;

        final Weather weather = new Weather();

        requestWeatherInfo(weather,weatherUrl);
        requestWeatherAQI(weather,AQIUrl);
        swipeRefresh.setRefreshing(false);

    }

    @Override
    public void onClick(View v) {
        Log.d("点击事件", "onclick");
        switch (v.getId()){
            case R.id.nav_button: {
                drawerLayout.openDrawer(GravityCompat.START);
                break;
            }

            case R.id.title_menu: {
                final PopupMenu popupMenu = new PopupMenu(WeatherActivity.this,v);
                popupMenu.getMenuInflater().inflate(R.menu.menu, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()){
                            case R.id.menu_save: {
                                Log.d("点击事件", "保存图片");
                                saveBingPicToLocal();
                                return true;
                            }

                            default:{
                                return false;
                            }
                        }
                    }
                });
                popupMenu.show();
            }

            default:
        }
    }

    private void saveBingPicToLocal() {
        if(downloadBinder == null){
            Log.d("开始下载", "downloadBinder为空");
            return;
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String bingPic = prefs.getString("bing_pic", null);
        if(bingPic == null){
            loadBingPic(2);
            return;
        }

        Log.d("开始下载", "下载图片");
        Log.d("开始下载", downloadBinder.toString());
        downloadBinder.startDownload(bingPic);

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(connection);
    }
}
