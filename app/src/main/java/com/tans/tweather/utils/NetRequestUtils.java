package com.tans.tweather.utils;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.Volley;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tans.tweather.bean.weather.AtmosphereBean;
import com.tans.tweather.bean.weather.ConditionBean;
import com.tans.tweather.bean.weather.ForecastBean;
import com.tans.tweather.bean.weather.WindBean;
import com.tans.tweather.interfaces.HttpRequestUtils;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import com.tans.tweather.interfaces.HttpRequestUtils.NetRequestListener;

/**
 * Created by tans on 2017/11/19.
 */

public class NetRequestUtils {

    private static NetRequestUtils instance = null;

    private Context mContext = null;
    private RequestQueue mQueue = null;

    public static NetRequestUtils newInstance() {
        if (instance == null) {
            instance = new NetRequestUtils();
        }
        return instance;
    }

    public void setContext(Context context) {
        this.mContext = context;
        mQueue = Volley.newRequestQueue(mContext);
    }

    public boolean isNetWorkAvailable() {
        ConnectivityManager connectivity = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo info = connectivity.getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                // 当前网络是连接的
                if (info.getState() == NetworkInfo.State.CONNECTED) {
                    // 当前所连接的网络可用
                    return true;
                }
            }
        }
        return false;
    }

    public void requestLocationInfo(final HttpRequestUtils.NetRequestListener listener, double [] location) {
        if (location == null) {
            VolleyError e = new VolleyError();
            listener.onFail(e.getMessage());
            return;
        }
        Response.Listener<String> responseListener = new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {

                listener.onSuccess(ResponseConvertUtils.getCurrentCityString(response));
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onFail(error.getMessage());
            }
        };
        requestNet(UrlUtils.getBaiduLocationUrl(location), responseListener, errorListener);
    }

    public void requestAtmosphereInfo(String location, final NetRequestListener listener) {
        Response.Listener<String> responseListener = new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                String resultString = ResponseConvertUtils.getAtmosphereJsonString(response);
                Gson gson = new Gson();
                AtmosphereBean result = gson.fromJson(resultString, AtmosphereBean.class);
                listener.onSuccess(result);
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onFail(error.getMessage());
            }
        };
        requestNet(UrlUtils.getWeatherAtomosphereUrl(location), responseListener, errorListener);
    }

    public void requestConditionInfo(String location, final NetRequestListener listener) {
        Response.Listener<String> responseListener = new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                String resultString = ResponseConvertUtils.getCurrentConditionJsonString(response);
                Gson gson = new Gson();
                ConditionBean result = gson.fromJson(resultString, ConditionBean.class);
                listener.onSuccess(result);
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onFail(error.getMessage());
            }
        };
        requestNet(UrlUtils.getWeatherConditionUrl(location), responseListener, errorListener);
    }

    public void requestForecastInfo(String location, final NetRequestListener listener) {

        Response.Listener<String> responseListener = new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                String resultString = ResponseConvertUtils.getFutureConditionJsonString(response);
                Gson gson = new Gson();
                List<ForecastBean> result = gson.fromJson(resultString, new TypeToken<ArrayList<ForecastBean>>() {
                }.getType());
                listener.onSuccess(result);
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onFail(error.getMessage());
            }
        };
        requestNet(UrlUtils.getWeatherForecastUrl(location), responseListener, errorListener);
    }

    public void requestWindInfo(String location, final NetRequestListener listener) {
        Response.Listener<String> responseListener = new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {
                String resultString = ResponseConvertUtils.getWindJsonString(response);
                Gson gson = new Gson();
                WindBean result = gson.fromJson(resultString, WindBean.class);
                listener.onSuccess(result);
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onFail(error.getMessage());
            }
        };
        requestNet(UrlUtils.getWeatherWindUrl(location), responseListener, errorListener);
    }


    public void requestCitiesInfo(final NetRequestListener listener, String parentCityCode) {
        Response.Listener<String> responseListener = new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                listener.onSuccess(response);
            }
        };
        Response.ErrorListener errorListener = new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                listener.onFail(error.getMessage());
            }
        };
        requestNet(UrlUtils.getChinaCitiesUrl(parentCityCode), responseListener, errorListener);
    }

    private NetRequestUtils() {

    }

    /**
     * http请求
     *
     * @param url      地址
     * @param response 成功监听
     * @param error    错误监听
     */
    private void requestNet(String url, Response.Listener<String> response, Response.ErrorListener error) {
        Log.i("URL", url);
        StringRequest request = new StringRequest(url, response, error);
//        request.setRetryPolicy(new DefaultRetryPolicy(
//                30*1000,
//                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
//                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        mQueue.add(request);
    }

    private class StringRequest extends Request<String> {
        private Response.Listener<String> mListener;

        /**
         * Creates a new request with the given method.
         *
         * @param method        the request {@link Method} to use
         * @param url           URL to fetch the string at
         * @param listener      Listener to receive the String response
         * @param errorListener Error listener, or null to ignore errors
         */
        public StringRequest(int method, String url, Response.Listener<String> listener,
                             Response.ErrorListener errorListener) {
            super(method, url, errorListener);
            mListener = listener;
        }

        /**
         * Creates a new GET request.
         *
         * @param url           URL to fetch the string at
         * @param listener      Listener to receive the String response
         * @param errorListener Error listener, or null to ignore errors
         */
        public StringRequest(String url, Response.Listener<String> listener, Response.ErrorListener errorListener) {
            this(Method.GET, url, listener, errorListener);
        }

        @Override
        protected void deliverResponse(String response) {
            if (mListener != null) {
                mListener.onResponse(response);
            }
        }

        @Override
        protected Response<String> parseNetworkResponse(NetworkResponse response) {
            String parsed;
            try {
                parsed = new String(response.data, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                parsed = new String(response.data);
            }
            return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
        }
    }

}
