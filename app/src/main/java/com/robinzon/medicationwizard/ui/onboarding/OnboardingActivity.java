package com.robinzon.medicationwizard.ui.onboarding;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.snackbar.Snackbar;
import com.robinzon.medicationwizard.MainActivity;
import com.robinzon.medicationwizard.R;
import com.robinzon.medicationwizard.databinding.ActivityOnboardingBinding;
import com.robinzon.medicationwizard.utils.SharedPreferencesManager;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {

    public static final String KEY_HAS_SEEN_ONBOARDING = "has_seen_onboarding";
    private final List<ImageView> dots = new ArrayList<>();
    private ActivityOnboardingBinding binding;
    private int pageBeforeSkip = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOnboardingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Resolve status bar overlap: Apply window insets to the root view
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            androidx.core.graphics.Insets bars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });

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
        setupTerms();

        OnboardingAdapter adapter = new OnboardingAdapter(pages);
        binding.viewPager.setAdapter(adapter);

        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateUiForPage(position, pages.size());
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                if (state == ViewPager2.SCROLL_STATE_DRAGGING) {
                    // Reset skip history if user starts manual navigation
                    pageBeforeSkip = -1;
                }
            }
        });

        // Ensure initial UI state is correct (crucial for rotation/restoration)
        binding.viewPager.post(() -> updateUiForPage(binding.viewPager.getCurrentItem(), pages.size()));

        binding.btnNext.setOnClickListener(v -> {
            pageBeforeSkip = -1; // Reset skip history on manual navigation
            if (binding.viewPager.getCurrentItem() < pages.size() - 1) {
                binding.viewPager.setCurrentItem(binding.viewPager.getCurrentItem() + 1);
            } else {
                if (binding.checkTerms.isChecked()) {
                    finishOnboarding();
                } else {
                    shakeTermsContainer();
                    Snackbar.make(binding.getRoot(), R.string.onboarding_error_terms, Snackbar.LENGTH_LONG).show();
                }
            }
        });

        binding.btnSkip.setOnClickListener(v -> {
            // Fix: Skip now navigates to the final page to ensure Terms agreement
            if (!pages.isEmpty()) {
                pageBeforeSkip = binding.viewPager.getCurrentItem();
                binding.viewPager.setCurrentItem(pages.size() - 1, true);
            }
        });

        // Handle Back Button: Navigate to previous slide instead of closing the app
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                int currentItem = binding.viewPager.getCurrentItem();
                if (currentItem > 0) {
                    if (pageBeforeSkip != -1 && currentItem == pages.size() - 1) {
                        // Reverse the "Skip" jump
                        binding.viewPager.setCurrentItem(pageBeforeSkip, true);
                        pageBeforeSkip = -1;
                    } else {
                        // Standard linear back navigation
                        binding.viewPager.setCurrentItem(currentItem - 1, true);
                        pageBeforeSkip = -1;
                    }
                } else {
                    // First slide: Close the app (standard behavior)
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });
    }

    private void shakeTermsContainer() {
        ObjectAnimator shake = ObjectAnimator.ofFloat(binding.containerTerms, "translationX", 0, 25, -25, 25, -25, 15, -15, 6, -6, 0);
        shake.setDuration(500);
        shake.start();

        // Optional: provide haptic feedback
        binding.containerTerms.performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS);
    }

    private void setupTerms() {
        String termsUrl = getString(R.string.url_terms).trim();
        String privacyUrl = getString(R.string.url_privacy).trim();
        String textTemplate = getString(R.string.onboarding_terms_agree, termsUrl, privacyUrl);

        Spanned html = Html.fromHtml(textTemplate, Html.FROM_HTML_MODE_COMPACT);
        SpannableStringBuilder spannable = new SpannableStringBuilder(html);
        URLSpan[] spans = spannable.getSpans(0, spannable.length(), URLSpan.class);

        for (URLSpan span : spans) {
            int start = spannable.getSpanStart(span);
            int end = spannable.getSpanEnd(span);
            String url = span.getURL();

            spannable.removeSpan(span);
            spannable.setSpan(new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    openInternalUrl(url);
                }
            }, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        binding.txtTerms.setText(spannable);
        binding.txtTerms.setMovementMethod(LinkMovementMethod.getInstance());

        // Fix: Added click listener to the TextView itself to catch taps outside links
        View.OnClickListener toggleListener = v -> binding.checkTerms.toggle();
        binding.containerTerms.setOnClickListener(toggleListener);
        binding.txtTerms.setOnClickListener(toggleListener);
    }

    private void openInternalUrl(String url) {
        Intent intent = new Intent(this, WebViewerActivity.class);
        intent.putExtra(WebViewerActivity.EXTRA_URL, url);

        int titleResId = url.contains("terms") ? R.string.legal_terms_title : R.string.legal_privacy_title;
        intent.putExtra(WebViewerActivity.EXTRA_TITLE, getString(titleResId));
        startActivity(intent);
    }

    private void setupDots(int count) {
        binding.dotsIndicatorContainer.removeAllViews();
        dots.clear();
        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(this);
            int size = (int) (12 * getResources().getDisplayMetrics().density);
            int margin = (int) (2 * getResources().getDisplayMetrics().density);

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

    private void updateUiForPage(int position, int totalPages) {
        updateDots(position);
        boolean isLastPage = position == totalPages - 1;
        binding.btnNext.setText(isLastPage ? R.string.onboarding_btn_finish : R.string.onboarding_btn_next);
        binding.containerTerms.setVisibility(isLastPage ? View.VISIBLE : View.GONE);

        // Hide Skip button on the last page to ensure agreement
        binding.btnSkip.setVisibility(isLastPage ? View.GONE : View.VISIBLE);
    }

    private void finishOnboarding() {
        SharedPreferencesManager.getInstance(this).setBoolean(KEY_HAS_SEEN_ONBOARDING, true);
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
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
            holder.cancelAnimations();
            
            // Reset recycled view state
            holder.scrollHint.setAlpha(1.0f);
            holder.scrollHint.setTranslationY(0f);
            holder.scrollHint.setVisibility(View.GONE);
            holder.scrollHint.clearAnimation();

            OnboardingPage page = pages.get(position);
            holder.title.setText(page.title);
            holder.desc.setText(page.description);
            holder.acc1.setImageResource(page.accessory1);
            holder.acc2.setImageResource(page.accessory2);

            // Use the brand new High-Definition Vector Wizard for perfect sharpness
            holder.mascot.setImageResource(R.drawable.ic_wizard_high_def);

            // Apply balanced floating animations
            holder.animators.add(startFloatingAnimation(holder.mascot, 0, -35f, 3200));
            holder.animators.add(startFloatingAnimation(holder.acc1, 200, 25f, 3500));
            holder.animators.add(startFloatingAnimation(holder.acc2, 400, -20f, 4000));

            // Add twinkling animations to background stars
            holder.animators.addAll(startTwinkleAnimation(holder.s1, 100));
            holder.animators.addAll(startTwinkleAnimation(holder.s2, 500));
            holder.animators.addAll(startTwinkleAnimation(holder.s3, 900));
            holder.animators.addAll(startTwinkleAnimation(holder.s4, 1300));

            // Logic: The scroll hint should only be shown once per page
            final String prefKey = "hint_seen_onboarding_" + position;
            final android.content.Context context = holder.itemView.getContext();

            if (SharedPreferencesManager.getInstance(context).getBoolean(prefKey, false)) {
                holder.scrollHint.setVisibility(View.GONE);
            } else {
                // Use GlobalLayoutListener for reliable measurement
                ViewTreeObserver.OnGlobalLayoutListener layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (holder.getBindingAdapterPosition() == RecyclerView.NO_POSITION) {
                            holder.scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            return;
                        }

                        if (SharedPreferencesManager.getInstance(context).getBoolean(prefKey, false)) {
                            holder.scrollHint.setVisibility(View.GONE);
                            holder.scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            return;
                        }

                        boolean canScroll = holder.scrollView.canScrollVertically(1);
                        holder.scrollHint.setVisibility(canScroll ? View.VISIBLE : View.GONE);
                        if (canScroll) {
                            // Start subtle bouncing animation for the hint
                            ObjectAnimator bounce = ObjectAnimator.ofFloat(holder.scrollHint, "translationY", 0f, -15f, 0f);
                            bounce.setDuration(1500);
                            bounce.setRepeatCount(ValueAnimator.INFINITE);
                            bounce.setInterpolator(new AccelerateDecelerateInterpolator());
                            bounce.start();
                            holder.animators.add(bounce);

                            // FIX: Make the hint clickable to trigger actual scroll action
                            holder.scrollHint.setOnClickListener(v -> {
                                SharedPreferencesManager.getInstance(context).setBoolean(prefKey, true);
                                holder.scrollHint.setVisibility(View.GONE);
                                holder.scrollHint.clearAnimation();

                                View innerView = holder.scrollView.getChildAt(0);
                                if (innerView != null) {
                                    holder.scrollView.smoothScrollTo(0, innerView.getBottom());
                                }
                            });
                            
                            // Stop listening once we've measured and shown it
                            holder.scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        }
                    }
                };
                holder.scrollView.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
                
                // Ensure cleanup if view is detached before listener fires
                holder.scrollView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                    @Override
                    public void onViewAttachedToWindow(@NonNull View v) {}

                    @Override
                    public void onViewDetachedFromWindow(@NonNull View v) {
                        holder.scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
                        holder.scrollView.removeOnAttachStateChangeListener(this);
                    }
                });
            }

            holder.scrollView.setOnScrollChangeListener((View.OnScrollChangeListener) (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                // Hide hint as soon as user starts scrolling down significantly
                if (scrollY > 50) {
                    if (holder.scrollHint.getVisibility() == View.VISIBLE) {
                        SharedPreferencesManager.getInstance(context).setBoolean(prefKey, true);
                        holder.scrollHint.animate().alpha(0f).setDuration(300).withEndAction(() -> holder.scrollHint.setVisibility(View.GONE)).start();
                    }
                } else if (!holder.scrollView.canScrollVertically(1)) {
                    holder.scrollHint.setVisibility(View.GONE);
                }
            });
        }

        private List<ObjectAnimator> startTwinkleAnimation(View view, long delay) {
            List<ObjectAnimator> anims = new ArrayList<>();
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

            anims.add(alpha);
            anims.add(scaleX);
            anims.add(scaleY);
            return anims;
        }

        private ObjectAnimator startFloatingAnimation(View view, long delay, float distance, long duration) {
            ObjectAnimator animator = ObjectAnimator.ofFloat(view, "translationY", 0f, distance, 0f);
            animator.setDuration(duration);
            animator.setStartDelay(delay);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.start();
            return animator;
        }

        @Override
        public void onViewRecycled(@NonNull ViewHolder holder) {
            super.onViewRecycled(holder);
            holder.cancelAnimations();
        }

        @Override
        public int getItemCount() {
            return pages.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView mascot, acc1, acc2;
            ImageView s1, s2, s3, s4;
            TextView title, desc;
            androidx.core.widget.NestedScrollView scrollView;
            View scrollHint;
            final List<ObjectAnimator> animators = new ArrayList<>();

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
                scrollView = itemView.findViewById(R.id.onboarding_scroll_view);
                scrollHint = itemView.findViewById(R.id.scroll_hint);
            }

            void cancelAnimations() {
                for (ObjectAnimator animator : animators) {
                    animator.cancel();
                }
                animators.clear();
            }
        }
    }
}
