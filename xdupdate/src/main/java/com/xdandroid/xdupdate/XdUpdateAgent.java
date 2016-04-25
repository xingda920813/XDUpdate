package com.xdandroid.xdupdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

/**
 * Created by XingDa on 2016/04/24.
 */
public class XdUpdateAgent {

    private XdUpdateAgent() {
    }

    private boolean forceUpdate = false;
    private boolean allow4G;
    private String jsonUrl;
    private boolean enabled;
    private int iconResId;

    public void forceUpdate(final Activity activity) {
        forceUpdate = true;
        update(activity);
    }

    public void update(final Activity activity) {
        if (!forceUpdate) {
            if (!enabled) {
                return;
            }
            if (!XdUpdateUtils.isWifi(activity) && !allow4G) {
                return;
            }
        }
        if (TextUtils.isEmpty(jsonUrl)) {
            throw new IllegalArgumentException("Please set jsonUrl.");
        }
        if (iconResId == 0) {
            throw new IllegalArgumentException("Please set iconResId.");
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                URL url;
                HttpURLConnection connection = null;
                InputStream is = null;
                String s = null;
                try {
                    url = new URL(jsonUrl);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.connect();
                    is = connection.getInputStream();
                    s = XdUpdateUtils.toString(is);
                } catch (IOException e) {
                    return;
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException ignored) {
                        }
                    }
                    if (connection != null) {
                        connection.disconnect();
                    }
                }
                final XdUpdateBean xdUpdateBean = new XdUpdateBean();
                try {
                    JSONObject jsonObject = new JSONObject(s);
                    xdUpdateBean.setVersionCode(jsonObject.getInt("versionCode"));
                    xdUpdateBean.setSize(jsonObject.getInt("size"));
                    xdUpdateBean.setVersionName(jsonObject.getString("versionName"));
                    xdUpdateBean.setUrl(jsonObject.getString("url"));
                    xdUpdateBean.setNote(jsonObject.getString("note"));
                    xdUpdateBean.setMd5(jsonObject.getString("md5"));
                } catch (JSONException e) {
                    return;
                }
                int currentCode = XdUpdateUtils.getVersionCode(activity.getApplicationContext());
                String currentName = XdUpdateUtils.getVersionName(activity.getApplicationContext());
                if (currentName != null) {
                    final int versionCode = xdUpdateBean.getVersionCode();
                    final String versionName = xdUpdateBean.getVersionName();
                    if (currentCode < versionCode || currentName.compareToIgnoreCase(versionName) < 0) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                final SharedPreferences sp = activity.getSharedPreferences("update",Context.MODE_MULTI_PROCESS);
                                long lastIgnoredDayBegin = sp.getLong("time", 0);
                                int lastIgnoredCode = sp.getInt("versionCode",0);
                                String lastIgnoredName = sp.getString("versionName","");
                                long todayBegin = XdUpdateUtils.dayBegin(new Date()).getTime();
                                if (!forceUpdate && todayBegin == lastIgnoredDayBegin && versionCode == lastIgnoredCode && versionName.equals(lastIgnoredName)) {
                                    return;
                                }
                                forceUpdate = false;
                                final File file = new File(activity.getExternalCacheDir(),"download.apk");
                                boolean fileExists = false;
                                if (file.exists()) {
                                    if (XdUpdateUtils.getMd5ByFile(file).equalsIgnoreCase(xdUpdateBean.getMd5())) {
                                        fileExists = true;
                                    } else {
                                        file.delete();
                                    }
                                }
                                AlertDialog.Builder builder = new AlertDialog.Builder(activity).setCancelable(false)
                                        .setTitle(versionName + "版本更新")
                                        .setMessage(xdUpdateBean.getNote())
                                        .setNegativeButton("稍后再说", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                sp.edit().putLong("time", XdUpdateUtils.dayBegin(new Date()).getTime()).putInt("versionCode", versionCode).putString("versionName", versionName).commit();
                                            }
                                        });
                                if (fileExists) {
                                    builder.setPositiveButton("立即安装(已下载)", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Uri uri = Uri.fromFile(file);
                                            Intent intent = new Intent(Intent.ACTION_VIEW);
                                            intent.setDataAndType(uri, "application/vnd.android.package-archive");
                                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                            activity.startActivity(intent);
                                        }
                                    });
                                } else {
                                    builder.setPositiveButton("立即下载(" + XdUpdateUtils.formatToMegaBytes(xdUpdateBean.getSize()) + "M)", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            Intent intent = new Intent(activity,XdUpdateService.class);
                                            intent.putExtra("xdUpdateBean", xdUpdateBean);
                                            intent.putExtra("appIcon",iconResId);
                                            activity.startService(intent);
                                        }
                                    });
                                }
                                builder.show();
                            }
                        });
                    }
                }
            }
        }).start();
    }

    public static class Builder {

        private String mJsonUrl = "";
        private boolean mAllow4G = false;
        private boolean mEnabled = true;
        private int mIconResId = 0;

        public Builder setJsonUrl(String jsonUrl) {
            mJsonUrl = jsonUrl;
            return this;
        }

        public Builder setAllow4G(boolean allow4G) {
            mAllow4G = allow4G;
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            mEnabled = enabled;
            return this;
        }

        public Builder setIconResId(int iconResId) {
            mIconResId = iconResId;
            return this;
        }

        public XdUpdateAgent build() {
            XdUpdateAgent agent = new XdUpdateAgent();
            agent.jsonUrl = mJsonUrl;
            agent.allow4G = mAllow4G;
            agent.enabled = mEnabled;
            agent.iconResId = mIconResId;
            return agent;
        }
    }
}
