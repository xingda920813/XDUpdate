package com.xdandroid.xdupdate;

import android.app.AlertDialog;

/**
 * Created by XingDa on 2016/05/28.
 */

public class XdAlertDialog {

    protected AlertDialog dialog;

    public XdAlertDialog(AlertDialog dialog) {
        this.dialog = dialog;
    }

    public AlertDialog getDialog() {
        return dialog;
    }

    public void dismiss() {
        if (dialog != null) {
            try {
                dialog.dismiss();
            } catch (Throwable t) {
                t.printStackTrace(System.err);
            }
        }
    }
}
