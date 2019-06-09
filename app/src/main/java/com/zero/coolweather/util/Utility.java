package com.zero.coolweather.util;

import com.google.gson.Gson;
import com.zero.coolweather.db.HotCity;
import com.zero.coolweather.gson.HotCityGSON;
import com.zero.coolweather.gson.QueryCity;
import com.zero.coolweather.gson.Weather;

import org.json.JSONArray;
import org.json.JSONObject;

public class Utility {

    /**
     * 处理服务器返回的热门城市数据
     * @param response
     * @return
     */
    public static boolean handleHotCityResponse(String response){
        try{
            //将JSON转为实例
            JSONObject jsonObject = new JSONObject(response);
            JSONArray jsonArray = jsonObject.getJSONArray("HeWeather6");
            String weatherContent = jsonArray.getJSONObject(0).toString();
            HotCityGSON hotCityGSON = new Gson().fromJson(weatherContent,HotCityGSON.class);
            for (int i = 0; i < hotCityGSON.basic.size(); i++){
                //存入数据库
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
     * @param response
     * @return
     */
    public static QueryCity handleQueryCityResponse(String response){
        try{
            //将JSON转为实例
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
     * @param weatherResponse
     * @param aqiResponse
     * @return
     */
    public static Weather handleWeatherResponse(String weatherResponse, String aqiResponse) {
        Weather weather = new Weather();
        weather.aqiInfo = handleAQIInfoResponse(aqiResponse);
        weather.weatherInfo = handleWeatherInfoResponse(weatherResponse);
        return weather;
    }

    /**
     * 将返回的JSON数据解析成Weather.AQIInfo实体类
     * @param response
     * @return
     */
    public static Weather.AQIInfo handleAQIInfoResponse(String response){
        try{
            //将JSON转为实例
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
     * @param response
     * @return
     */
    public static Weather.WeatherInfo handleWeatherInfoResponse(String response){
        try{
            //将JSON转为实例
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
