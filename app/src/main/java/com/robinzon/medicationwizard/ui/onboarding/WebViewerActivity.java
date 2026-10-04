package com.robinzon.medicationwizard.ui.onboarding;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.appcompat.app.AppCompatActivity;

import com.robinzon.medicationwizard.databinding.ActivityWebViewerBinding;

/**
 * Activity for displaying web content such as terms of service or privacy policies.
 * Uses a WebView to render the content and provides a standard toolbar with a title and back button.
 */
public class WebViewerActivity extends AppCompatActivity {

    public static final String EXTRA_URL = "extra_url";
    public static final String EXTRA_TITLE = "extra_title";

    /**
     * Initializes the activity, sets up the UI components, and loads the provided URL.
     * Extracts the URL and title from the intent extras and configures the WebView settings.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down
     *                           then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     *                           Otherwise it is null.
     */
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActivityWebViewerBinding binding = ActivityWebViewerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        String url = getIntent().getStringExtra(EXTRA_URL);
        String title = getIntent().getStringExtra(EXTRA_TITLE);

        binding.toolbar.setTitle(title != null ? title : "");
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        WebSettings settings = binding.webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);

        binding.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                binding.progressIndicator.setVisibility(View.GONE);
            }
        });

        binding.webView.setWebChromeClient(new WebChromeClient());

        if (url != null) {
            binding.webView.loadUrl(url);
        }
    }
}
