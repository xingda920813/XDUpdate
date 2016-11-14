package com.xdandroid.xdupdate;

import android.app.*;
import android.app.Notification;
import android.content.*;
import android.net.*;
import android.os.*;
import android.support.v7.app.AlertDialog;
import android.text.*;

import org.json.*;

import java.io.*;
import java.util.*;

import okhttp3.*;
import rx.Observable;
import rx.*;
import rx.android.schedulers.*;
import rx.schedulers.*;

/**
 * Created by XingDa on 2016/04/24.
 */
public class XdUpdateAgent {

    protected XdUpdateAgent() {
    }

    protected static XdUpdateAgent instance;

    protected boolean forceUpdate;
    protected boolean uncancelable;
    protected boolean allow4G;
    protected XdUpdateBean updateBeanLocallyProvided;
    protected String jsonUrl;
    protected int iconResId;
    protected boolean showNotification;
    protected OnUpdateListener l;
    protected Subscription md5Subscription, subscription;
    protected AlertDialog dialog;

    public AlertDialog getDialog() {
        return dialog;
    }

    public XdUpdateAgent setJsonUrl(String jsonUrl) {
        instance.jsonUrl = jsonUrl;
        return instance;
    }

    public void onDestroy() {
        if (dialog != null) {
            try {
                dialog.dismiss();
            } catch (Throwable ignored) {
            }
        }
        if (md5Subscription != null) md5Subscription.unsubscribe();
        if (subscription != null) subscription.unsubscribe();
    }

    public void forceUpdate(Activity activity) {
        forceUpdate = true;
        update(activity);
    }

    public void forceUpdateUncancelable(Activity activity) {
        uncancelable = true;
        forceUpdate(activity);
    }

    public void update(final Activity activity) {
        if (!forceUpdate && !allow4G && !XdUpdateUtils.isWifi(activity)) return;
        if (updateBeanLocallyProvided == null && TextUtils.isEmpty(jsonUrl)) {
            System.err.println("Please set updateBean or jsonUrl.");
            return;
        }
        if (updateBeanLocallyProvided != null) {
            updateMatters(updateBeanLocallyProvided, activity);
        } else {
            subscription = Observable.create(new Observable.OnSubscribe<Response>() {
                @Override
                public void call(Subscriber<? super Response> subscriber) {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(jsonUrl).build();
                    Response response;
                    try {
                        response = client.newCall(request).execute();
                        if (response.isSuccessful()) {
                            subscriber.onNext(response);
                        } else {
                            subscriber.onError(new IOException(response.code() + ": " + response.body().string()));
                        }
                    } catch (Throwable e) {
                        subscriber.onError(e);
                    }
                }
            }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                    new Subscriber<Response>() {

                        public void onCompleted() {
                        }

                        @Override
                        public void onError(Throwable e) {
                            if (XdConstants.debugMode) e.printStackTrace();
                        }

                        @Override
                        public void onNext(Response response) {
                            String responseBody;
                            try {
                                responseBody = response.body().string();
                            } catch (Throwable e) {
                                if (XdConstants.debugMode) e.printStackTrace();
                                return;
                            }
                            if (XdConstants.debugMode) System.out.println(responseBody);
                            final XdUpdateBean xdUpdateBean = new XdUpdateBean();
                            try {
                                JSONObject jsonObject = new JSONObject(responseBody);
                                xdUpdateBean.versionCode = jsonObject.getInt("versionCode");
                                xdUpdateBean.size = jsonObject.getInt("size");
                                xdUpdateBean.versionName = jsonObject.getString("versionName");
                                xdUpdateBean.url = jsonObject.getString("url");
                                xdUpdateBean.note = jsonObject.getString("note");
                                xdUpdateBean.md5 = jsonObject.getString("md5");
                            } catch (JSONException e) {
                                if (XdConstants.debugMode) e.printStackTrace();
                                return;
                            }
                            updateMatters(xdUpdateBean, activity);
                        }
                    });
        }
    }

