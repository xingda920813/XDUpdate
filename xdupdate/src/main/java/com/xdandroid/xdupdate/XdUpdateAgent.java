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

    protected XdUpdateAgent() {}

    protected static XdUpdateAgent sInstance;

    protected boolean mForceUpdate;
    protected XdUpdateBean mUpdateBeanProvided;
    protected String mJsonUrl;
    protected int mIconResId;
    protected boolean mShowDialogIfWifi;
    protected OnUpdateListener mListener;

    public void forceUpdate(Activity activity) {
        mForceUpdate = true;
        update(activity);
    }

    public void update(final Activity activity) {
        if (mUpdateBeanProvided == null && TextUtils.isEmpty(mJsonUrl)) {
            System.err.println("Please set updateBean or mJsonUrl.");
            mForceUpdate = false;
            return;
        }
        if (mUpdateBeanProvided != null) {
            updateMatters(mUpdateBeanProvided, activity);
        } else {
            Observable.create(new Observable.OnSubscribe<String>() {
                @Override
                public void call(Subscriber<? super String> subscriber) {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder().url(mJsonUrl).build();
                    Response response;
                    try {
                        response = client.newCall(request).execute();
                        if (response.isSuccessful()) {
                            subscriber.onNext(response.body().string());
                        } else {
                            subscriber.onError(new IOException(response.code() + ": " + response.body().string()));
                        }
                    } catch (Throwable t) {
                        subscriber.onError(t);
                    }
                }
            }).subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread()).subscribe(
                    new Subscriber<String>() {

                        public void onCompleted() {}

                        @Override
                        public void onError(Throwable e) {
                            e.printStackTrace();
                            mForceUpdate = false;
                        }

                        @Override
                        public void onNext(String responseBody) {
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
                            } catch (Exception e) {
                                e.printStackTrace();
                                mForceUpdate = false;
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
            if (mListener != null) mListener.onUpdate(true, updateBean);
            final SharedPreferences sp = activity.getSharedPreferences("update", Context.MODE_PRIVATE);
            long lastIgnoredDayBegin = sp.getLong("time", 0);
            int lastIgnoredCode = sp.getInt("versionCode", 0);
            long todayBegin = XdUpdateUtils.dayBegin(new Date()).getTime();
            if (!mForceUpdate && todayBegin == lastIgnoredDayBegin && versionCode == lastIgnoredCode) {
                mForceUpdate = false;
                return;
            }
            final File file = new File(activity.getExternalCacheDir(), "update.apk");
            if (file.exists()) {
                XdUpdateUtils.getMd5ByFile(file, new Subscriber<String>() {

                    boolean fileExists = false;

                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {
                        file.delete();
                        e.printStackTrace();
                        proceedToUI(sp, file, fileExists, activity, versionName, updateBean, versionCode);
                    }

                    @Override
                    public void onNext(String md5JustDownloaded) {
                        String md5InUpdateBean = updateBean.md5;
                        if (md5JustDownloaded.equalsIgnoreCase(md5InUpdateBean)) {
                            fileExists = true;
                        } else {
                            file.delete();
                            System.err.println("MD5 mismatch. md5JustDownloaded: " + md5JustDownloaded + ". md5InUpdateBean: " + md5InUpdateBean + ".");
                        }
                        proceedToUI(sp, file, fileExists, activity, versionName, updateBean, versionCode);
                    }
                });
            } else {
                proceedToUI(sp, file, false, activity, versionName, updateBean, versionCode);
            }
        } else {
            if (mListener != null) mListener.onUpdate(false, updateBean);
        }
        mForceUpdate = false;
    }

    protected void proceedToUI(SharedPreferences sp, File file, boolean fileExists, Activity activity, String versionName, XdUpdateBean xdUpdateBean, int versionCode) {
        if (mForceUpdate || (mShowDialogIfWifi && XdUpdateUtils.isWifi(activity.getApplicationContext()))) {
            showAlertDialog(sp, file, fileExists, activity, versionName, xdUpdateBean, versionCode);
        } else {
            showNotification(sp, file, fileExists, activity, versionName, xdUpdateBean, versionCode);
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
        int smallIconResId = mIconResId > 0 ? mIconResId : XdUpdateUtils.getAppIconResId(activity.getApplicationContext());
        String title = XdUpdateUtils.getApplicationName(activity.getApplicationContext()) + " " + versionName + " " + XdConstants.hintText;
        Notification.Builder builder = new Notification.Builder(activity)
                .setAutoCancel(true)
                .setTicker(title)
                .setSmallIcon(smallIconResId)
                .setContentTitle(title)
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
                .setMessage(xdUpdateBean.note)
                .setNegativeButton(XdConstants.laterText, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    sp.edit()
                      .putLong("time", XdUpdateUtils.dayBegin(new Date()).getTime())
                      .putInt("versionCode", versionCode)
                      .putString("versionName", versionName)
                      .apply();
                }
            });
        if (fileExists) {
            builder.setPositiveButton(XdConstants.installText, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                                .detectFileUriExposure()
                                .penaltyLog()
                                .build());
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
                    intent.putExtra("appIcon", mIconResId);
                    activity.startService(intent);
                }
            });
        }
        try {builder.show();} catch (Exception ignored) {}
    }

    public static class Builder {

        protected XdUpdateBean updateBeanProvided;
        protected String jsonUrl;
        protected int iconResId;
        protected boolean showDialogIfWifi;
        protected OnUpdateListener l;

        public Builder setUpdateBean(XdUpdateBean updateBeanProvided) {
            this.updateBeanProvided = updateBeanProvided;
            return this;
        }

        public Builder setJsonUrl(String jsonUrl) {
            this.jsonUrl = jsonUrl;
            return this;
        }

        public Builder setIconResId(int iconResId) {
            this.iconResId = iconResId;
            return this;
        }

        public Builder setShowDialogIfWifi(boolean showDialogIfWifi) {
            this.showDialogIfWifi = showDialogIfWifi;
            return this;
        }

        public Builder setOnUpdateListener(OnUpdateListener l) {
            this.l = l;
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
            if (sInstance == null) sInstance = new XdUpdateAgent();
            if (updateBeanProvided != null) {
                sInstance.mUpdateBeanProvided = updateBeanProvided;
            } else {
                sInstance.mJsonUrl = jsonUrl;
            }
            sInstance.mIconResId = iconResId;
            sInstance.mShowDialogIfWifi = showDialogIfWifi;
            sInstance.mListener = l;
            return sInstance;
        }
    }

    public interface OnUpdateListener {
        void onUpdate(boolean needUpdate, XdUpdateBean updateBean);
    }
}
