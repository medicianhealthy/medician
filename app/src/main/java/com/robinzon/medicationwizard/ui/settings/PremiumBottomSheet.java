package com.robinzon.medicationwizard.ui.settings;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.tabs.TabLayoutMediator;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.billing.BillingManager;
import com.robinzon.medicationwizard.ui.MedicationWizardBottomSheet;

import java.util.ArrayList;
import java.util.List;

/**
 * A specialized BottomSheet for promoting and handling Premium upgrades.
 */
public class PremiumBottomSheet extends MedicationWizardBottomSheet {

    private static final long AUTO_SCROLL_DELAY = 5000L; // 5 seconds
    private final Handler autoScrollHandler = new Handler(Looper.getMainLooper());
    private ViewPager2 benefitPager;
    private TabLayoutMediator tabLayoutMediator;
    private Runnable autoScrollRunnable;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialog);
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            View bottomSheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
            if (bottomSheet != null) {
                BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(bottomSheet);
                behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                behavior.setSkipCollapsed(true);

                // Set the bottom sheet container background to transparent to let our gradient show
                bottomSheet.setBackground(new ColorDrawable(Color.TRANSPARENT));
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_premium, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        benefitPager = view.findViewById(R.id.benefit_pager);
        setupBenefitCarousel(view);

        view.findViewById(R.id.btn_upgrade).setOnClickListener(v -> {
            if (getActivity() != null) {
                BillingManager.getInstance(requireContext()).launchPurchaseFlow(getActivity());
                dismiss();
            }
        });

        view.findViewById(R.id.btn_maybe_later).setOnClickListener(v -> dismiss());
    }

    private void setupBenefitCarousel(View view) {
        List<Benefit> benefits = new ArrayList<>();
        benefits.add(new Benefit(getString(R.string.premium_benefit_ad_free_title), getString(R.string.premium_benefit_ad_free_desc), R.drawable.ic_sparkle));
        if (com.robinzon.medicationwizard.AppConfig.CLOUD_BACKUP_ENABLED) {
            benefits.add(new Benefit(getString(R.string.premium_benefit_cloud_title), getString(R.string.premium_benefit_cloud_desc), R.drawable.ic_cloud_upload));
        } else {
            benefits.add(new Benefit(getString(R.string.premium_benefit_backup_title), getString(R.string.premium_benefit_backup_desc), R.drawable.ic_cloud_upload));
        }
        benefits.add(new Benefit(getString(R.string.premium_benefit_rest_title), getString(R.string.premium_benefit_rest_desc), R.drawable.ic_nightlight));
        benefits.add(new Benefit(getString(R.string.premium_benefit_support_title), getString(R.string.premium_benefit_support_desc), R.drawable.ic_help_outline));

        BenefitAdapter adapter = new BenefitAdapter(benefits);
        benefitPager.setAdapter(adapter);

        // 1. Initialize runnable FIRST to avoid NPE in onPageSelected
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (benefitPager == null || !isAdded()) return;
                int count = adapter.getItemCount();
                if (count == 0) return;
                int nextItem = (benefitPager.getCurrentItem() + 1) % count;
                benefitPager.setCurrentItem(nextItem, true);
            }
        };

        // 2. Setup indicator
        tabLayoutMediator = new TabLayoutMediator(view.findViewById(R.id.tab_indicator), benefitPager, (tab, position) -> {
        });
        tabLayoutMediator.attach();

        // 3. Register callback
        benefitPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (autoScrollRunnable != null) {
                    autoScrollHandler.removeCallbacks(autoScrollRunnable);
                    autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY);
                }
            }
        });

        // 4. Start initial scroll
        autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY);
    }

    @Override
    public void onDestroyView() {
        autoScrollHandler.removeCallbacksAndMessages(null);
        if (tabLayoutMediator != null) {
            tabLayoutMediator.detach();
            tabLayoutMediator = null;
        }
        benefitPager = null;
        super.onDestroyView();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
            autoScrollHandler.postDelayed(autoScrollRunnable, AUTO_SCROLL_DELAY);
        }
    }

    private static class Benefit {
        final String title;
        final String desc;
        final int iconRes;

        Benefit(String title, String desc, int iconRes) {
            this.title = title;
            this.desc = desc;
            this.iconRes = iconRes;
        }
    }

    private static class BenefitAdapter extends RecyclerView.Adapter<BenefitAdapter.ViewHolder> {
        private final List<Benefit> data;

        BenefitAdapter(List<Benefit> data) {
            this.data = data;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_premium_benefit, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Benefit b = data.get(position);
            holder.title.setText(b.title);
            holder.desc.setText(b.desc);
            holder.icon.setImageResource(b.iconRes);

            // All premium text on gradient should be white/off-white for contrast
            holder.title.setTextColor(Color.WHITE);
            holder.desc.setTextColor(Color.parseColor("#CCFFFFFF"));
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView title, desc;
            final ImageView icon;

            ViewHolder(View v) {
                super(v);
                title = v.findViewById(R.id.txt_benefit_title);
                desc = v.findViewById(R.id.txt_benefit_desc);
                icon = v.findViewById(R.id.img_benefit_icon);
            }
        }
    }
}
