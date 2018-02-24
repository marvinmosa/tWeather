package com.tans.tweather.manager;

import android.app.Application;
import android.content.SharedPreferences;

import com.tans.tweather.interfaces.ISpManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by tans on 2017/11/19.
 */

public class SpManager implements ISpManager {


    private String mCommonCities = null; //todo:需要转换成list
    private String mCurrentCity = null;
    private int mWallPaperAlpha = 0;
    private SharedPreferences mSp = null;

    private static SpManager instance = null;
    public static String SP_FILE_NAME = "sp_tweather"; //sp文件名字
    public static String SP_KEY_CURRENT_CITY = "current_city";//当前使用城市key
    public static String SP_KEY_COMMON_USE_CITIES = "common_use_cities";//常用城市key
    public static String SP_KEY_WALLPAPER_ALPHA = "wallpaper_alpha";//壁纸透明度key


    public static SpManager newInstance() {
        if (instance == null) {
            instance = new SpManager();
        }
        return instance;
    }

    private SpManager() {

    }

    public void initSp(Application context) {
        if (mSp == null)
            mSp = context.getSharedPreferences(SP_FILE_NAME, context.MODE_PRIVATE);
    }

    @Override
    public void storeCommonUseCities(String commonUseCities) {
        mSp.edit().putString(SP_KEY_COMMON_USE_CITIES, commonUseCities).commit();
        mCommonCities = commonUseCities;
    }

    @Override
    public Set<String> getCommonUseCities() {
        mCommonCities = mSp.getString(SP_KEY_COMMON_USE_CITIES, null);
        return null;
    }

    @Override
    public void storeCurrentUseCity(String city) {
        mSp.edit().putString(SP_KEY_CURRENT_CITY, city).commit();
        mCurrentCity = city;
    }

    @Override
    public String getCurrentUseCity() {
        mCurrentCity = mSp.getString(SP_KEY_CURRENT_CITY, "");
        return mCurrentCity;
    }

    @Override
    public void storeWallPaperAlpha(int a) {
        mSp.edit().putInt(SP_KEY_WALLPAPER_ALPHA, a);
        mWallPaperAlpha = a;
    }

    @Override
    public int getWallPaperAlpha() {
        mWallPaperAlpha = mSp.getInt(SP_KEY_WALLPAPER_ALPHA, 0);
        return mWallPaperAlpha;
    }
}
