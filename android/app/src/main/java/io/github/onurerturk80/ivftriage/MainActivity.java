package io.github.onurerturk80.ivftriage;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.webkit.WebViewAssetLoader;

/**
 * Hosts the bundled triage calculator. The page is the same file that is served on
 * the web — it is copied into the APK assets at build time — so the app and the
 * supplementary file can never drift apart.
 */
public class MainActivity extends ComponentActivity {

    private static final String ASSET_HOST = "appassets.androidplatform.net";

    /**
     * Serving the page through WebViewAssetLoader gives it an ordinary https origin,
     * which a file:// URL does not. The page then runs under the same rules it does
     * in a browser instead of under the WebView's file-scheme restrictions.
     */
    private static final String PAGE_URL = "https://" + ASSET_HOST + "/assets/index.html";

    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        EdgeToEdge.enable(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final View root = findViewById(R.id.root);
        webView = findViewById(R.id.webview);

        // Pad the page out from under the system bars, the cutout and the keyboard.
        // What we pad away shows the root's dark background, so the insets read as
        // chrome around the light page.
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
                            | WindowInsetsCompat.Type.displayCutout()
                            | WindowInsetsCompat.Type.ime());
            v.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });

        // Light glyphs on the dark chrome, matching the page's theme-color.
        WindowInsetsControllerCompat barAppearance =
                new WindowInsetsControllerCompat(getWindow(), root);
        barAppearance.setAppearanceLightStatusBars(false);
        barAppearance.setAppearanceLightNavigationBars(false);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setAllowFileAccess(false);
        settings.setAllowContentAccess(false);
        settings.setMediaPlaybackRequiresUserGesture(true);

        final WebViewAssetLoader assetLoader = new WebViewAssetLoader.Builder()
                .setDomain(ASSET_HOST)
                .addPathHandler("/assets/", new WebViewAssetLoader.AssetsPathHandler(this))
                .build();

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(
                    @NonNull WebView view, @NonNull WebResourceRequest request) {
                return assetLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            public boolean shouldOverrideUrlLoading(
                    @NonNull WebView view, @NonNull WebResourceRequest request) {
                Uri url = request.getUrl();
                if (ASSET_HOST.equals(url.getHost())) {
                    return false;
                }
                // The page carries no links today, but a DOI added to it later must
                // not strand the reader inside an app that holds no INTERNET permission.
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, url)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                } catch (ActivityNotFoundException noBrowser) {
                    // Nothing can open it; stay on the calculator rather than crash.
                }
                return true;
            }
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (webView.canGoBack()) {
                    webView.goBack();
                    return;
                }
                setEnabled(false);
                getOnBackPressedDispatcher().onBackPressed();
            }
        });

        if (savedInstanceState == null) {
            webView.loadUrl(PAGE_URL);
        } else {
            webView.restoreState(savedInstanceState);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        webView.saveState(outState);
    }
}
