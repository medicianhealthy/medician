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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.google.android.material.color.MaterialColors;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.shape.MaterialShapeDrawable;
import com.google.android.material.shape.ShapeAppearanceModel;

/**
 * Branded Dialog wrapper for Medication Wizard.
 * Provides the "Magical" look: 28dp corners, 1dp primary stroke, and centered headers.
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
        float strokeWidth = 1 * density; // Softened from 2dp to 1dp

        // 1. Geometry: 28dp Rounded Corners
        ShapeAppearanceModel shapeAppearanceModel = new ShapeAppearanceModel.Builder()
                .setAllCornerSizes(cornerRadius)
                .build();

        MaterialShapeDrawable shapeDrawable = new MaterialShapeDrawable(shapeAppearanceModel);

        // 2. Magic Border: Primary Colored Stroke (Subtle 1dp)
        int surfaceAttr = context.getResources().getIdentifier("colorSurface", "attr", context.getPackageName());
        int primaryAttr = context.getResources().getIdentifier("colorPrimary", "attr", context.getPackageName());

        int surfaceColor = MaterialColors.getColor(context, surfaceAttr, Color.WHITE);
        int primaryColor = MaterialColors.getColor(context, primaryAttr, Color.BLUE);

        shapeDrawable.setFillColor(ColorStateList.valueOf(surfaceColor));
        shapeDrawable.setStroke(strokeWidth, primaryColor);

        builder.setBackground(shapeDrawable);
    }

    public void setTitle(String title) {
        // Simple start-aligned title using standard Material text styling
        builder.setTitle(title);
    }

    public void setMessage(String message) {
        // High-end readable text layout: Start-aligned and balanced to avoid orphans.
        TextView messageView = new TextView(context);
        messageView.setText(message);
        messageView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

        // HIGH_QUALITY/BALANCED break strategy to prevent "lonely words"
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            // Constant 1 is BREAK_STRATEGY_HIGH_QUALITY, 2 is BREAK_STRATEGY_BALANCED
            // High quality performs more aggressive optimization to avoid orphans
            messageView.setBreakStrategy(1);
            messageView.setHyphenationFrequency(android.text.Layout.HYPHENATION_FREQUENCY_FULL);
        }

        // Start alignment (Left for LTR, Right for RTL)
        messageView.setGravity(Gravity.START);

        float density = context.getResources().getDisplayMetrics().density;
        int paddingSide = (int) (24 * density);
        int paddingTop = 0;
        int paddingBottom = (int) (8 * density);

        messageView.setPadding(paddingSide, paddingTop, paddingSide, paddingBottom);

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

    public void setSingleChoiceItems(CharSequence[] items, int checkedItem, final DialogInterface.OnClickListener listener) {
        builder.setSingleChoiceItems(items, checkedItem, listener);
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
                positiveButton.setAllCaps(false); // M3 compliant sentence-case
            }

            Button negativeButton = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
            if (negativeButton != null) {
                int variantAttr = context.getResources().getIdentifier("colorOnSurfaceVariant", "attr", context.getPackageName());
                int secondaryColor = MaterialColors.getColor(context, variantAttr, Color.GRAY);
                negativeButton.setTextColor(secondaryColor);
                negativeButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                negativeButton.setAllCaps(false); // M3 compliant sentence-case
            }

            Button neutralButton = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
            if (neutralButton != null) {
                int variantAttr = context.getResources().getIdentifier("colorOnSurfaceVariant", "attr", context.getPackageName());
                int secondaryColor = MaterialColors.getColor(context, variantAttr, Color.GRAY);
                neutralButton.setTextColor(secondaryColor);
                neutralButton.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
                neutralButton.setAllCaps(false); // M3 compliant sentence-case
            }
        }
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
