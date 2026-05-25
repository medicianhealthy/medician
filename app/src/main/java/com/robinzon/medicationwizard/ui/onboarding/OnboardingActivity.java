package com.robinzon.medicationwizard.ui.onboarding;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.databinding.ActivityOnboardingBinding;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    public static final String KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding";
    private ActivityOnboardingBinding binding;
    private final List<ImageView> dots = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        List<OnboardingPage> pages = new ArrayList<>();
        pages.add(new OnboardingPage(
                R.drawable.ic_magic_wand, 
                R.drawable.ic_med_pill,
                getString(R.string.onboarding_title_1), 
                getString(R.string.onboarding_desc_1)));
        
        pages.add(new OnboardingPage(
                R.drawable.ic_clock, 
                R.drawable.ic_med_drops,
                getString(R.string.onboarding_title_2), 
                getString(R.string.onboarding_desc_2)));
        
        pages.add(new OnboardingPage(
                R.drawable.ic_list, 
                R.drawable.ic_magic_wand,
                getString(R.string.onboarding_title_3), 
                getString(R.string.onboarding_desc_3)));

        setupDots(pages.size());
        
        OnboardingAdapter adapter = new OnboardingAdapter(pages);
        binding.viewPager.setAdapter(adapter);

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateDots(position);
                if (position == pages.size() - 1) {
                    binding.btnNext.setText(R.string.onboarding_btn_finish);
                } else {
                    binding.btnNext.setText(R.string.onboarding_btn_next);
                }
            }
        });

        binding.btnNext.setOnClickListener(v -> {
            if (binding.viewPager.getCurrentItem() < pages.size() - 1) {
                binding.viewPager.setCurrentItem(binding.viewPager.getCurrentItem() + 1);
            } else {
                finishOnboarding();
            }
        });

        binding.btnSkip.setOnClickListener(v -> finishOnboarding());
    }

    private void setupDots(int count) {
        binding.dotsIndicatorContainer.removeAllViews();
        dots.clear();
        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(this);
            int size = (int) (8 * getResources().getDisplayMetrics().density);
            int margin = (int) (4 * getResources().getDisplayMetrics().density);
            
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);
            dot.setImageResource(R.drawable.dot_indicator);
            
            binding.dotsIndicatorContainer.addView(dot);
            dots.add(dot);
        }
        updateDots(0);
    }

    private void updateDots(int position) {
        for (int i = 0; i < dots.size(); i++) {
            dots.get(i).setSelected(i == position);
            // Visual feedback: active dot is slightly larger
            float scale = (i == position) ? 1.2f : 1.0f;
            dots.get(i).animate().scaleX(scale).scaleY(scale).setDuration(200).start();
        }
    }

    private void finishOnboarding() {
        SharedPreferencesManager.getInstance(this).setBoolean(KEY_HAS_SEEN_ONBOARDING, true);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private static class OnboardingPage {
        int accessory1;
        int accessory2;
        String title;
        String description;

        OnboardingPage(int acc1, int acc2, String title, String description) {
            this.accessory1 = acc1;
            this.accessory2 = acc2;
            this.title = title;
            this.description = description;
        }
    }

    private static class OnboardingAdapter extends RecyclerView.Adapter<OnboardingAdapter.ViewHolder> {
        private final List<OnboardingPage> pages;

        OnboardingAdapter(List<OnboardingPage> pages) {
            this.pages = pages;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_onboarding_page, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            OnboardingPage page = pages.get(position);
            holder.title.setText(page.title);
            holder.desc.setText(page.description);
            holder.acc1.setImageResource(page.accessory1);
            holder.acc2.setImageResource(page.accessory2);
            
            // Use the brand new High-Definition Vector Wizard for perfect sharpness
            holder.mascot.setImageResource(R.drawable.ic_wizard_high_def);
            
            // Apply balanced floating animations
            startFloatingAnimation(holder.mascot, 0, -35f, 3200);
            startFloatingAnimation(holder.acc1, 200, 25f, 3500); 
            startFloatingAnimation(holder.acc2, 400, -20f, 4000); 
            
            // Add twinkling animations to background stars
            startTwinkleAnimation(holder.s1, 100);
            startTwinkleAnimation(holder.s2, 500);
            startTwinkleAnimation(holder.s3, 900);
            startTwinkleAnimation(holder.s4, 1300);
        }

        private void startTwinkleAnimation(View view, long delay) {
            ObjectAnimator alpha = ObjectAnimator.ofFloat(view, "alpha", 0.2f, 1.0f, 0.2f);
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1.2f, 0.8f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1.2f, 0.8f);
            
            alpha.setDuration(2500);
            scaleX.setDuration(2500);
            scaleY.setDuration(2500);
            
            alpha.setStartDelay(delay);
            scaleX.setStartDelay(delay);
            scaleY.setStartDelay(delay);
            
            alpha.setRepeatCount(ValueAnimator.INFINITE);
            scaleX.setRepeatCount(ValueAnimator.INFINITE);
            scaleY.setRepeatCount(ValueAnimator.INFINITE);
            
            alpha.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
            
            alpha.start();
            scaleX.start();
            scaleY.start();
        }

        private void startFloatingAnimation(View view, long delay, float distance, long duration) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationY", 0f, distance, 0f);
            animator.setDuration(duration);
            animator.setStartDelay(delay);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.start();
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView mascot, acc1, acc2;
            ImageView s1, s2, s3, s4;
            TextView title, desc;

            ViewHolder(@NonNull View itemView) {
                super(itemView);
                mascot = itemView.findViewById(R.id.img_mascot);
                acc1 = itemView.findViewById(R.id.img_accessory_1);
                acc2 = itemView.findViewById(R.id.img_accessory_2);
                
                s1 = itemView.findViewById(R.id.sparkle_1);
                s2 = itemView.findViewById(R.id.sparkle_2);
                s3 = itemView.findViewById(R.id.sparkle_3);
                s4 = itemView.findViewById(R.id.sparkle_4);

                title = itemView.findViewById(R.id.txt_title);
                desc = itemView.findViewById(R.id.txt_description);
            }
        }
    }
}
