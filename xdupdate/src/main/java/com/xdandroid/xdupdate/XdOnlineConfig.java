package com.xdandroid.xdupdate;

import android.text.TextUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Map;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by XingDa on 2016/4/25.
 */
public class XdOnlineConfig {

    protected XdOnlineConfig() {
    }
    protected static XdOnlineConfig instance;
    protected String mapUrl;
    protected OnConfigAcquiredListener l;
    protected Subscription subscription;

    public XdOnlineConfig setMapUrl(String mapUrl) {
        instance.mapUrl = mapUrl;
        return instance;
    }

    public void onDestroy() {
        if (subscription != null) subscription.unsubscribe();
    }

    public interface OnConfigAcquiredListener {
        public void onConfigAcquired(Map<Serializable, Serializable> map);

        public void onFailure(Throwable e);
    }

    public void getOnlineConfig() {
        if (TextUtils.isEmpty(mapUrl)) throw new NullPointerException("Please set mapUrl.");
        if (l == null) throw new NullPointerException("Please set onConfigAcquiredListener.");
        subscription = Observable.create(new Observable.OnSubscribe<Map<Serializable, Serializable>>() {
            @Override
            public void call(Subscriber<? super Map<Serializable, Serializable>> subscriber) {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(mapUrl).build();
                Response response;
                InputStream is = null;
                try {
                    response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        is = response.body().byteStream();
                        final Map<Serializable, Serializable> map = XdUpdateUtils.toMap(is);
                        if (XdConstants.isDebugMode()) System.out.println(map);
                        subscriber.onNext(map);
                    } else {
                        subscriber.onError(new IOException(response.code() + " : " + response.body().string()));
                    }
                } catch (Throwable e) {
                    subscriber.onError(e);
                } finally {
                    XdUpdateUtils.closeQuietly(is);
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Map<Serializable, Serializable>>() {

                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        if (XdConstants.isDebugMode()) e.printStackTrace();
                        l.onFailure(e);
                    }

                    @Override
                    public void onNext(Map<Serializable, Serializable> map) {
                        l.onConfigAcquired(map);
                    }
                });
    }

    public static class Builder {

        protected String mMapUrl = "";
        protected OnConfigAcquiredListener mListener = null;

        public Builder setMapUrl(String mapUrl) {
            mMapUrl = mapUrl;
            return this;
        }

        public Builder setOnConfigAcquiredListener(OnConfigAcquiredListener l) {
            mListener = l;
            return this;
        }

        public Builder setDebugMode(boolean debugMode) {
            XdConstants.setDebugMode(debugMode);
            return this;
        }

        public XdOnlineConfig build() {
            if (instance == null) instance = new XdOnlineConfig();
            instance.mapUrl = mMapUrl;
            instance.l = mListener;
            return instance;
        }
    }
}
