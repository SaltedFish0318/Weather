package com.zero.coolweather.gson;

import com.google.gson.annotations.SerializedName;

/**
 * Created by 86738 on 2019/1/20.
 */

public class Now {

    @SerializedName("tmp")
    public String temperature;

    public String cond_txt;

}
