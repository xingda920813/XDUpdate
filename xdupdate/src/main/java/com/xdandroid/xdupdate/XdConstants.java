package com.xdandroid.xdupdate;

import java.io.Serializable;

/**
 * Created by XingDa on 2016/5/3.
 */
public class XdConstants implements Serializable {

    protected static boolean debugMode = false;
    protected static String downloadText = "立即下载";
    protected static String installText = "立即安装(已下载)";
    protected static String laterText = "稍后再说";
    protected static String hintText = "版本更新";
    protected static String downloadingText = "正在下载";

    protected static boolean isDebugMode() {
        return debugMode;
    }

    protected static void setDebugMode(boolean debugMode) {
        XdConstants.debugMode = debugMode;
    }

    protected static String getDownloadText() {
        return downloadText;
    }

    protected static void setDownloadText(String downloadText) {
        XdConstants.downloadText = downloadText;
    }

    protected static String getInstallText() {
        return installText;
    }

    protected static void setInstallText(String installText) {
        XdConstants.installText = installText;
    }

    protected static String getLaterText() {
        return laterText;
    }

    protected static void setLaterText(String laterText) {
        XdConstants.laterText = laterText;
    }

    protected static String getHintText() {
        return hintText;
    }

    protected static void setHintText(String hintText) {
        XdConstants.hintText = hintText;
    }

    protected static String getDownloadingText() {
        return downloadingText;
    }

    protected static void setDownloadingText(String downloadingText) {
        XdConstants.downloadingText = downloadingText;
    }
}
