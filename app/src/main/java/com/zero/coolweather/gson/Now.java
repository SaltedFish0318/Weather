package com.zero.coolweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by 86738 on 2019/1/20.
 */

public class Now {

    //温度
    @SerializedName("tmp")
    public String temperature;

    //体感温度
    @SerializedName("fl")
    public String fellTmp;

    //天气代码
    public String cond_code;

    //天气描述
    public String cond_txt;

    //风向
    public String wind_dir;

    //风力
    public String wind_sc;

    //风速
    public String wind_spd;

    //相对湿度
    public String hum;

    //大气压强
    public String pres;

    //能见度
    public String vis;

    //云量
    public String cloud;
}
