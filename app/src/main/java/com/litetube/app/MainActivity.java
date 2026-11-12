package com.litetube.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.ByteArrayInputStream;
import java.util.Locale;

public final class MainActivity extends Activity {
    private static final String HOME = "https://m.youtube.com/";
    private static final String PREFS = "litetube_settings";
    private static final String FILTER_KEY = "filter_enabled";
    private static final String AUTO_SKIP_KEY = "auto_skip_enabled";
    private static final String[] BLOCKED_HOSTS = {
        "doubleclick.net", "googleadservices.com", "googlesyndication.com",
        "adservice.google.com", "adservice.google.co.in",
        "google-analytics.com", "googletagmanager.com"
    };
    private volatile boolean filterEnabled;
    private boolean autoSkipEnabled;
    private TextView skipButton;
    private WebView webView;
    private FrameLayout root;
    private LinearLayout toolbar;
    private TextView filterButton;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private int previousOrientation;
    private final Handler skipHandler = new Handler(Looper.getMainLooper());
    private boolean active = false;
    private final Runnable skipCheck = new Runnable() {
        @Override public void run() {
            if (!active || webView == null) return;
            String url = webView.getUrl();
            if (autoSkipEnabled && url != null && url.startsWith("https://m.youtube.com/")) {
                // Only click YouTube's already-enabled, visible Skip Ad button.
                // Never seek in the video, hide an ad overlay, or bypass the countdown.
                webView.evaluateJavascript(SKIP_SCRIPT, null);
            }
            skipHandler.postDelayed(this, 1500);
        }
    };
    private static final String SKIP_SCRIPT =
        "(function(){try{" +
        "var ad=document.querySelector('.ad-showing,.ytp-ad-player-overlay,.ytp-ad-preview-container');" +
        "if(!ad)return false;" +
        "var selectors=['.ytp-ad-skip-button','.ytp-ad-skip-button-modern'," +
        "'.ytp-skip-ad-button','.ytp-ad-skip-button-container button'," +
        "'button[aria-label=\\\"Skip Ad\\\"]','button[aria-label=\\\"Skip Ads\\\"]'];" +
        "var nodes=document.querySelectorAll(selectors.join(','));" +
        "for(var i=0;i<nodes.length;i++){var b=nodes[i];" +
        "var r=b.getBoundingClientRect(),st=getComputedStyle(b);" +
        "if(!b.disabled&&b.getAttribute('aria-disabled')!=='true'&&" +
        "r.width>0&&r.height>0&&st.visibility!=='hidden'&&st.display!=='none'){" +
        "b.click();return true;}}return false;" +
        "}catch(e){return false;}})();";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        filterEnabled = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(FILTER_KEY, true);
        autoSkipEnabled = getSharedPreferences(PREFS, MODE_PRIVATE).getBoolean(AUTO_SKIP_KEY, true);
        getWindow().setStatusBarColor(Color.rgb(16, 16, 16));
        getWindow().setNavigationBarColor(Color.rgb(16, 16, 16));

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setBackgroundColor(Color.rgb(24, 24, 24));
        addButton("⌂", "Home", () -> webView.loadUrl(HOME));
        addButton("‹", "Back", () -> { if (webView.canGoBack()) webView.goBack(); });
        addButton("↻", "Reload", () -> webView.reload());
        TextView title = new TextView(this);
        title.setText("LiteTube");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(dp(8), 0, 0, 0);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        filterButton = addButton("", "Toggle ad filter", () -> {
            filterEnabled = !filterEnabled;
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(FILTER_KEY, filterEnabled).apply();
            updateFilterButton();
            webView.reload();
        });
        filterButton.setTextSize(12);
        updateFilterButton();
        skipButton = addButton("", "Toggle automatic Skip Ad", () -> {
            autoSkipEnabled = !autoSkipEnabled;
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putBoolean(AUTO_SKIP_KEY, autoSkipEnabled).apply();
            updateSkipButton();
        });
        skipButton.setTextSize(11);
        updateSkipButton();
        content.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(48)));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(true);
        s.setSupportMultipleWindows(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        s.setLoadsImagesAutomatically(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (!filterEnabled || request.isForMainFrame()) return null;
                Uri uri = request.getUrl();
                if (uri == null || !"https".equalsIgnoreCase(uri.getScheme())) return null;
                if (isBlockedHost(uri.getHost())) {
                    return new WebResourceResponse("text/plain", "UTF-8", new ByteArrayInputStream(new byte[0]));
                }
                return null;
            }
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (!request.isForMainFrame()) return false;
                Uri uri = request.getUrl();
                String scheme = uri.getScheme();
                String host = uri.getHost();
                if ("https".equalsIgnoreCase(scheme) && isYouTubeHost(host)) return false;
                if ("http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme)) {
                    openExternal(new Intent(Intent.ACTION_VIEW, uri));
                    return true;
                }
                return true;
            }
        });
        webView.setWebChromeClient(new WebChromeClient() {
            @Override public void onShowCustomView(View view, CustomViewCallback callback) {
                if (fullscreenView != null) { callback.onCustomViewHidden(); return; }
                fullscreenView = view;
                fullscreenCallback = callback;
                previousOrientation = getRequestedOrientation();
                toolbar.setVisibility(View.GONE);
                webView.setVisibility(View.GONE);
                root.addView(view, new FrameLayout.LayoutParams(-1, -1));
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            }
            @Override public void onHideCustomView() { exitFullscreen(); }
        });
        content.addView(webView, new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));
        setContentView(root);
        if (state != null) webView.restoreState(state);
        else webView.loadUrl(HOME);
    }

    private static boolean isBlockedHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(Locale.ROOT);
        for (String domain : BLOCKED_HOSTS) {
            if (h.equals(domain) || h.endsWith("." + domain)) return true;
        }
        return false;
    }
    private static boolean isYouTubeHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(Locale.ROOT);
        return h.equals("youtube.com") || h.endsWith(".youtube.com")
                || h.equals("youtu.be") || h.equals("youtube-nocookie.com")
                || h.endsWith(".youtube-nocookie.com");
    }
    private void updateFilterButton() {
        filterButton.setText(filterEnabled ? "Ads\nON" : "Ads\nOFF");
        filterButton.setTextColor(filterEnabled ? Color.rgb(110, 220, 145) : Color.LTGRAY);
        filterButton.setContentDescription(filterEnabled ? "Ad filter enabled; tap to disable" : "Ad filter disabled; tap to enable");
    }
    private void updateSkipButton() {
        skipButton.setText(autoSkipEnabled ? "Skip\\nON" : "Skip\\nOFF");
        skipButton.setTextColor(autoSkipEnabled ? Color.rgb(110, 220, 145) : Color.LTGRAY);
        skipButton.setContentDescription(autoSkipEnabled ? "Auto Skip enabled; tap to disable" : "Auto Skip disabled; tap to enable");
    }
    private void openExternal(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException ignored) { }
    }
    private TextView addButton(String symbol, String description, Runnable action) {
        TextView button = new TextView(this);
        button.setText(symbol);
        button.setTextSize(26);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(description);
        button.setOnClickListener(v -> action.run());
        toolbar.addView(button, new LinearLayout.LayoutParams(dp(52), dp(48)));
        return button;
    }
    private int dp(int size) { return Math.round(size * getResources().getDisplayMetrics().density); }
    private void exitFullscreen() {
        if (fullscreenView == null) return;
        root.removeView(fullscreenView);
        fullscreenView = null;
        toolbar.setVisibility(View.VISIBLE);
        webView.setVisibility(View.VISIBLE);
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(previousOrientation);
        WebChromeClient.CustomViewCallback callback = fullscreenCallback;
        fullscreenCallback = null;
        if (callback != null) callback.onCustomViewHidden();
    }
    @Override public void onBackPressed() {
        if (fullscreenView != null) exitFullscreen();
        else if (webView != null && webView.canGoBack()) webView.goBack();
        else super.onBackPressed();
    }
    @Override protected void onPause() {
        active = false;
        skipHandler.removeCallbacks(skipCheck);
        if (webView != null) webView.onPause();
        super.onPause();
    }
    @Override protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
        active = true;
        skipHandler.removeCallbacks(skipCheck);
        skipHandler.postDelayed(skipCheck, 1500);
    }
    @Override protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }
    @Override protected void onDestroy() {
        active = false;
        skipHandler.removeCallbacks(skipCheck);
        if (fullscreenView != null) exitFullscreen();
        if (webView != null) {
            ViewGroup parent = (ViewGroup) webView.getParent();
            if (parent != null) parent.removeView(webView);
            webView.destroy();
            webView = null;
        }
        super.onDestroy();
    }
}
