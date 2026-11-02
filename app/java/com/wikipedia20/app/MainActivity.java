package com.wikipedia20.app;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends Activity {

    private WebView web;
    // The app always loads the deployed site, so every web update lands in the app.
    // If that fails (offline, site down) it falls back to the copy bundled in assets.
    private static final String REMOTE = "https://technicalnihilist-lgtm.github.io/wikipedia-2.0/index.html";
    private static final String LOCAL = "file:///android_asset/index.html";
    private boolean pageReady = false;
    private boolean fellBack = false;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        web = new WebView(this);
        web.setBackgroundColor(0xFFF6F3EC);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setCacheMode(WebSettings.LOAD_DEFAULT);
        if (Build.VERSION.SDK_INT >= 21) {
            s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        }

        web.addJavascriptInterface(new Bridge(), "Bridge");

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onPermissionRequest(final android.webkit.PermissionRequest request) {
                request.deny();
            }
        });

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView v, String url) {
                if (url != null && url.startsWith("https://technicalnihilist-lgtm.github.io/")) {
                    return false;                       // stay inside the app
                }
                if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
                    try {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    } catch (Exception ignored) { }
                    return true;
                }
                return false;
            }

            @Override
            public void onPageFinished(WebView v, String url) {
                pageReady = true;
                String js = readAsset("inject.js");
                if (js != null) v.evaluateJavascript(js, null);
            }

            @Override
            public void onReceivedError(WebView v, int code, String desc, String failing) {
                if (!pageReady && !fellBack) fallback();
            }

            @Override
            public void onReceivedError(WebView v, android.webkit.WebResourceRequest req,
                                        android.webkit.WebResourceError err) {
                if (req.isForMainFrame() && !pageReady && !fellBack) fallback();
            }
        });

        // transparent status bar: the page draws behind it
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }

        web.loadUrl(REMOTE);
        new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
            public void run() {
                if (!pageReady && !fellBack) fallback();
            }
        }, 7000);
    }

    private void fallback() {
        if (fellBack) return;
        fellBack = true;
        web.loadUrl(LOCAL);
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : 0;
    }

    private String readAsset(String name) {
        try {
            InputStream in = getAssets().open(name);
            byte[] buf = new byte[in.available()];
            int n = in.read(buf);
            in.close();
            return new String(buf, 0, n < 0 ? 0 : n, "UTF-8");
        } catch (Exception e) {
            return null;
        }
    }

    public class Bridge {
        @JavascriptInterface
        public void save(final String name, final String dataUrl) {
            runOnUiThread(new Runnable() {
                public void run() {
                    try {
                        int comma = dataUrl.indexOf(',');
                        if (comma < 0) throw new Exception("bad data");
                        String mime = dataUrl.substring(5, Math.max(5, dataUrl.indexOf(';')));
                        byte[] bytes = Base64.decode(dataUrl.substring(comma + 1), Base64.DEFAULT);
                        String safe = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
                        if (safe.length() == 0) safe = "image.jpg";

                        if (Build.VERSION.SDK_INT >= 29) {
                            ContentValues cv = new ContentValues();
                            cv.put(MediaStore.Downloads.DISPLAY_NAME, safe);
                            cv.put(MediaStore.Downloads.MIME_TYPE, mime);
                            cv.put(MediaStore.Downloads.IS_PENDING, 1);
                            Uri uri = getContentResolver()
                                    .insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                            if (uri == null) throw new Exception("cannot create file");
                            OutputStream os = getContentResolver().openOutputStream(uri);
                            os.write(bytes);
                            os.close();
                            cv.clear();
                            cv.put(MediaStore.Downloads.IS_PENDING, 0);
                            getContentResolver().update(uri, cv, null, null);
                            toast("Saved to Downloads/" + safe);
                        } else {
                            File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
                            if (dir == null) dir = getFilesDir();
                            if (!dir.exists()) dir.mkdirs();
                            File f = new File(dir, safe);
                            FileOutputStream o = new FileOutputStream(f);
                            o.write(bytes);
                            o.close();
                            toast("Saved: " + f.getAbsolutePath());
                        }
                    } catch (Exception e) {
                        toast("Save failed: " + e.getMessage());
                    }
                }
            });
        }

        @JavascriptInterface
        public int sb() {
            return statusBarHeight();
        }

        @JavascriptInterface
        public void statusBar(final boolean dark) {
            if (Build.VERSION.SDK_INT < 23) return;
            runOnUiThread(new Runnable() {
                public void run() {
                    View dv = getWindow().getDecorView();
                    int flags = dv.getSystemUiVisibility();
                    if (dark) {
                        flags &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    } else {
                        flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                    }
                    dv.setSystemUiVisibility(flags);
                }
            });
        }

        @JavascriptInterface
        public void toastMsg(final String msg) {
            runOnUiThread(new Runnable() {
                public void run() { toast(msg); }
            });
        }
    }

    private void toast(String m) {
        Toast.makeText(this, m, Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_BACK && web != null && web.canGoBack()) {
            web.goBack();
            return true;
        }
        return super.onKeyDown(code, e);
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }
}
