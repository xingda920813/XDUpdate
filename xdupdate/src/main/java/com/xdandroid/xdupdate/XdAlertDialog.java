package com.xdandroid.xdupdate;

import android.app.AlertDialog;

/**
 * Created by XingDa on 2016/5/6.
 */
public class XdAlertDialog {

    private AlertDialog dialog;

    XdAlertDialog(AlertDialog dialog) {
        this.dialog = dialog;
    }

    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }
}
