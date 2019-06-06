package com.zero.coolweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by 86738 on 2019/1/20.
 */

public class Basic {

    @SerializedName("location")
    public String cityName;

    @SerializedName("cid")
    public String weatherId;

    @SerializedName("parent_city")
    public String parentCity;

}
