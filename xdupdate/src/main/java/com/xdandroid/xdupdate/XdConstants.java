package com.xdandroid.xdupdate;

/**
 * Created by XingDa on 2016/5/3.
 */
public class XdConstants {

    private static boolean debugMode = false;
    private static String downloadText = "立即下载";
    private static String installText = "立即安装(已下载)";
    private static String laterText = "稍后再说";
    private static String hintText = "版本更新";
    private static String downloadingText = "正在下载";

    public static boolean isDebugMode() {
        return debugMode;
    }

    public static void setDebugMode(boolean debugMode) {
        XdConstants.debugMode = debugMode;
    }

    public static String getDownloadText() {
        return downloadText;
    }

    public static void setDownloadText(String downloadText) {
        XdConstants.downloadText = downloadText;
    }

    public static String getInstallText() {
        return installText;
    }

    public static void setInstallText(String installText) {
        XdConstants.installText = installText;
    }

    public static String getLaterText() {
        return laterText;
    }

    public static void setLaterText(String laterText) {
        XdConstants.laterText = laterText;
    }

    public static String getHintText() {
        return hintText;
    }

    public static void setHintText(String hintText) {
        XdConstants.hintText = hintText;
    }

    public static String getDownloadingText() {
        return downloadingText;
    }

    public static void setDownloadingText(String downloadingText) {
        XdConstants.downloadingText = downloadingText;
    }
}
