package com.xdandroid.xdupdate;

import android.annotation.*;
import android.app.*;
import android.app.Notification;
import android.content.*;
import android.net.*;
import android.os.*;

import java.io.*;

import okhttp3.*;
import rx.*;
import rx.schedulers.*;

/**
 * Created by XingDa on 2016/04/24.
 */
public class XdUpdateService extends Service {

    protected Notification.Builder mBuilder;
    protected NotificationManager mNotificationManager;
    protected volatile int mFileLength;
    protected volatile int mLength;
    protected DeleteReceiver mDeleteReceiver;
    protected File mFile;
    protected volatile boolean mInterrupted;
    protected Subscription mSubscription;

    protected static final int TYPE_FINISHED = 0;
    protected static final int TYPE_DOWNLOADING = 1;

    protected class DeleteReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            mInterrupted = true;
            handler.sendEmptyMessage(TYPE_FINISHED);
            mNotificationManager.cancel(2);
            if (mFile != null && mFile.exists()) mFile.delete();
            stopSelf();
        }
    }

    @SuppressLint("HandlerLeak")
    protected Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            removeCallbacksAndMessages(null);
            switch (msg.what) {
                case TYPE_DOWNLOADING:
                    if (mInterrupted) {
                        mNotificationManager.cancel(2);
                    } else {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) return;
                        mNotificationManager.notify(2, mBuilder
                                .setContentText(XdUpdateUtils.formatToMegaBytes(mLength) + "M/" +
                                        XdUpdateUtils.formatToMegaBytes(mFileLength) + "M")
                                .setProgress(mFileLength, mLength, false)
                                .build());
                        sendEmptyMessageDelayed(TYPE_DOWNLOADING, 500);
                    }
                    break;
                case TYPE_FINISHED:
                    mNotificationManager.cancel(2);
                    break;
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressWarnings("ResourceType")
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final XdUpdateBean xdUpdateBean = (XdUpdateBean) intent.getSerializableExtra("xdUpdateBean");
        int iconResId = intent.getIntExtra("appIcon", 0);
        if (xdUpdateBean == null) {
            stopSelf();
            return START_NOT_STICKY;
        }
        mDeleteReceiver = new DeleteReceiver();
        getApplicationContext().registerReceiver(mDeleteReceiver, new IntentFilter("com.xdandroid.xdupdate.DeleteUpdate"));
        int smallIconResId = iconResId > 0 ? iconResId : XdUpdateUtils.getAppIconResId(getApplicationContext());
        String title = XdUpdateUtils.getApplicationName(getApplicationContext()) + " " + xdUpdateBean.versionName + " " + XdConstants.downloadingText + "...";
        mBuilder = new Notification.Builder(XdUpdateService.this)
                .setProgress(0, 0, false)
                .setAutoCancel(false)
                .setTicker(title)
                .setSmallIcon(smallIconResId)
                .setContentTitle(title)
                .setContentText("")
                .setDeleteIntent(PendingIntent.getBroadcast(getApplicationContext(), 3, new Intent("com.xdandroid.xdupdate.DeleteUpdate"), PendingIntent.FLAG_CANCEL_CURRENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBuilder.setShowWhen(true);
            mBuilder.setVibrate(new long[0]);
            mBuilder.setPriority(Notification.PRIORITY_HIGH);
        }
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mSubscription = Observable.create(new Observable.OnSubscribe<Response>() {
            @Override
            public void call(Subscriber<? super Response> subscriber) {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(xdUpdateBean.url).build();
                Response response;
                try {
                    response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        subscriber.onNext(response);
                    } else {
                        subscriber.onError(new IOException(response.code() + ": " + response.body().string()));
                    }
                } catch (Throwable t) {
                    subscriber.onError(t);
                }
            }
        }).subscribeOn(Schedulers.io()).subscribe(
                new Subscriber<Response>() {

                    public void onCompleted() {}

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Response response) {
                        InputStream is = null;
                        FileOutputStream fos = null;
                        try {
                            is = response.body().byteStream();
                            mFileLength = (int) response.body().contentLength();
                            mFile = new File(getExternalCacheDir(), "update.apk");
                            if (mFile.exists()) mFile.delete();
                            fos = new FileOutputStream(mFile);
                            byte[] buffer = new byte[8192];
                            int hasRead;
                            handler.sendEmptyMessage(TYPE_DOWNLOADING);
                            mInterrupted = false;
                            while ((hasRead = is.read(buffer)) >= 0) {
                                if (mInterrupted) return;
                                fos.write(buffer, 0, hasRead);
                                mLength = mLength + hasRead;
                            }
                            handler.sendEmptyMessage(TYPE_FINISHED);
                            mLength = 0;
                            if (mFile.exists()) {
                                String md5JustDownloaded = XdUpdateUtils.getMd5ByFile(mFile);
                                String md5InUpdateBean = xdUpdateBean.md5;
                                if (md5JustDownloaded.equalsIgnoreCase(md5InUpdateBean)) {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                                        StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                                                .detectFileUriExposure()
                                                .penaltyLog()
                                                .build());
                                    Uri uri = Uri.fromFile(mFile);
                                    Intent intent = new Intent(Intent.ACTION_VIEW);
                                    intent.setDataAndType(uri, "application/vnd.android.package-archive");
                                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(intent);
                                } else {
                                    mFile.delete();
                                    throw new Exception("MD5 mismatch. md5JustDownloaded: " + md5JustDownloaded + ". md5InUpdateBean: " + md5InUpdateBean + ".");
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            sendBroadcast(new Intent("com.xdandroid.xdupdate.DeleteUpdate"));
                        } finally {
                            XdUpdateUtils.closeQuietly(fos);
                            XdUpdateUtils.closeQuietly(is);
                            stopSelf();
                        }
                    }
                });
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDeleteReceiver != null) getApplicationContext().unregisterReceiver(mDeleteReceiver);
        if (mSubscription != null) mSubscription.unsubscribe();
    }
}
