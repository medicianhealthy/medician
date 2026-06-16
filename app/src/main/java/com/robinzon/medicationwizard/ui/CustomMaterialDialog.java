package com.robinzon.medicationwizard.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;
import com.robinzon.medicationwizard.R;

/**
 * Branded Dialog wrapper for Medication Wizard.
 * Provides the "Magical" look: 28dp corners, 2dp primary stroke, and symmetrical wand headers.
 */
public class CustomMaterialDialog {

    private final MaterialAlertDialogBuilder builder;
    private final Context context;
    private Dialog dialog;

    public CustomMaterialDialog(@NonNull Context context) {
        this.context = context;
        this.builder = new MaterialAlertDialogBuilder(context);
        applyMagicalStyling();
    }

    private void applyMagicalStyling() {
        float density = context.getResources().getDisplayMetrics().density;
        float cornerRadius = 28 * density;
        float strokeWidth = 2 * density;

        // 1. Geometry: 28dp Rounded Corners
        ShapeAppearanceModel shapeAppearanceModel = new ShapeAppearanceModel.Builder()
                .setAllCornerSizes(cornerRadius)
                .build();

        MaterialShapeDrawable shapeDrawable = new MaterialShapeDrawable(shapeAppearanceModel);
        
        // 2. Magic Border: Primary Colored Stroke
        int surfaceAttr = context.getResources().getIdentifier("colorSurface", "attr", context.getPackageName());
        int primaryAttr = context.getResources().getIdentifier("colorPrimary", "attr", context.getPackageName());
        
        int surfaceColor = MaterialColors.getColor(context, surfaceAttr, Color.WHITE);
        int primaryColor = MaterialColors.getColor(context, primaryAttr, Color.BLUE);
        
        shapeDrawable.setFillColor(ColorStateList.valueOf(surfaceColor));
        shapeDrawable.setStroke(strokeWidth, primaryColor);
        
        builder.setBackground(shapeDrawable);
    }

    public void setTitle(String title) {
        // 3. Magical Header: Centered title with symmetrical magic wands
        LinearLayout titleLayout = new LinearLayout(context);
        titleLayout.setOrientation(LinearLayout.HORIZONTAL);
        titleLayout.setGravity(Gravity.CENTER);
        int padding = (int) (24 * context.getResources().getDisplayMetrics().density);
        titleLayout.setPadding(padding, padding, padding, (int) (padding * 0.2));

        int primaryAttr = context.getResources().getIdentifier("colorPrimary", "attr", context.getPackageName());
        int primaryColor = MaterialColors.getColor(context, primaryAttr, Color.BLUE);
        int iconSize = (int) (24 * context.getResources().getDisplayMetrics().density);

        ImageView leftIcon = new ImageView(context);
        leftIcon.setImageResource(R.drawable.ic_magic_wand);
        leftIcon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        leftIcon.setImageTintList(ColorStateList.valueOf(primaryColor));

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        titleView.setTypeface(null, Typeface.BOLD);
        titleView.setTextColor(primaryColor);
        titleView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        int textMargin = (int) (12 * context.getResources().getDisplayMetrics().density);
        textParams.setMargins(textMargin, 0, textMargin, 0);
        titleView.setLayoutParams(textParams);

        ImageView rightIcon = new ImageView(context);
        rightIcon.setImageResource(R.drawable.ic_magic_wand);
        rightIcon.setLayoutParams(new LinearLayout.LayoutParams(iconSize, iconSize));
        rightIcon.setImageTintList(ColorStateList.valueOf(primaryColor));
        rightIcon.setScaleX(-1); // Flip horizontally for symmetry

        titleLayout.addView(leftIcon);
        titleLayout.addView(titleView);
        titleLayout.addView(rightIcon);
        
        builder.setCustomTitle(titleLayout);
    }

    public void setMessage(String message) {
        // We wrap the message to ensure it's centered and has better padding
        TextView messageView = new TextView(context);
        messageView.setText(message);
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        messageView.setGravity(Gravity.CENTER);
        int padding = (int) (20 * context.getResources().getDisplayMetrics().density);
        messageView.setPadding(padding, (int)(padding * 0.5), padding, padding);
        
        int variantAttr = context.getResources().getIdentifier("colorOnSurfaceVariant", "attr", context.getPackageName());
        int textColor = MaterialColors.getColor(context, variantAttr, Color.GRAY);
        messageView.setTextColor(textColor);
        
        builder.setView(messageView);
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

    public void setItems(CharSequence[] items, final DialogInterface.OnClickListener listener) {
        builder.setItems(items, listener);
    }

    public void setView(View view) {
        builder.setView(view);
    }

    public void show() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
        dialog = builder.create();
        dialog.show();
        
        // 4. Guided Actions: Bold Primary Buttons
        if (dialog instanceof AlertDialog) {
            AlertDialog alertDialog = (AlertDialog) dialog;
            
            Button positiveButton = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
            if (positiveButton != null) {
                positiveButton.setTypeface(null, Typeface.BOLD);
                int primaryAttr = context.getResources().getIdentifier("colorPrimary", "attr", context.getPackageName());
                int primaryColor = MaterialColors.getColor(context, primaryAttr, Color.BLUE);
                positiveButton.setTextColor(primaryColor);
                positiveButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                positiveButton.setAllCaps(true);
            }
            
            Button negativeButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            if (negativeButton != null) {
                int variantAttr = context.getResources().getIdentifier("colorOnSurfaceVariant", "attr", context.getPackageName());
                int secondaryColor = MaterialColors.getColor(context, variantAttr, Color.GRAY);
                negativeButton.setTextColor(secondaryColor);
                negativeButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                negativeButton.setAllCaps(false);
            }
        }
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
