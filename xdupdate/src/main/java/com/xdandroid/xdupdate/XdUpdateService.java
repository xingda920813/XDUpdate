package com.xdandroid.xdupdate;

import android.annotation.*;
import android.app.Notification;
import android.app.*;
import android.content.*;
import android.net.*;
import android.os.*;

import java.io.*;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.functions.*;
import io.reactivex.schedulers.*;
import okhttp3.*;

public class XdUpdateService extends Service {

    protected Notification.Builder mBuilder;
    protected NotificationManager mNotificationManager;
    protected volatile int mFileLength;
    protected volatile int mLength;
    protected DeleteReceiver mDeleteReceiver;
    protected File mFile;
    protected volatile boolean mInterrupted;
    protected Disposable mDisposable;

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
                                .setContentText(XdUpdateUtils.formatToMegaBytes(mLength) + "M/" + XdUpdateUtils.formatToMegaBytes(mFileLength) + "M")
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
                .setContentText("");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mBuilder.setShowWhen(true);
            mBuilder.setVibrate(new long[0]);
            mBuilder.setPriority(Notification.PRIORITY_HIGH);
        }
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mDisposable = Flowable.create(new FlowableOnSubscribe<Response>() {
            @Override
            public void subscribe(FlowableEmitter<Response> e) throws Exception {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder().url(xdUpdateBean.url).build();
                Response response;
                try {
                    response = client.newCall(request).execute();
                    if (response.isSuccessful()) {
                        e.onNext(response);
                    } else {
                        e.onError(new IOException(response.code() + ": " + response.body().string()));
                    }
                } catch (Throwable t) {
                    e.onError(t);
                }
            }
        }, BackpressureStrategy.BUFFER)
                              .subscribeOn(Schedulers.io())
                              .subscribe(new Consumer<Response>() {
            @Override
            public void accept(Response response) throws Exception {
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
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                throwable.printStackTrace();
            }
        });
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mDeleteReceiver != null) getApplicationContext().unregisterReceiver(mDeleteReceiver);
        if (mDisposable != null) mDisposable.dispose();
    }
}
