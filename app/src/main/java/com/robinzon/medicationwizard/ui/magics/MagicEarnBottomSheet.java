package com.robinzon.medicationwizard.ui.magics;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.ads.AdsManager;
import com.robinzon.medicationwizard.managers.MagicManager;
import com.robinzon.medicationwizard.ui.MedicationWizardBottomSheet;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import android.content.Intent;
import android.net.Uri;
import android.content.pm.ResolveInfo;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class MagicEarnBottomSheet extends MedicationWizardBottomSheet {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_magic_earn, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateBalance(view);

        SharedPreferencesManager sp = SharedPreferencesManager.getInstance(requireContext());
        long lastShare = sp.getLong("magic_last_share", 0);
        long now = com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal();

        java.util.Calendar lastCal = java.util.Calendar.getInstance();
        lastCal.setTimeInMillis(lastShare);
        java.util.Calendar nowCal = java.util.Calendar.getInstance();
        nowCal.setTimeInMillis(now);

        boolean alreadySharedToday = lastShare != 0 &&
                lastCal.get(java.util.Calendar.DAY_OF_YEAR) == nowCal.get(java.util.Calendar.DAY_OF_YEAR) &&
                lastCal.get(java.util.Calendar.YEAR) == nowCal.get(java.util.Calendar.YEAR);

        long lastStory = sp.getLong("magic_last_story", 0);
        java.util.Calendar storyCal = java.util.Calendar.getInstance();
        storyCal.setTimeInMillis(lastStory);
        boolean alreadyStoryToday = lastStory != 0 &&
                storyCal.get(java.util.Calendar.DAY_OF_YEAR) == nowCal.get(java.util.Calendar.DAY_OF_YEAR) &&
                storyCal.get(java.util.Calendar.YEAR) == nowCal.get(java.util.Calendar.YEAR);

        View shareCard = view.findViewById(R.id.btn_earn_share);
        if (alreadySharedToday) {
            shareCard.setAlpha(0.5f);
        }

        View storyCard = view.findViewById(R.id.btn_earn_story);
        if (alreadyStoryToday) {
            storyCard.setAlpha(0.5f);
        }

        View rateCard = view.findViewById(R.id.btn_earn_rate);
        boolean alreadyRated = sp.getBoolean("magic_has_rated", false);
        if (alreadyRated) {
            rateCard.setAlpha(0.5f);
        }

        view.findViewById(R.id.btn_earn_rv).setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity main) {
                main.getAdsManager().showRewarded(status -> {
                    if (status == AdsManager.RewardedStatus.SUCCESS) {
                        MagicManager.getInstance(requireContext()).addMagics(1);
                        Toast.makeText(requireContext(), getString(R.string.magic_earned_toast, 1), Toast.LENGTH_SHORT).show();
                        updateBalance(view);
                    } else {
                        Toast.makeText(requireContext(), R.string.reward_ad_not_ready, Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });

        view.findViewById(R.id.btn_earn_share).setOnClickListener(v -> {
            if (alreadySharedToday) {
                Toast.makeText(requireContext(), R.string.magic_share_limit_toast, Toast.LENGTH_SHORT).show();
                return;
            }
            if (getActivity() instanceof MainActivity main) {
                main.shareToWhatsApp();
                dismiss();
            }
        });

        view.findViewById(R.id.btn_earn_story).setOnClickListener(v -> {
            if (alreadyStoryToday) {
                Toast.makeText(requireContext(), R.string.magic_share_limit_toast, Toast.LENGTH_SHORT).show();
                return;
            }

            // Create target intent
            Intent targetIntent = new Intent(Intent.ACTION_SEND);
            targetIntent.setType("text/plain");
            targetIntent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_message));

            List<Intent> targetedShareIntents = new ArrayList<>();
            List<ResolveInfo> resInfo = requireContext().getPackageManager().queryIntentActivities(targetIntent, 0);

            if (!resInfo.isEmpty()) {
                for (ResolveInfo info : resInfo) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("text/plain");
                    intent.putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_message));
                    String packageName = info.activityInfo.packageName.toLowerCase();

                    // Filter only Facebook and Instagram packages
                    if (packageName.contains("facebook") || packageName.contains("instagram")) {
                        intent.setPackage(info.activityInfo.packageName);
                        targetedShareIntents.add(intent);
                    }
                }

                if (targetedShareIntents.isEmpty()) {
                    Toast.makeText(requireContext(), "Facebook or Instagram not found!", Toast.LENGTH_SHORT).show();
                    return;
                }

                Intent chooserIntent;
                if (targetedShareIntents.size() == 1) {
                    chooserIntent = targetedShareIntents.get(0);
                } else {
                    chooserIntent = Intent.createChooser(targetedShareIntents.remove(0), getString(R.string.magic_earn_story_title));
                    chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, targetedShareIntents.toArray(new Parcelable[]{}));
                }
                startActivity(chooserIntent);

                // Reward (simulated)
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    MagicManager.getInstance(requireContext()).addMagics(3);
                    sp.setLong("magic_last_story", com.robinzon.medicationwizard.utils.TimeManager.getInstance().getCurrentTimeInMillisFakeOrReal());
                    Toast.makeText(requireContext(), getString(R.string.magic_earned_toast, 3), Toast.LENGTH_SHORT).show();
                    dismiss();
                }, 2000);
            }
        });

        view.findViewById(R.id.btn_earn_rate).setOnClickListener(v -> {
            if (alreadyRated) {
                Toast.makeText(requireContext(), R.string.magic_rate_limit_toast, Toast.LENGTH_SHORT).show();
                return;
            }

            try {
                String pkg = requireContext().getPackageName();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + pkg)));
            } catch (Exception e) {
                String pkg = requireContext().getPackageName();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + pkg)));
            }

            MagicManager.getInstance(requireContext()).addMagics(3);
            sp.setBoolean("magic_has_rated", true);
            Toast.makeText(requireContext(), getString(R.string.magic_earned_toast, 3), Toast.LENGTH_SHORT).show();
            dismiss(); // Dismiss after rating as it's one-time and balance updates in background
        });
    }

    private void updateBalance(View view) {
        TextView balanceView = view.findViewById(R.id.txt_magic_balance);
        int balance = MagicManager.getInstance(requireContext()).getMagicBalance();
        balanceView.setText(getString(R.string.magic_balance_format, balance));
    }
}
