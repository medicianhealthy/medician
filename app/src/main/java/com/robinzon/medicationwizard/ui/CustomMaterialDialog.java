package com.robinzon.medicationwizard.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;

import androidx.annotation.NonNull;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class CustomMaterialDialog {

    private final MaterialAlertDialogBuilder builder;
    private Dialog dialog;

    public CustomMaterialDialog(@NonNull Context context) {
        builder = new MaterialAlertDialogBuilder(context);
    }

    public void setTitle(String title) {
        builder.setTitle(title);
    }

    public void setMessage(String message) {
        builder.setMessage(message);
    }

    public void setPositiveButton(String text, DialogInterface.OnClickListener listener) {
        builder.setPositiveButton(text, listener);
    }

    public void setNegativeButton(String text, DialogInterface.OnClickListener listener) {
        builder.setNegativeButton(text, listener);
    }

    public void setNeutralButton(String text, DialogInterface.OnClickListener listener) {
        builder.setNeutralButton(text, listener);
    }

    public void setOnDismissListener(DialogInterface.OnDismissListener listener) {
        builder.setOnDismissListener(listener);
    }

    public void show() {
        if (dialog != null) {
            dialog.dismiss();
        }
        dialog = builder.create();
        dialog.show();
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}

