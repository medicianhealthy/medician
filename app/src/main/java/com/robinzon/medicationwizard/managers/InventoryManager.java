package com.robinzon.medicationwizard.managers;

import android.content.Context;
import android.widget.Toast;

import com.robinzon.medicationwizard.AppConfig;
import com.robinzon.medicationwizard.BuildConfig;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.entities.Medication;
import com.robinzon.medicationwizard.notifications.NotificationManager;

import java.util.ArrayList;

public class InventoryManager {

    public static void decrementInventory(Context context, String medicationId, float amount) {
        if (!AppConfig.isFeatureUnlocked(context, AppConfig.FeaturePassType.INVENTORY)) return;

        ArrayList<Medication> meds = Medication.getSavedMedications(context);
        for (Medication m : meds) {
            if (m.getId().equals(medicationId)) {
                float oldVal = m.getInventoryCurrent();
                float newVal = Math.max(0, oldVal - amount);
                m.setInventoryCurrent(newVal);
                m.addToMedicationList(context);

                if (BuildConfig.DEBUG) {
                    Toast.makeText(context, context.getString(R.string.inventory_debug_toast, String.valueOf(oldVal), String.valueOf(newVal)), Toast.LENGTH_SHORT).show();
                }

                checkThreshold(context, m);
                break;
            }
        }
    }

    private static void checkThreshold(Context context, Medication m) {
        if (m.getInventoryThreshold() <= 0) return;

        boolean shouldAlert = false;
        String alertMsg = "";

        if (m.getInventoryAlertType() == Medication.InventoryAlertType.AMOUNT_REACHED) {
            if (m.getInventoryCurrent() <= m.getInventoryThreshold()) {
                shouldAlert = true;
                if (m.getInventoryCurrent() <= 0) {
                    alertMsg = context.getString(R.string.inventory_empty_stock_notif_msg, m.getCommercialName());
                } else {
                    String unit = m.getMeasurementUnit() != null ? m.getMeasurementUnit().getLabel(context) : "";
                    alertMsg = context.getString(R.string.inventory_low_stock_notif_msg, m.getCommercialName(), m.getInventoryCurrent() + " " + unit);
                }
            }
        } else {
            // Days Before logic
            int frequency = m.getDailyFrequency();
            if (frequency > 0) {
                float amountPerDay = frequency * m.getAmount();
                float daysLeft = m.getInventoryCurrent() / amountPerDay;
                if (daysLeft <= m.getInventoryThreshold()) {
                    shouldAlert = true;
                    int intDays = (int) Math.ceil(daysLeft);
                    String timeLabel = intDays + " " + context.getString(R.string.unit_days);
                    alertMsg = context.getString(R.string.inventory_low_stock_notif_msg, m.getCommercialName(), timeLabel);
                }
            }
        }

        if (shouldAlert) {
            NotificationManager.postInventoryAlert(context, m.getCommercialName(), alertMsg);
        }
    }
}
