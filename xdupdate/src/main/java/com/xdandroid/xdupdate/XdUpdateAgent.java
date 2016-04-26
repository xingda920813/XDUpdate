package com.xdandroid.xdupdate;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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

    public interface OnUpdateListener {
        public void onUpdate(boolean needUpdate, XdUpdateBean updateBean);
    }

    private boolean forceUpdate = false;
    private boolean allow4G;
    private String jsonUrl;
    private boolean enabled;
    private int iconResId;
    private boolean showNotification;
    private OnUpdateListener l;

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
                final int currentCode = XdUpdateUtils.getVersionCode(activity.getApplicationContext());
                final String currentName = XdUpdateUtils.getVersionName(activity.getApplicationContext());
                if (currentName != null) {
                    final int versionCode = xdUpdateBean.getVersionCode();
                    final String versionName = xdUpdateBean.getVersionName();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            if (currentCode < versionCode || currentName.compareToIgnoreCase(versionName) < 0) {
                                if (l != null) {
                                    l.onUpdate(true,xdUpdateBean);
                                }
                                final SharedPreferences sp = activity.getSharedPreferences("update",Context.MODE_MULTI_PROCESS);
                                long lastIgnoredDayBegin = sp.getLong("time", 0);
                                int lastIgnoredCode = sp.getInt("versionCode",0);
                                String lastIgnoredName = sp.getString("versionName","");
                                long todayBegin = XdUpdateUtils.dayBegin(new Date()).getTime();
                                if (!forceUpdate && todayBegin == lastIgnoredDayBegin && versionCode == lastIgnoredCode && versionName.equals(lastIgnoredName)) {
                                    return;
                                }
                                final File file = new File(activity.getExternalCacheDir(),"download.apk");
                                boolean fileExists = false;
                                if (file.exists()) {
                                    if (XdUpdateUtils.getMd5ByFile(file).equalsIgnoreCase(xdUpdateBean.getMd5())) {
                                        fileExists = true;
                                    } else {
                                        file.delete();
                                    }
                                }
                                if (showNotification && !forceUpdate) {
                                    showNotification(sp, file, fileExists, activity, versionName, xdUpdateBean, versionCode);
                                } else {
                                    showAlertDialog(sp, file, fileExists, activity, versionName, xdUpdateBean, versionCode);
                                }
                                forceUpdate = false;
                            } else {
                                if (l != null) {
                                    l.onUpdate(false,xdUpdateBean);
                                }
                            }
                        }
                    });
                }
            }
        }).start();
    }

    private void showNotification(final SharedPreferences sp, final File file, final boolean fileExists, final Activity activity, final String versionName, final XdUpdateBean xdUpdateBean, final int versionCode) {
        activity.getApplicationContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                showAlertDialog(sp,file, fileExists,activity,versionName,xdUpdateBean,versionCode);
            }
        },new IntentFilter("com.xdandroid.xdupdate.UpdateDialog"));
        activity.getApplicationContext().registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                sp.edit().putLong("time", XdUpdateUtils.dayBegin(new Date()).getTime()).putInt("versionCode", versionCode).putString("versionName", versionName).commit();
            }
        },new IntentFilter("com.xdandroid.xdupdate.IgnoreUpdate"));
        Notification.Builder builder = new Notification.Builder(activity)
                .setAutoCancel(true)
                .setTicker(XdUpdateUtils.getApplicationName(activity.getApplicationContext()) + versionName + "版本更新")
                .setSmallIcon(iconResId)
                .setContentTitle(XdUpdateUtils.getApplicationName(activity.getApplicationContext()) + versionName + "版本更新")
                .setContentText(xdUpdateBean.getNote())
                .setContentIntent(PendingIntent.getBroadcast(activity.getApplicationContext(), 1, new Intent("com.xdandroid.xdupdate.UpdateDialog"), PendingIntent.FLAG_CANCEL_CURRENT))
                .setDeleteIntent(PendingIntent.getBroadcast(activity.getApplicationContext(), 2, new Intent("com.xdandroid.xdupdate.IgnoreUpdate"), PendingIntent.FLAG_CANCEL_CURRENT));
        NotificationManager manager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1,builder.getNotification());
    }

    private void showAlertDialog(final SharedPreferences sp, final File file, boolean fileExists, final Activity activity, final String versionName, final XdUpdateBean xdUpdateBean, final int versionCode) {
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

    public static class Builder {

        private String mJsonUrl = "";
        private boolean mAllow4G = false;
        private boolean mEnabled = true;
        private int mIconResId = 0;
        private boolean mShowNotification = true;
        private OnUpdateListener mListener = null;

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

        public Builder setShowNotification(boolean showNotification) {
            mShowNotification = showNotification;
            return this;
        }

        public Builder setOnUpdateListener(OnUpdateListener l) {
            mListener = l;
            return this;
        }

        public XdUpdateAgent build() {
            XdUpdateAgent updateAgent = new XdUpdateAgent();
            updateAgent.jsonUrl = mJsonUrl;
            updateAgent.allow4G = mAllow4G;
            updateAgent.enabled = mEnabled;
            updateAgent.iconResId = mIconResId;
            updateAgent.showNotification = mShowNotification;
            updateAgent.l = mListener;
            return updateAgent;
        }
    }
}
