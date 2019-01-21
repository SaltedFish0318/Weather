package com.zero.coolweather.gson;

import org.litepal.crud.DataSupport;

import java.util.List;

/**
 * Created by 86738 on 2019/1/20.
 */

public class HotCityGSON extends DataSupport {

    public List<Basic> basic;

    public class Basic{

        public String cid;

        public String location;

        public String parent_city;

    }

}
