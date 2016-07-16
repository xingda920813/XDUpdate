package com.xdandroid.xdupdate;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by XingDa on 2016/04/24.
 */
public class XdUpdateUtils {

    protected XdUpdateUtils() {
    }

    public static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (Throwable ignored) {
            }
        }
    }

    public static Date dayBegin(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTime();
    }

    public static String formatToMegaBytes(long bytes) {
        double megaBytes = bytes / 1048576.0;
        if (megaBytes < 1) {
            return new DecimalFormat("0.0").format(megaBytes);
        }
        return new DecimalFormat("#.0").format(megaBytes);
    }

    public static Subscription getMd5ByFile(final File file, Subscriber<String> md5Subscriber) {
        return Observable.create(new Observable.OnSubscribe<String>() {
            @Override
            public void call(Subscriber<? super String> subscriber) {
                FileInputStream fis = null;
                try {
                    fis = new FileInputStream(file);
                    MappedByteBuffer byteBuffer = fis.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
                    MessageDigest md5 = MessageDigest.getInstance("MD5");
                    md5.update(byteBuffer);
                    BigInteger bi = new BigInteger(1, md5.digest());
                    subscriber.onNext(bi.toString(16));
                } catch (Throwable e) {
                    subscriber.onError(e);
                } finally {
                    closeQuietly(fis);
                }
            }
        }).subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(md5Subscriber);
    }

    public static String getMd5ByFile(File file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file);
            MappedByteBuffer byteBuffer = fis.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md5.digest());
            return bi.toString(16);
        } catch (Throwable e) {
            if (XdConstants.debugMode) e.printStackTrace();
            return "";
        } finally {
            closeQuietly(fis);
        }
    }

    public static String getApplicationName(Context app) {
        PackageManager packageManager;
        ApplicationInfo applicationInfo;
        try {
            packageManager = app.getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(app.getPackageName(), 0);
            return (String) packageManager.getApplicationLabel(applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
            if (XdConstants.debugMode) e.printStackTrace();
            return "";
        }
    }

    public static int getVersionCode(Context app) {
        int versionCode = 0;
        PackageManager manager = app.getPackageManager();
        PackageInfo info;
        try {
            info = manager.getPackageInfo(app.getPackageName(), 0);
            versionCode = info.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            if (XdConstants.debugMode) e.printStackTrace();
        }
        return versionCode;
    }

    public static String getVersionName(Context app) {
        String versionName = "";
        PackageManager manager = app.getPackageManager();
        PackageInfo info;
        try {
            info = manager.getPackageInfo(app.getPackageName(), 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            if (XdConstants.debugMode) e.printStackTrace();
        }
        return versionName;
    }

    public static int getAppIconResId(Context app) {
        int id = 0;
        PackageManager pm = app.getPackageManager();
        String pkg = app.getPackageName();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            id = ai.icon;
        } catch (PackageManager.NameNotFoundException e) {
            if (XdConstants.debugMode) e.printStackTrace();
        }
        return id;
    }

    public static boolean isWifi(Context context) {
        NetworkInfo networkInfo = ((ConnectivityManager) (context.getSystemService(Context.CONNECTIVITY_SERVICE))).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected() && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET || networkInfo.getType() == 17 || networkInfo.getType() == -1 || networkInfo.getType() == 13 || networkInfo.getType() == 16);
    }

    @SuppressWarnings("unchecked")
    public static Map<Serializable, Serializable> toMap(InputStream is) throws IOException, ClassNotFoundException {
        if (is == null) {
            System.err.println("inputStream == null");
            return null;
        }
        ObjectInputStream ois = new ObjectInputStream(is);
        Map<Serializable, Serializable> map = (Map<Serializable, Serializable>) ois.readObject();
        closeQuietly(ois);
        return map;
    }
}
