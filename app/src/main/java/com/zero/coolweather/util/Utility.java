package com.zero.coolweather.util;

import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.zero.coolweather.db.City;
import com.zero.coolweather.db.Country;
import com.zero.coolweather.db.HotCity;
import com.zero.coolweather.db.Province;
import com.zero.coolweather.gson.HotCityGSON;
import com.zero.coolweather.gson.QueryCity;
import com.zero.coolweather.gson.Weather;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by 86738 on 2019/1/19.
 */

public class Utility {

    /**
     * 处理服务器返回的热门城市数据
     */
    public static boolean handleHotCityResponse(String response){
        try{
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather6");
            String weatherContent = jsonArray.getJSONObject(0).toString();
            HotCityGSON hotCityGSON = new Gson().fromJson(weatherContent,HotCityGSON.class);
            for (int i = 0; i < hotCityGSON.basic.size(); i++){
                HotCity hotCity = new HotCity();
                hotCity.setWeatherId(hotCityGSON.basic.get(i).cid);
                hotCity.setCityName(hotCityGSON.basic.get(i).location);
                hotCity.setParentCity(hotCityGSON.basic.get(i).parent_city);
                hotCity.save();
            }
            return true;
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 处理服务器的查询城市的列表数据
     */
    public static QueryCity handleQueryCityResponse(String response){
        try{
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather6");
            String weatherContent = jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherContent,QueryCity.class);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将返回的JSON数据解析成Weather实体类
     */
    public static Weather handleWeatherResponse(String weatherResponse, String aqiResponse) {
        Weather weather = new Weather();
        weather.aqiInfo = handleAQIInfoResponse(aqiResponse);
        weather.weatherInfo = handleWeatherInfoResponse(weatherResponse);
        return weather;
    }


    /**
     * 将返回的JSON数据解析成Weather.AQIInfo实体类
     */
    public static Weather.AQIInfo handleAQIInfoResponse(String response){
        try{
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather6");
            String weatherContent = jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherContent,Weather.AQIInfo.class);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 将返回的JSON数据解析成Weather.WeatherInfo实体类
     */
    public static Weather.WeatherInfo handleWeatherInfoResponse(String response){
        try{
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather6");
            String weatherContent = jsonArray.getJSONObject(0).toString();
            return new Gson().fromJson(weatherContent,Weather.WeatherInfo.class);
        }catch (Exception e){
            e.printStackTrace();
        }
        return null;
    }

}
