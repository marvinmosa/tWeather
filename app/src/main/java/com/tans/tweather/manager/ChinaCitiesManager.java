package com.tans.tweather.manager;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import com.j256.ormlite.dao.Dao;
import com.tans.tweather.application.BaseApplication;
import com.tans.tweather.database.DatabaseHelper;
import com.tans.tweather.database.bean.LocationBean;
import com.tans.tweather.utils.ResponseConvertUtils;
import com.tans.tweather.utils.UrlUtils;
import com.tans.tweather.utils.httprequest.BaseHttpRequestUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by mine on 2017/12/28.
 */

public class ChinaCitiesManager {

    public static String TAG = ChinaCitiesManager.class.getSimpleName();
    public static String LOAD_CURRENT_LOCATION = "current location";
    public static int ROOT_CITY_LEVEL = 1;
    public static String ROOT_CITY_PARENT_CODE = "";
    public static int END_LEVEL = 3;

    private static ChinaCitiesManager instance;
    private DatabaseHelper mDatabaseHelper = null;
    private Dao<LocationBean,String> mDao = null;
    private BaseHttpRequestUtils mHttpRequestUtils = null;
    private Context mContext = null;

    private SpManager mSpManager = null;

    private List<CommonCitesChangeListener> commonCitesChangeListeners = null;

    private List<CurrentCitesChangeListener> currentCitesChangeListeners = null;

    public interface CommonCitesChangeListener {
        void onChange();
    }

    public interface CurrentCitesChangeListener {
        void onChange();
    }

    /**
     * 当前城市信息更新的监听
     */
    public interface LoadCurrentCityListener {
        void onSuccess(String s);

        void onFail(String e);
    }

    public interface LoadChildrenCityListener {
        void onSuccess(List<LocationBean> childCities);
        void onFail(String e);
    }

    public static ChinaCitiesManager newInstance() {
        if (instance == null) {
            instance = new ChinaCitiesManager();
        }
        return instance;
    }

