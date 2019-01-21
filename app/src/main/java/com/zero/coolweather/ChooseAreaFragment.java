package com.zero.coolweather;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.zero.coolweather.db.City;
import com.zero.coolweather.db.Country;
import com.zero.coolweather.db.HotCity;
import com.zero.coolweather.db.MyCity;
import com.zero.coolweather.db.Province;
import com.zero.coolweather.gson.HotCityGSON;
import com.zero.coolweather.gson.QueryCity;
import com.zero.coolweather.util.HttpUtil;
import com.zero.coolweather.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;
import okhttp3.internal.Util;

/**
 * Created by 86738 on 2019/1/19.
 */

public class ChooseAreaFragment extends Fragment {

    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTRY = 2;

    private boolean FLAG_LOCATION_GET = false;

    public LocationClient mLocationClient;

    private ProgressDialog progressDialog;

    private TextView titleText;

    private Button backButton;

    private Button turnButton;

    private RelativeLayout queryCityRL;

    private RelativeLayout myCityRL;

    private EditText chooseAreaEdit;

    private ListView listView;

    private ListView myCityListView;

    private ArrayAdapter<String> adapter;

    private ArrayAdapter<String> myCityAdapter;

    private List<String> dataList = new ArrayList<>();

    private List<String> myDataList = new ArrayList<>();

    public String myPosition;

    /**
     * 我的城市列表
     */
    private List<MyCity> myCityList;

    /**
     * 热门城市列表
     */
    private List<HotCity> hotCityList;

    /**
     * 查询城市列表
     */
    private List<QueryCity.Basic> queryCityList;

    /**
     * 省列表
     */
    private List<Province> provinceList;

    /**
     * 市列表
     */
    private List<City> cityList;

    /**
     * 县列表
     */
    private List<Country> countryList;

    /**
     * 选中的省份
     */
    private Province selectedProvince;

    /**
     * 选中的城市
     */
    private City selectedCity;

