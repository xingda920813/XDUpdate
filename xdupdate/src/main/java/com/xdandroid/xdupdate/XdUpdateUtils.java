package com.xdandroid.xdupdate;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.math.BigInteger;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

/**
 * Created by XingDa on 2016/04/24.
 */
public class XdUpdateUtils {

    public static Date dayBegin(final Date date) {
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
        } else {
            return new DecimalFormat("#.0").format(megaBytes);
        }
    }

    public static String getMd5ByFile(File file) {
        String value = "";
        FileInputStream is = null;
        try {
            is = new FileInputStream(file);
            MappedByteBuffer byteBuffer = is.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteBuffer);
            BigInteger bi = new BigInteger(1, md5.digest());
            value = bi.toString(16);
        } catch (Exception ignored) {
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException ignored) {
                }
            }
        }
        return value;
    }

    public static String getApplicationName(Context app) {
        PackageManager packageManager;
        ApplicationInfo applicationInfo;
        try {
            packageManager = app.getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(app.getPackageName(), 0);
            return (String) packageManager.getApplicationLabel(applicationInfo);
        } catch (PackageManager.NameNotFoundException e) {
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
        } catch (PackageManager.NameNotFoundException ignored) {
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
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return versionName;
    }

    public static boolean isWifi(Context context) {
        NetworkInfo networkInfo = ((ConnectivityManager) (context.getSystemService(Context.CONNECTIVITY_SERVICE))).getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected() && (networkInfo.getType() == ConnectivityManager.TYPE_WIFI || networkInfo.getType() == ConnectivityManager.TYPE_ETHERNET || networkInfo.getType() == 17 || networkInfo.getType() == -1 || networkInfo.getType() == 13 || networkInfo.getType() == 16);
    }

    public static String toString(InputStream is) {
        if (is == null) {
            return "";
        }
        ByteArrayOutputStream baos = null;
        try {
            baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) > 0) {
              baos.write(buffer, 0, len);
            }
            return baos.toString();
        } catch (IOException ignored) {
            return "";
        } finally {
            if (baos != null) {
                try {
                    baos.close();
                } catch (IOException ignored) {
                }
            }
            try {
                is.close();
            } catch (IOException ignored) {
            }
        }
    }

    public static Map<Object,Object> toMap(InputStream is) throws IOException,ClassNotFoundException,NullPointerException {
        if (is == null) {
            throw new NullPointerException("inputStream == null");
        }
        ObjectInputStream ois = new ObjectInputStream(is);
        Map<Object,Object> map = (Map<Object, Object>) ois.readObject();
        ois.close();
        return map;
    }
}
