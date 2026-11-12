package com.litetube.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public final class MainActivity extends Activity {
    private static final String HOME = "https://m.youtube.com/";
    private WebView webView;
    private FrameLayout root;
    private LinearLayout toolbar;
    private View fullscreenView;
    private WebChromeClient.CustomViewCallback fullscreenCallback;
    private int previousOrientation;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(16, 16, 16));
        getWindow().setNavigationBarColor(Color.rgb(16, 16, 16));

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        toolbar = new LinearLayout(this);
        toolbar.setGravity(Gravity.CENTER_VERTICAL);
        toolbar.setBackgroundColor(Color.rgb(24, 24, 24));
        addButton("⌂", () -> webView.loadUrl(HOME));
        addButton("‹", () -> { if (webView.canGoBack()) webView.goBack(); });
        addButton("↻", () -> webView.reload());
        TextView title = new TextView(this);
        title.setText("LiteTube");
        title.setTextColor(Color.WHITE);
        title.setTextSize(17);
        title.setGravity(Gravity.CENTER_VERTICAL);
        title.setPadding(14, 0, 0, 0);
        toolbar.addView(title, new LinearLayout.LayoutParams(0, dp(48), 1));
        content.addView(toolbar, new LinearLayout.LayoutParams(-1, dp(48)));

        webView = new WebView(this);
        webView.setBackgroundColor(Color.BLACK);
        WebSettings s = webView.getSettings();
        s.setJavaScriptEnabled(true); // YouTube needs JavaScript.
        s.setDomStorageEnabled(true); // YouTube preferences and sign-in.
        s.setMediaPlaybackRequiresUserGesture(true); // Prevent automatic playback.
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
                // Do not auto-launch custom URI schemes from untrusted web content.
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

    private static boolean isYouTubeHost(String host) {
        if (host == null) return false;
        String h = host.toLowerCase(java.util.Locale.ROOT);
        return h.equals("youtube.com") || h.endsWith(".youtube.com")
                || h.equals("youtu.be") || h.equals("youtube-nocookie.com")
                || h.endsWith(".youtube-nocookie.com");
    }

    private void openExternal(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException ignored) { }
    }

    private void addButton(String symbol, Runnable action) {
        TextView button = new TextView(this);
        button.setText(symbol);
        button.setTextSize(26);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.CENTER);
        button.setContentDescription(symbol.equals("⌂") ? "Home" : symbol.equals("‹") ? "Back" : "Reload");
        button.setOnClickListener(v -> action.run());
        toolbar.addView(button, new LinearLayout.LayoutParams(dp(52), dp(48)));
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
        if (webView != null) webView.onPause();
        super.onPause();
    }

    @Override protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override protected void onSaveInstanceState(Bundle outState) {
        if (webView != null) webView.saveState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override protected void onDestroy() {
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