    protected void updateMatters(final XdUpdateBean updateBean, final Activity activity) {
        final int currentCode = XdUpdateUtils.getVersionCode(activity.getApplicationContext());
        final int versionCode = updateBean.versionCode;
        final String versionName = updateBean.versionName;
        if (currentCode < versionCode) {
            if (l != null) l.onUpdate(true, updateBean);
            final SharedPreferences sp = activity.getSharedPreferences("update", Context.MODE_PRIVATE);
            long lastIgnoredDayBegin = sp.getLong("time", 0);
            int lastIgnoredCode = sp.getInt("versionCode", 0);
            long todayBegin = XdUpdateUtils.dayBegin(new Date()).getTime();
            if (!forceUpdate && todayBegin == lastIgnoredDayBegin && versionCode == lastIgnoredCode) return;
            final File file = new File(activity.getExternalCacheDir(), "update.apk");
            if (file.exists()) {
                md5Subscription = XdUpdateUtils.getMd5ByFile(file, new Subscriber<String>() {

                    boolean fileExists = false;

                    public void onCompleted() {
                    }

                    @Override
                    public void onError(Throwable e) {
                        file.delete();
                        if (XdConstants.debugMode) e.printStackTrace();
                        proceedToUI(sp, file, fileExists, activity, versionName, updateBean, versionCode);
                    }

                    @Override
                    public void onNext(String Md5JustDownloaded) {
                        String Md5InUpdateBean = updateBean.md5;
                        if (Md5JustDownloaded.equalsIgnoreCase(Md5InUpdateBean)) {
                            fileExists = true;
                        } else {
                            file.delete();
                            if (XdConstants.debugMode) System.err.println("Md5 dismatch. Md5JustDownloaded: " + Md5JustDownloaded + ". Md5InUpdateBean: " + Md5InUpdateBean + ".");
                        }
                        proceedToUI(sp, file, fileExists, activity, versionName, updateBean, versionCode);
                    }
                });
            } else {
                proceedToUI(sp, file, false, activity, versionName, updateBean, versionCode);
            }
        } else {
            if (l != null) l.onUpdate(false, updateBean);
        }
        forceUpdate = false;
        uncancelable = false;
    }

    protected void proceedToUI(SharedPreferences sp, File file, boolean fileExists, Activity activity, String versionName, XdUpdateBean xdUpdateBean, int versionCode) {
        if (showNotification && !forceUpdate) {
            showNotification(sp, file, fileExists, activity, versionName, xdUpdateBean, versionCode);
        } else {
            showAlertDialog(sp, file, fileExists, activity, versionName, xdUpdateBean, versionCode);
        }
    }

