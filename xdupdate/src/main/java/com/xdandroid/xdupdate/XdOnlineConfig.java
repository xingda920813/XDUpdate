package com.xdandroid.xdupdate;

import android.text.*;

import java.io.*;
import java.util.*;

import okhttp3.*;
import rx.Observable;
import rx.*;
import rx.android.schedulers.*;
import rx.schedulers.*;

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
        if (TextUtils.isEmpty(mapUrl)) {
            System.err.println("Please set mapUrl.");
            return;
        }
        if (l == null) {
            System.err.println("Please set onConfigAcquiredListener.");
            return;
        }
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
                        if (XdConstants.debugMode) System.out.println(map);
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
                        if (XdConstants.debugMode) e.printStackTrace();
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
            XdConstants.debugMode = debugMode;
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
