package com.zero.coolweather;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
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

    //位置已获取
    private boolean FLAG_LOCATION_GET = false;

    //位置
    public LocationClient mLocationClient;

    //进度条
    private ProgressDialog progressDialog;

    //标题
    private TextView titleText;

    //返回按钮
    //private Button backButton;

    //切换按钮
    private Button turnButton;

    private RelativeLayout queryCityRL;

    private RelativeLayout myCityRL;

    //输入框
    private EditText chooseAreaEdit;

    //城市列表
    private ListView listView;

    //我的城市列表
    private ListView myCityListView;

    //城市适配器
    private ArrayAdapter<String> adapter;

    //我的城市适配器
    private ArrayAdapter<String> myCityAdapter;

    private List<String> dataList = new ArrayList<>();

    private List<String> myDataList = new ArrayList<>();

    public String myPosition;
    public String myPositionCity;

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

    public boolean isFirstTimeGetLocation;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState){
        View view = inflater.inflate(R.layout.choose_area,container,false);
        Log.d("碎片创建","View");
        mLocationClient = new LocationClient(getActivity().getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        titleText = (TextView) view.findViewById(R.id.title_text);
//        backButton = (Button) view.findViewById(R.id.back_button);
//        backButton.setVisibility(View.GONE);
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

    public boolean isLocationStarted(){
        return mLocationClient.isStarted();
    }

    public void restartLocation(){
        //isFirstTimeGetLocation = true;
        mLocationClient.restart();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Log.d("活动创建","碎片创建");

        myCityListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String weatherId = myCityList.get(position).getCityName();
                WeatherActivity activity = (WeatherActivity) getActivity();
                activity.drawerLayout.closeDrawers();
                activity.swipeRefresh.setRefreshing(true);
                activity.requestWeather(weatherId,myCityList.get(position).getParentCity());
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

                String weatherId = "";
                String parentCity = "";
                int flag = 0;
                if (chooseAreaEdit.getText().toString().equals("")) {
                    if (position != 0) {
                        weatherId = hotCityList.get(position - 1).getWeatherId();
                        parentCity = hotCityList.get(position - 1).getParentCity();
                    } else {
                        if(!mLocationClient.isStarted())
                            mLocationClient.start();

                        Log.d("选择使用定位","选择定位");
                        showProgressDialog();
                        flag = 1;
                    }
                } else {
                    weatherId = queryCityList.get(position).cid;
                    parentCity = queryCityList.get(position).parent_city;
                    chooseAreaEdit.setText("");
                }

                if(flag == 0)
                    turnToWeather(weatherId,parentCity);
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

    private void turnToWeather(String weatherId,String parentCity){
        if (!TextUtils.isEmpty(weatherId)) {
            if (getActivity() instanceof MainActivity) {
                Log.d("页面跳转",weatherId);
                Intent intent = new Intent(getActivity(), WeatherActivity.class);
                intent.putExtra("weather_id", weatherId);
                intent.putExtra("parent_city",parentCity);
                startActivity(intent);
                getActivity().finish();
            } else if (getActivity() instanceof WeatherActivity) {
                WeatherActivity activity = (WeatherActivity) getActivity();
                activity.drawerLayout.closeDrawers();
                activity.swipeRefresh.setRefreshing(true);
                activity.requestWeather(weatherId,parentCity);
            }
        }
    }

    public void requestMyPosition() {
        initLocation();
    }

    private void initLocation() {
        isFirstTimeGetLocation = true;
        LocationClientOption option = new LocationClientOption();
        option.setIsNeedAddress(true);
        //option.setScanSpan(2*60*1000);
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
            dataList.add("定位获取");
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
     * 根据传入的地址和类型从服务器上查询省市县数据
     */
    private void queryFromServer(String address, final String type) {
        //showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        //closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();
                boolean result = true;

                if ("hotCity".equals(type)) {
                    result = Utility.handleHotCityResponse(responseText);
                } else if ("queryCity".equals(type)) {
                    QueryCity queryCity = Utility.handleQueryCityResponse(responseText);
                    queryCityList = queryCity.basic;
                    if (queryCity.status.equals("ok")) {
                        Log.d("66666666666666666", "true");
                        dataList.clear();
                        for (QueryCity.Basic query : queryCityList) {
                            dataList.add(query.location);
                        }
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
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
                            if ("hotCity".equals(type)) {
                                queryHotCity();
                            }
                        }
                    });
                }

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
            progressDialog.setMessage("正在获取位置");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    public class MyLocationListener implements BDLocationListener{
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            myPosition = bdLocation.getDistrict();
            if (mLocationClient.isStarted()) {
                mLocationClient.stop();
            }

            if (myPosition != null && !myPosition.equals("")) {
                Log.d("定位成功",myPosition);
                //FLAG_LOCATION_GET = true;
                myPositionCity = bdLocation.getCity();
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getActivity(), "已定位至：" + myPosition, Toast.LENGTH_SHORT).show();
                        turnToWeather(myPosition,myPositionCity);
                    }
                });
            } else {
                //FLAG_LOCATION_GET = false;
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        if(ContextCompat.checkSelfPermission(getActivity(),Manifest.permission.ACCESS_COARSE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(getActivity(), "定位失败！", Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(getActivity(), "请允许使用定位权限！", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if(mLocationClient.isStarted()){
            mLocationClient.stop();
        }
    }
}