    public void initDependencies(SpManager spManager, BaseHttpRequestUtils httpRequestUtils) {
        mSpManager = spManager;
        mDatabaseHelper = DatabaseHelper.getHelper(BaseApplication.getInstance());
        mContext = BaseApplication.getInstance();
        mHttpRequestUtils = httpRequestUtils;
        try {
            mDao = mDatabaseHelper.getDao(LocationBean.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void queryChildrenCities(final String parentCode,final int level, final LoadChildrenCityListener listener) {
        if (level > END_LEVEL) {
            listener.onFail("error level");
        } else {
            Observable.create(new Observable.OnSubscribe<List<LocationBean>>() {

                @Override
                public void call(Subscriber<? super List<LocationBean>> subscriber) {
                    List<LocationBean> resultCities = null;
                    try {
                        resultCities = mDao.queryBuilder().where().eq("parent_code", parentCode).query();
                        subscriber.onNext(resultCities);
                    } catch (SQLException e) {
                        subscriber.onError(e);
                        e.printStackTrace();
                    }
                }
            })
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<List<LocationBean>>() {
                        @Override
                        public void onCompleted() {

                        }

                        @Override
                        public void onError(Throwable e) {
                            listener.onFail(e.getMessage());
                        }

                        @Override
                        public void onNext(List<LocationBean> locations) {
                            if (locations.size() == 0) {
                                requestCitiesInfo(parentCode, level, listener);
                            } else {
                                listener.onSuccess(locations);
                            }
                        }
                    });
        }
    }

    /**
     * 获取当前城市
     * @param listener
     */
    public void loadCurrentCity(final LoadCurrentCityListener listener) {

        if(!mHttpRequestUtils.isNetWorkAvailable()) {
            listener.onFail("网络不可用");
        } else {
            mHttpRequestUtils.request(UrlUtils.getBaiduLocationBaseUrl(),
                    UrlUtils.getBaiduLocationPath(),
                    BaseHttpRequestUtils.HttpRequestMethod.GET,
                    UrlUtils.createBaiduLocationParams(getLocation()),
                    new BaseHttpRequestUtils.HttpRequestListener<String>() {
                        @Override
                        public void onSuccess(String result) {
                            listener.onSuccess(ResponseConvertUtils.getCurrentCityString(result));
                        }
                        @Override
                        public void onFail(String e) {
                            listener.onFail(e);
                        }

                        @Override
                        public Class<String> getResultType() {
                            return String.class;
                        }
                    });
        }
    }

    public List<String> getCommonCities() {
        return mSpManager.listCommonUseCities();
    }

    public void setCommonCities(List<String> cities) {
        mSpManager.storeCommonUseCities(cities);
        notifyCommonCitesChange();
    }

    public String getCurrentCity () {

        return mSpManager.getCurrentUseCity();
    }

    public void setCurrentCity(String city) {
        mSpManager.storeCurrentUseCity(city);
        notifyCurrentCityChange();
    }

    public void registerCurrentCityChangeListener(CurrentCitesChangeListener listener) {
        if (currentCitesChangeListeners == null) {
            currentCitesChangeListeners = new ArrayList<>();
        }
        if(!currentCitesChangeListeners.contains(listener)) {
            currentCitesChangeListeners.add(listener);
        }
    }

    public void registerCommonCitesChangeListener(CommonCitesChangeListener listener) {
        if(commonCitesChangeListeners == null) {
            commonCitesChangeListeners = new ArrayList<>();
        }
        if(! commonCitesChangeListeners.contains(listener)) {
            commonCitesChangeListeners.add(listener);
        }
    }

    public void unregisterCurrentCityChangeListener(CurrentCitesChangeListener listener) {
        if(listener != null && currentCitesChangeListeners != null)
            currentCitesChangeListeners.remove(listener);
    }

    public void unregisterCommCitesChangeListener(CommonCitesChangeListener listener) {
        if(null != listener && commonCitesChangeListeners != null)
            commonCitesChangeListeners.remove(listener);
    }

    private ChinaCitiesManager () {
    }

    private void requestCitiesInfo(final String parentCityCode, final int level, final LoadChildrenCityListener listener) {
        mHttpRequestUtils.request(UrlUtils.getChinaCitiesBaseUrl(),
                UrlUtils.getChinaCitiesPath(parentCityCode),
                BaseHttpRequestUtils.HttpRequestMethod.GET,
                null,
                new BaseHttpRequestUtils.HttpRequestListener<String>() {
                    @Override
                    public void onSuccess(String result) {

                        String[] cities = splitCityString(result);
                        final List<LocationBean> locations = new ArrayList();
                        Observable.from(cities)
                                .observeOn(Schedulers.io())
                                .map(new Func1<String, Boolean>() {
                                    @Override
                                    public Boolean call(String s) {
                                        String[] city = splitCityNameAndCode(s);
                                        LocationBean locationBean = new LocationBean();
                                        locationBean.setParentCode(parentCityCode);
                                        locationBean.setLevel(level);
                                        locationBean.setCode(city[0]);
                                        locationBean.setCityName(city[1]);
                                        try {
                                            locations.add(mDao.createIfNotExists(locationBean));
                                            return true;
                                        } catch (SQLException e) {
                                            e.printStackTrace();
                                            return false;
                                        }
                                    }
                                })
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Subscriber<Boolean>() {
                                    @Override
                                    public void onCompleted() {
                                        if(listener != null) {
                                            listener.onSuccess(locations);
                                        }
                                    }

                                    @Override
                                    public void onError(Throwable e) {
                                        if(listener != null) {
                                            listener.onFail(e.getMessage());
                                        }
                                    }

                                    @Override
                                    public void onNext(Boolean aBoolean) {

                                    }
                                });
                    }

                    @Override
                    public Class<String> getResultType() {
                        return String.class;
                    }

                    @Override
                    public void onFail(String e) {

                    }
                });

    }


    private void notifyCurrentCityChange() {
        if(currentCitesChangeListeners != null) {
            for(CurrentCitesChangeListener listener : currentCitesChangeListeners) {
                listener.onChange();
            }
        }
    }

    private void notifyCommonCitesChange() {
        if(commonCitesChangeListeners != null) {
            for(CommonCitesChangeListener listener : commonCitesChangeListeners) {
                listener.onChange();
            }
        }
    }

    /**
     * 获取当前位置，需要用户开启 权限
     *
     * @return 返回精度和纬度的double数组，空的话就是没有权限。
     */
    private double[] getLocation() {
        if (mContext == null)
            return null;
        LocationManager locationManager = (LocationManager) mContext.getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = locationManager.getProviders(true);
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return null;
        }
        Location location = null;
        for(String provider: providers) {
            if(location == null) {
                location = locationManager.getLastKnownLocation(provider);
            } else {
                break;
            }
        }
        if (location == null)
            return null;
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();
        double[] result = {latitude, longitude};
        return result;
    }

    private String[] splitCityString(String s) {
        return s.split(",");
    }

    private String[] splitCityNameAndCode(String s) {
        return s.split("\\|");
    }
}