    /**
     * 当前选中的级别
     */
    private int currentLevel;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.choose_area,container,false);
        mLocationClient = new LocationClient(getActivity().getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        titleText = (TextView) view.findViewById(R.id.title_text);
        backButton = (Button) view.findViewById(R.id.back_button);
        backButton.setVisibility(View.GONE);
        listView = (ListView) view.findViewById(R.id.list_view);
        turnButton = (Button) view.findViewById(R.id.turn_button);
        myCityRL = (RelativeLayout) view.findViewById(R.id.my_city_list);
        queryCityRL = (RelativeLayout) view.findViewById(R.id.query_city_list);
        myCityListView = (ListView) view.findViewById(R.id.my_city_list_view);
        chooseAreaEdit = (EditText) view.findViewById(R.id.choose_area_edit);
        adapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,dataList);
        myCityAdapter = new ArrayAdapter<>(getContext(),android.R.layout.simple_list_item_1,myDataList);
        listView.setAdapter(adapter);
        myCityListView.setAdapter(myCityAdapter);
        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);

        myCityListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String weatherId = myCityList.get(position).getCityName();
                WeatherActivity activity = (WeatherActivity) getActivity();
                activity.drawerLayout.closeDrawers();
                activity.swipeRefresh.setRefreshing(true);
                activity.requestWeather(weatherId);
            }
        });

        myCityListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String deleteCityName = myCityList.get(position).getCityName();
                DataSupport.deleteAll(MyCity.class, "cityName=?", deleteCityName);
                loadMyCityListView();
                Toast.makeText(getActivity(), deleteCityName + " 删除成功", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                /*
                if(currentLevel == LEVEL_PROVINCE){
                    selectedProvince = provinceList.get(position);
                    queryCities();
                }else if(currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    queryCountries();
                } else if (currentLevel == LEVEL_COUNTRY) {
                    String weatherId = countryList.get(position).getWeatherId();
                    if (getActivity() instanceof MainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    } else if (getActivity() instanceof WeatherActivity) {
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }
                }
                */

                String weatherId = "";
                if (chooseAreaEdit.getText().toString().equals("")) {
                    if (position != 0) {
                        weatherId = hotCityList.get(position - 1).getWeatherId();
                    } else {
                        if (FLAG_LOCATION_GET) {
                            weatherId = myPosition;
                            Toast.makeText(getActivity(), "正在获取" + myPosition + "的天气信息", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getActivity(), "位置暂未获取，请稍候", Toast.LENGTH_SHORT).show();
                        }


                    }
                } else {
                    weatherId = queryCityList.get(position).cid;
                }

                if (!TextUtils.isEmpty(weatherId)) {
                    if (getActivity() instanceof MainActivity) {
                        Intent intent = new Intent(getActivity(), WeatherActivity.class);
                        intent.putExtra("weather_id", weatherId);
                        startActivity(intent);
                        getActivity().finish();
                    } else if (getActivity() instanceof WeatherActivity) {
                        WeatherActivity activity = (WeatherActivity) getActivity();
                        activity.drawerLayout.closeDrawers();
                        activity.swipeRefresh.setRefreshing(true);
                        activity.requestWeather(weatherId);
                    }
                }
            }
        });

        turnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (myCityRL.getVisibility() == View.GONE) {
                    myCityRL.setVisibility(View.VISIBLE);
                    queryCityRL.setVisibility(View.GONE);
                    chooseAreaEdit.setVisibility(View.GONE);
                    titleText.setText("我的");
                    loadMyCityListView();
                }else{
                    queryCityRL.setVisibility(View.VISIBLE);
                    myCityRL.setVisibility(View.GONE);
                    chooseAreaEdit.setVisibility(View.VISIBLE);
                    titleText.setText("");
                    queryHotCity();
                }
            }
        });

        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel == LEVEL_CITY){
                    queryProvinces();
                }else if (currentLevel == LEVEL_COUNTRY){
                    queryCities();
                }
            }
        });

        chooseAreaEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                if (!chooseAreaEdit.getText().toString().equals("")) {
                    queryCityByKeyWord(chooseAreaEdit.getText().toString());
                } else {
                    queryHotCity();
                }


            }
        });

        //queryProvinces();
        queryHotCity();
        myCityRL.setVisibility(View.GONE);
        requestMyPosition();
        //loadMyCityListView();
    }

    private void requestMyPosition() {
        initLocation();
        mLocationClient.start();
    }

    private void initLocation() {
        LocationClientOption option = new LocationClientOption();
        option.setIsNeedAddress(true);
        option.setScanSpan(2*60*1000);
        mLocationClient.setLocOption(option);
    }

    public void loadMyCityListView() {

        myCityList = DataSupport.findAll(MyCity.class);
        if (myCityList.size() > 0) {
            myDataList.clear();
            for (MyCity myCity : myCityList) {
                myDataList.add(myCity.getCityName());
            }
            myCityAdapter.notifyDataSetChanged();
        }

    }

    /**
     * 查找城市
     */
    private void queryCityByKeyWord(String keyWord) {
        String parameters = "location=" + keyWord +  "&key=205adaf1dd184d2eaa2327b33bfcb467";
        String address = "https://search.heweather.net/find?" + parameters;
        queryFromServer(address,"queryCity");
    }

    /**
     * 查找热门城市
     */
    private void queryHotCity() {

        hotCityList = DataSupport.findAll(HotCity.class);
        if (hotCityList.size() > 0) {
            dataList.clear();
            dataList.add("我的位置");
            for (HotCity hotCity : hotCityList) {
                dataList.add(hotCity.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
        } else {
            String parameters = "group=cn" +  "&key=205adaf1dd184d2eaa2327b33bfcb467";
            String hotCityUrl = "https://search.heweather.net/top?" + parameters;
            queryFromServer(hotCityUrl,"hotCity");
        }

    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryProvinces() {
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);
        provinceList = DataSupport.findAll(Province.class);
        if (provinceList.size() > 0) {
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        }else{
            String address = "http://guolin.tech/api/china";
            queryFromServer(address, "province");
        }
    }

    /**
     * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = DataSupport.where("provinceId = ?",
                String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    /**
     * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryCountries() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countryList = DataSupport.where("cityId = ?",
                String.valueOf(selectedCity.getId())).find(Country.class);
        if (countryList.size() > 0) {
            dataList.clear();
            for (Country country : countryList) {
                dataList.add(country.getCountryName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_COUNTRY;
        }else{
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();
            String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
            queryFromServer(address, "country");
        }
    }

    /**
     * 根据传入的地址和类型从服务器上查询省市县数据
     */
    private void queryFromServer(String address, final String type) {
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                Log.d("66666666666666666", responseText);
                boolean result = true;
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponce(responseText);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponce(responseText,selectedProvince.getId());
                } else if ("country".equals(type)) {
                    result = Utility.handleCountryResponce(responseText,selectedCity.getId());
                } else if ("hotCity".equals(type)) {
                    result = Utility.handleHotCityResonse(responseText);
                } else if ("queryCity".equals(type)) {
                    QueryCity queryCity = Utility.handleQueryCityResonse(responseText);
                    queryCityList = queryCity.basic;
                    if (queryCity.status.equals("ok")) {
                        Log.d("66666666666666666", "true");
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                dataList.clear();
                                for (QueryCity.Basic query : queryCityList) {
                                    dataList.add(query.location);
                                }
                                adapter.notifyDataSetChanged();
                                listView.setSelection(0);
                            }
                        });

                        result = true;
                    }else{
                        Log.d("66666666666666666", "false");
                        result = false;
                    }
                }

                if (result) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if ("province".equals(type)) {
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("country".equals(type)) {
                                queryCountries();
                            } else if ("hotCity".equals(type)) {
                                queryHotCity();
                            } else if ("queryCity".equals(type)) {

                            }
                        }
                    });
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                    }
                });
            }
        });
    }

    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    private void showProgressDialog() {
        if(progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    public class MyLocationListener implements BDLocationListener{
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            myPosition = bdLocation.getDistrict();

            if (myPosition != null && !myPosition.equals("")) {
                FLAG_LOCATION_GET = true;
            } else {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity(), "请允许该应用使用位置权限", Toast.LENGTH_SHORT).show();
                    }
                });
            } 
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();

        if (mLocationClient.isStarted()) {
            mLocationClient.stop();
        }
    }
}
