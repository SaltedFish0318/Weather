package com.zero.coolweather.gson;

import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Created by 86738 on 2019/1/20.
 */

public class Weather {

    public static WeatherInfo weatherInfo;

    public class WeatherInfo{

        public Basic basic;

        @SerializedName("daily_forecast")
        public List<Forecast> forecastList;

        @SerializedName("lifestyle")
        public List<Suggestion> lifestyleList;

        @SerializedName("hourly")
        public List<HourlyForecast> hourlyForecastList;

        public Now now;

        public String status;

        public Update update;

    }

    public static AQIInfo aqiInfo;

    public class AQIInfo{

        @SerializedName("air_now_city")
        public AQI aqi;

        public String status;

    }
}
