package com.zero.coolweather.db;

import org.litepal.crud.DataSupport;

public class HotCity extends DataSupport {

    private int id;

    private String cityName;

    private String weatherId;

    public String getParentCity() {
        return parentCity;
    }

    public void setParentCity(String parentCity) {
        this.parentCity = parentCity;
    }

    private String parentCity;

    public void setId(int id) {
        this.id = id;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public void setWeatherId(String weatherId) {
        this.weatherId = weatherId;
    }

    public int getId() {

        return id;
    }

    public String getCityName() {
        return cityName;
    }

    public String getWeatherId() {
        return weatherId;
    }
}
