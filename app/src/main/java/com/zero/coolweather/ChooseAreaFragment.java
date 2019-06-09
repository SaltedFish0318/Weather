package com.zero.coolweather;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
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
import com.zero.coolweather.db.HotCity;
import com.zero.coolweather.db.MyCity;
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

public class ChooseAreaFragment extends Fragment {

    //位置
    public LocationClient mLocationClient;

    //进度条
    private ProgressDialog progressDialog;

    //标题
    private TextView titleText;

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

    //城市列表List
    private List<String> dataList = new ArrayList<>();

    //我的天气列表List
    private List<String> myDataList = new ArrayList<>();

    //我的位置
    public String myPosition;

    //我的位置所在父级城市
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

    /**
     * 判断位置服务开启
     * @return
     */
    public boolean isLocationStarted(){
        return mLocationClient.isStarted();
    }

    /**
     * 重启位置服务
     */
    public void restartLocation(){
        mLocationClient.restart();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState){
        super.onActivityCreated(savedInstanceState);
        Log.d("活动创建","碎片创建");

        //我的城市列表点击监听器
        myCityListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //天气查询
                String weatherId = myCityList.get(position).getCityName();
                WeatherActivity activity = (WeatherActivity) getActivity();
                activity.drawerLayout.closeDrawers();
                activity.swipeRefresh.setRefreshing(true);
                activity.requestWeather(weatherId,myCityList.get(position).getParentCity());
            }
        });

        //我的城市列表长按监听器
        myCityListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                //删除城市
                String deleteCityName = myCityList.get(position).getCityName();
                DataSupport.deleteAll(MyCity.class, "cityName=?", deleteCityName);
                loadMyCityListView();
                Toast.makeText(getActivity(), deleteCityName + " 删除成功", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        //城市列表点击监听器
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

                String weatherId = "";
                String parentCity = "";
                int flag = 0;
                if (chooseAreaEdit.getText().toString().equals("")) {
                    //搜索框为空
                    if (position != 0) {
                        //没有点击定位获取
                        weatherId = hotCityList.get(position - 1).getWeatherId();
                        parentCity = hotCityList.get(position - 1).getParentCity();
                    } else {
                        //点击定位获取
                        if(!mLocationClient.isStarted())
                            mLocationClient.start();    //开启定位
                        else
                            mLocationClient.requestLocation(); //请求位置

                        Log.d("选择使用定位","选择定位");
                        showProgressDialog();   //等待框显示
                        flag = 1;
                    }
                } else {
                    //搜索框不为空，重搜索的列表中查询
                    weatherId = queryCityList.get(position).cid;
                    parentCity = queryCityList.get(position).parent_city;
                    chooseAreaEdit.setText("");
                }

                if(flag == 0)   //没有使用定位，跳转
                    turnToWeather(weatherId,parentCity);
            }
        });

        //我的城市和第一页城市的切换按钮
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

        //搜索框改变后自动查询输入内容的城市
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

        queryHotCity();
        myCityRL.setVisibility(View.GONE);
        initLocation();
    }

    /**
     * 通过城市天气ID，父级城市来获取天气和空气信息
     * @param weatherId
     * @param parentCity
     */
    private void turnToWeather(String weatherId,String parentCity){
        if (!TextUtils.isEmpty(weatherId)) {
            if (getActivity() instanceof MainActivity) {
                //如果在初始的MainActivity则需要跳转到WeatherActivity活动
                Log.d("页面跳转",weatherId);
                Intent intent = new Intent(getActivity(), WeatherActivity.class);
                intent.putExtra("weather_id", weatherId);
                intent.putExtra("parent_city",parentCity);
                startActivity(intent);
                getActivity().finish();
            } else if (getActivity() instanceof WeatherActivity) {
                //如果在WeatherActivity活动则直接查询
                WeatherActivity activity = (WeatherActivity) getActivity();
                activity.drawerLayout.closeDrawers();
                activity.swipeRefresh.setRefreshing(true);
                activity.requestWeather(weatherId,parentCity);
            }
        }
    }

    /**
     * 初始化位置
     */
    private void initLocation() {
        isFirstTimeGetLocation = true;
        LocationClientOption option = new LocationClientOption();
        option.setIsNeedAddress(true);
        mLocationClient.setLocOption(option);
    }

    /**
     * 从数据库中加载我的城市
     */
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
     * @param keyWord
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
            //若数据库中存在数据
            dataList.clear();
            dataList.add("定位获取");
            for (HotCity hotCity : hotCityList) {
                dataList.add(hotCity.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
        } else {
            //数据库中没有数据，调用查找
            String parameters = "group=cn" +  "&key=205adaf1dd184d2eaa2327b33bfcb467";
            String hotCityUrl = "https://search.heweather.net/top?" + parameters;
            queryFromServer(hotCityUrl,"hotCity");
        }

    }

    /**
     * 根据传入的地址和类型从服务器上查询省市县数据
     * @param address
     * @param type
     */
    private void queryFromServer(String address, final String type) {
        //showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //请求成功
                String responseText = response.body().string();
                boolean result = true;

                if ("hotCity".equals(type)) {
                    //请求类型为热门城市
                    result = Utility.handleHotCityResponse(responseText);
                } else if ("queryCity".equals(type)) {
                    //请求类型为城市搜索
                    QueryCity queryCity = Utility.handleQueryCityResponse(responseText);
                    queryCityList = queryCity.basic;
                    if (queryCity.status.equals("ok")) {
                        //返回JSON成功
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
                        result = false;
                    }
                }

                if (result) {
                    //在主线程中更新界面
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

    /**
     * 关闭等待框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

    /**
     * 显示等待框
     */
    private void showProgressDialog() {
        if(progressDialog == null){
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在获取位置");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 天气获取监听器
     */
    public class MyLocationListener implements BDLocationListener{
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            myPosition = bdLocation.getDistrict();

            if (myPosition != null && !myPosition.equals("")) {
                Log.d("定位成功",myPosition);
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
        //介绍位置获取
        if(mLocationClient.isStarted()){
            mLocationClient.stop();
        }
    }
}