    @SuppressWarnings("ResourceType")
    protected void showNotification(final SharedPreferences sp, final File file, final boolean fileExists, final Activity activity, final String versionName, final XdUpdateBean xdUpdateBean, final int versionCode) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) return;
        activity.getApplicationContext().registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                showAlertDialog(sp, file, fileExists, activity, versionName, xdUpdateBean, versionCode);
            }
        }, new IntentFilter("com.xdandroid.xdupdate.UpdateDialog"));
        activity.getApplicationContext().registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                sp.edit()
                  .putLong("time", XdUpdateUtils.dayBegin(new Date()).getTime())
                  .putInt("versionCode", versionCode)
                  .putString("versionName", versionName)
                  .apply();
            }
        }, new IntentFilter("com.xdandroid.xdupdate.IgnoreUpdate"));
        int smallIconResId = iconResId > 0 ? iconResId : XdUpdateUtils.getAppIconResId(activity.getApplicationContext());
        Notification.Builder builder = new Notification.Builder(activity)
                .setAutoCancel(true)
                .setTicker(XdUpdateUtils.getApplicationName(activity.getApplicationContext()) + " " +  versionName + " " + XdConstants.hintText)
                .setSmallIcon(smallIconResId)
                .setContentTitle(XdUpdateUtils.getApplicationName(activity.getApplicationContext()) + " " + versionName + " " + XdConstants.hintText)
                .setContentText(xdUpdateBean.note)
                .setContentIntent(PendingIntent.getBroadcast(activity.getApplicationContext(), 1, new Intent("com.xdandroid.xdupdate.UpdateDialog"), PendingIntent.FLAG_CANCEL_CURRENT))
                .setDeleteIntent(PendingIntent.getBroadcast(activity.getApplicationContext(), 2, new Intent("com.xdandroid.xdupdate.IgnoreUpdate"), PendingIntent.FLAG_CANCEL_CURRENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            builder.setShowWhen(true);
            builder.setVibrate(new long[0]);
        }
        builder.setPriority(Notification.PRIORITY_HIGH);
        NotificationManager manager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(1, builder.build());
    }

    protected void showAlertDialog(final SharedPreferences sp, final File file, boolean fileExists, final Activity activity, final String versionName, final XdUpdateBean xdUpdateBean, final int versionCode) {
        AlertDialog.Builder builder = new AlertDialog
                .Builder(activity)
                .setCancelable(false)
                .setTitle(versionName + " " + XdConstants.hintText)
                .setMessage(xdUpdateBean.note);
        if (!uncancelable) {
            builder.setNegativeButton(XdConstants.laterText, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    sp.edit()
                      .putLong("time", XdUpdateUtils.dayBegin(new Date()).getTime())
                      .putInt("versionCode", versionCode)
                      .putString("versionName", versionName)
                      .apply();
                }
            });
        }
        if (fileExists) {
            builder.setPositiveButton(XdConstants.installText, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    Uri uri = Uri.fromFile(file);
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "application/vnd.android.package-archive");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    activity.startActivity(intent);
                }
            });
        } else {
            builder.setPositiveButton(XdConstants.downloadText + "(" + XdUpdateUtils.formatToMegaBytes(xdUpdateBean.size) + "M)", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(activity, XdUpdateService.class);
                    intent.putExtra("xdUpdateBean", xdUpdateBean);
                    intent.putExtra("appIcon", iconResId);
                    activity.startService(intent);
                }
            });
        }
        dialog = builder.create();
        dialog.show();
    }

    public static class Builder {

        protected XdUpdateBean mUpdateBean;
        protected String mJsonUrl;
        protected boolean mAllow4G;
        protected int mIconResId;
        protected boolean mShowNotification = true;
        protected OnUpdateListener mListener;

        public Builder setUpdateBean(XdUpdateBean updateBean) {
            mUpdateBean = updateBean;
            return this;
        }

        public Builder setJsonUrl(String jsonUrl) {
            mJsonUrl = jsonUrl;
            return this;
        }

        public Builder setAllow4G(boolean allow4G) {
            mAllow4G = allow4G;
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

        public Builder setDebugMode(boolean debugMode) {
            XdConstants.debugMode = debugMode;
            return this;
        }

        public Builder setDownloadText(String downloadText) {
            if (!TextUtils.isEmpty(downloadText)) XdConstants.downloadText = downloadText;
            return this;
        }

        public Builder setInstallText(String installText) {
            if (!TextUtils.isEmpty(installText)) XdConstants.installText = installText;
            return this;
        }

        public Builder setLaterText(String laterText) {
            if (!TextUtils.isEmpty(laterText)) XdConstants.laterText = laterText;
            return this;
        }

        public Builder setHintText(String hintText) {
            if (!TextUtils.isEmpty(hintText)) XdConstants.hintText = hintText;
            return this;
        }

        public Builder setDownloadingText(String downloadingText) {
            if (!TextUtils.isEmpty(downloadingText)) XdConstants.downloadingText = downloadingText;
            return this;
        }

        public XdUpdateAgent build() {
            if (instance == null) instance = new XdUpdateAgent();
            if (mUpdateBean != null) {
                instance.updateBeanLocallyProvided = mUpdateBean;
            } else {
                instance.jsonUrl = mJsonUrl;
            }
            instance.allow4G = mAllow4G;
            instance.iconResId = mIconResId;
            instance.showNotification = mShowNotification;
            instance.l = mListener;
            return instance;
        }
    }

    public interface OnUpdateListener {
        public void onUpdate(boolean needUpdate, XdUpdateBean updateBean);
    }
}
