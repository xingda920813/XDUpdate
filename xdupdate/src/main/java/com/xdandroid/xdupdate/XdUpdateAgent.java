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
import android.os.Build;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by XingDa on 2016/04/24.
 */
public class XdUpdateAgent {

    protected XdUpdateAgent() {
    }
    protected static XdUpdateAgent instance;

    protected boolean forceUpdate = false;
    protected boolean uncancelable = false;
    protected boolean allow4G;
    protected String jsonUrl;
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
        if (TextUtils.isEmpty(jsonUrl)) {
            System.err.println("Please set jsonUrl.");
            return;
        }
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
                        subscriber.onError(new IOException(response.code() + " : " + response.body().string()));
                    }
                } catch (Throwable e) {
                    subscriber.onError(e);
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Subscriber<Response>() {

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
                        final int currentCode = XdUpdateUtils.getVersionCode(activity.getApplicationContext());
                        final String currentName = XdUpdateUtils.getVersionName(activity.getApplicationContext());
                        final int versionCode = xdUpdateBean.versionCode;
                        final String versionName = xdUpdateBean.versionName;
                        if (currentCode < versionCode || currentName.compareToIgnoreCase(versionName) < 0) {
                            if (l != null) l.onUpdate(true, xdUpdateBean);
                            final SharedPreferences sp = activity.getSharedPreferences("update", Context.MODE_PRIVATE);
                            long lastIgnoredDayBegin = sp.getLong("time", 0);
                            int lastIgnoredCode = sp.getInt("versionCode", 0);
                            String lastIgnoredName = sp.getString("versionName", "");
                            long todayBegin = XdUpdateUtils.dayBegin(new Date()).getTime();
                            if (!forceUpdate && todayBegin == lastIgnoredDayBegin && versionCode == lastIgnoredCode && versionName.equals(lastIgnoredName))
                                return;
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
                                        proceedToUI(sp, file, fileExists, activity, versionName, xdUpdateBean, versionCode);
                                    }

                                    @Override
                                    public void onNext(String Md5JustDownloaded) {
                                        String Md5InUpdateBean = xdUpdateBean.md5;
                                        if (Md5JustDownloaded.equalsIgnoreCase(Md5InUpdateBean)) {
                                            fileExists = true;
                                        } else {
                                            file.delete();
                                            if (XdConstants.debugMode) System.err.println("Md5 dismatch. Md5JustDownloaded : " + Md5JustDownloaded + ". Md5InUpdateBean : " + Md5InUpdateBean + ".");
                                        }
                                        proceedToUI(sp, file, fileExists, activity, versionName, xdUpdateBean, versionCode);
                                    }
                                });
                            } else {
                                proceedToUI(sp, file, false, activity, versionName, xdUpdateBean, versionCode);
                            }
                        } else {
                            if (l != null) l.onUpdate(false, xdUpdateBean);
                        }
                        forceUpdate = false;
                        uncancelable = false;
                    }
                });
    }

    private void proceedToUI(SharedPreferences sp, File file, boolean fileExists, Activity activity, String versionName, XdUpdateBean xdUpdateBean, int versionCode) {
        if (showNotification && !forceUpdate) {
            showNotification(sp, file, fileExists, activity, versionName, xdUpdateBean, versionCode);
        } else {
            showAlertDialog(sp, file, fileExists, activity, versionName, xdUpdateBean, versionCode);
        }
    }

    protected void showNotification(final SharedPreferences sp, final File file, final boolean fileExists, final Activity activity, final String versionName, final XdUpdateBean xdUpdateBean, final int versionCode) {
        activity.getApplicationContext().registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                showAlertDialog(sp, file, fileExists, activity, versionName, xdUpdateBean, versionCode);
            }
        }, new IntentFilter("com.xdandroid.xdupdate.UpdateDialog"));
        activity.getApplicationContext().registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                sp.edit().putLong("time", XdUpdateUtils.dayBegin(new Date()).getTime()).putInt("versionCode", versionCode).putString("versionName", versionName).apply();
            }
        }, new IntentFilter("com.xdandroid.xdupdate.IgnoreUpdate"));
        Notification.Builder builder = new Notification.Builder(activity)
                .setAutoCancel(true)
                .setTicker(XdUpdateUtils.getApplicationName(activity.getApplicationContext()) + versionName + XdConstants.hintText)
                .setSmallIcon(XdUpdateUtils.getAppIconResId(activity.getApplicationContext()))
                .setContentTitle(XdUpdateUtils.getApplicationName(activity.getApplicationContext()) + versionName + XdConstants.hintText)
                .setContentText(xdUpdateBean.note)
                .setContentIntent(PendingIntent.getBroadcast(activity.getApplicationContext(), 1, new Intent("com.xdandroid.xdupdate.UpdateDialog"), PendingIntent.FLAG_CANCEL_CURRENT))
                .setDeleteIntent(PendingIntent.getBroadcast(activity.getApplicationContext(), 2, new Intent("com.xdandroid.xdupdate.IgnoreUpdate"), PendingIntent.FLAG_CANCEL_CURRENT));
        NotificationManager manager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) return;
        manager.notify(1, builder.build());
    }

    protected void showAlertDialog(final SharedPreferences sp, final File file, boolean fileExists, final Activity activity, final String versionName, final XdUpdateBean xdUpdateBean, final int versionCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity).setCancelable(false)
                .setTitle(versionName + XdConstants.hintText)
                .setMessage(xdUpdateBean.note);
        if (!uncancelable) {
            builder.setNegativeButton(XdConstants.laterText, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    sp.edit().putLong("time", XdUpdateUtils.dayBegin(new Date()).getTime()).putInt("versionCode", versionCode).putString("versionName", versionName).apply();
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
                    activity.startService(intent);
                }
            });
        }
        dialog = builder.create();
        dialog.show();
    }

    public static class Builder {

        protected String mJsonUrl = "";
        protected boolean mAllow4G = false;
        protected boolean mShowNotification = true;
        protected OnUpdateListener mListener = null;

        public Builder setJsonUrl(String jsonUrl) {
            mJsonUrl = jsonUrl;
            return this;
        }

        public Builder setAllow4G(boolean allow4G) {
            mAllow4G = allow4G;
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
            instance.jsonUrl = mJsonUrl;
            instance.allow4G = mAllow4G;
            instance.showNotification = mShowNotification;
            instance.l = mListener;
            return instance;
        }
    }

    public interface OnUpdateListener {
        public void onUpdate(boolean needUpdate, XdUpdateBean updateBean);
    }
}
