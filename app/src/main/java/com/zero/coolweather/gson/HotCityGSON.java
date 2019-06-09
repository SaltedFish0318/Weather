package com.zero.coolweather.gson;

import org.litepal.crud.DataSupport;

import java.util.List;

public class HotCityGSON extends DataSupport {

    public List<Basic> basic;

    public class Basic{

        public String cid;

        public String location;

        public String parent_city;

    }

}
