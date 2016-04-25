package com.xdandroid.xdupdate;

import java.io.Serializable;

/**
 * Created by XingDa on 2016/04/24.
 */
public class XdUpdateBean implements Serializable {

    private int versionCode,size;
    private String versionName,url,note,md5;

    public String getMd5() {
        return md5;
    }

    public void setMd5(String md5) {
        this.md5 = md5;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    @Override
    public String toString() {
        return "XdUpdateBean{" +
                "md5='" + md5 + '\'' +
                ", versionCode=" + versionCode +
                ", size=" + size +
                ", versionName='" + versionName + '\'' +
                ", url='" + url + '\'' +
                ", note='" + note + '\'' +
                '}';
    }
}
