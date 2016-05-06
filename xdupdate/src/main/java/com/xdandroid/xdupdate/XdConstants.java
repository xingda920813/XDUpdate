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

    static boolean isDebugMode() {
        return debugMode;
    }

    static void setDebugMode(boolean debugMode) {
        XdConstants.debugMode = debugMode;
    }

    static String getDownloadText() {
        return downloadText;
    }

    static void setDownloadText(String downloadText) {
        XdConstants.downloadText = downloadText;
    }

    static String getInstallText() {
        return installText;
    }

    static void setInstallText(String installText) {
        XdConstants.installText = installText;
    }

    static String getLaterText() {
        return laterText;
    }

    static void setLaterText(String laterText) {
        XdConstants.laterText = laterText;
    }

    static String getHintText() {
        return hintText;
    }

    static void setHintText(String hintText) {
        XdConstants.hintText = hintText;
    }

    static String getDownloadingText() {
        return downloadingText;
    }

    static void setDownloadingText(String downloadingText) {
        XdConstants.downloadingText = downloadingText;
    }
}
