package com.xdandroid.xdupdate;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import java.io.InputStream;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;

/**
 * Created by XingDa on 2016/4/25.
 */
public class XdOnlineConfig {

    private String mapUrl;
    private boolean enabled;
    private OnConfigAcquiredListener l;

    public interface OnConfigAcquiredListener {
        public void onConfigAcquired(Map<Serializable, Serializable> map);
        public void onFailure(Exception e);
    }

    public void getOnlineConfig() {
        if (!enabled) {
            return;
        }
        if (TextUtils.isEmpty(mapUrl)) {
            throw new NullPointerException("Please set mapUrl.");
        }
        if (l == null) {
            throw new NullPointerException("Please set onConfigAcquiredListener.");
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                URL url;
                HttpURLConnection connection = null;
                InputStream is = null;
                try {
                    url = new URL(mapUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();
                    is = connection.getInputStream();
                    final Map<Serializable, Serializable> map = XdUpdateUtils.toMap(is);
                    if (XdConstants.isDebugMode()) System.out.println(map);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            l.onConfigAcquired(map);
                        }
                    });
                } catch (final Exception e) {
                    if (XdConstants.isDebugMode()) e.printStackTrace(System.err);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            l.onFailure(e);
                        }
                    });
                } finally {
                    XdUpdateUtils.closeQuietly(is);
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
            }
        }).start();
    }

    public static class Builder {

        private String mMapUrl = "";
        private boolean mEnabled = true;
        private OnConfigAcquiredListener mListener = null;

        public Builder setMapUrl(String mapUrl) {
            mMapUrl = mapUrl;
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            mEnabled = enabled;
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
            XdOnlineConfig onlineConfig = new XdOnlineConfig();
            onlineConfig.mapUrl = mMapUrl;
            onlineConfig.enabled = mEnabled;
            onlineConfig.l = mListener;
            return onlineConfig;
        }
    }
}
