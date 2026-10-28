package com.lyricsstudio.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends Activity {

    private static final String ASSET_HOST = "appassets.androidplatform.net";
    private static final int REQ_FILE = 1001;

    private WebView web;
    private ValueCallback<Uri[]> fileCallback;

    private static String mimeOf(String path) {
        String p = path.toLowerCase();
        if (p.endsWith(".html")) return "text/html";
        if (p.endsWith(".js")) return "application/javascript";
        if (p.endsWith(".wasm")) return "application/wasm";
        if (p.endsWith(".css")) return "text/css";
        if (p.endsWith(".json")) return "application/json";
        return "application/octet-stream";
    }

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);

        Window w = getWindow();
        w.setStatusBarColor(0xFFDFE9FF);
        w.setNavigationBarColor(0xFFFFE3EF);
        if (Build.VERSION.SDK_INT >= 23) {
            w.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        web = new WebView(this);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setAllowFileAccess(true);
        s.setAllowContentAccess(true);
        s.setSupportZoom(false);
        s.setBuiltInZoomControls(false);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(false);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setJavaScriptCanOpenWindowsAutomatically(false);

        web.setBackgroundColor(0xFFDFE9FF);
        web.setOverScrollMode(View.OVER_SCROLL_NEVER);
        web.addJavascriptInterface(new Bridge(), "AndroidBridge");

        // lets <input type="file"> open the system picker (for importing a .gguf model)
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback,
                                             FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.addCategory(Intent.CATEGORY_OPENABLE);
                i.setType("*/*");
                try {
                    startActivityForResult(Intent.createChooser(i, "Select a GGUF model"), REQ_FILE);
                    return true;
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;
                    return false;
                }
            }
        });

        // serve the bundled files over https so fetch()/wasm loading works inside the WebView
        web.setWebViewClient(new WebViewClient() {
            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                if (u != null && ASSET_HOST.equals(u.getHost())) {
                    String path = u.getPath();
                    if (path == null) path = "/";
                    String asset = path.startsWith("/") ? path.substring(1) : path;
                    try {
                        InputStream is = getAssets().open(asset);
                        Map<String, String> h = new HashMap<>();
                        h.put("Access-Control-Allow-Origin", "*");
                        h.put("Cross-Origin-Opener-Policy", "same-origin");
                        h.put("Cross-Origin-Embedder-Policy", "require-corp");
                        h.put("Cross-Origin-Resource-Policy", "cross-origin");
                        h.put("Cache-Control", "no-store");
                        return new WebResourceResponse(mimeOf(asset), null, 200, "OK", h, is);
                    } catch (Exception e) {
                        return new WebResourceResponse("text/plain", "utf-8", 404, "Not Found", null, null);
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        setContentView(web);
        web.loadUrl("https://" + ASSET_HOST + "/assets/index.html");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE) {
            if (fileCallback == null) return;
            Uri[] result = null;
            if (resultCode == RESULT_OK && data != null && data.getData() != null) {
                result = new Uri[]{ data.getData() };
            }
            fileCallback.onReceiveValue(result);
            fileCallback = null;
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onKeyDown(int code, KeyEvent e) {
        if (code == KeyEvent.KEYCODE_BACK && web != null && web.canGoBack()) {
            web.goBack();
            return true;
        }
        return super.onKeyDown(code, e);
    }

    private void toast(final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                Toast.makeText(MainActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String safeName(String name) {
        if (name == null || name.trim().isEmpty()) return "lyrics.txt";
        String n = name.replaceAll("[\\\\/:*?\"<>|]+", "_").trim();
        if (!n.toLowerCase().endsWith(".txt")) n = n + ".txt";
        return n.length() > 80 ? n.substring(0, 76) + ".txt" : n;
    }

    public class Bridge {

        @JavascriptInterface
        public void saveText(String name, String text) {
            String file = safeName(name);
            try {
                byte[] bytes = (text == null ? "" : text).getBytes("UTF-8");
                if (Build.VERSION.SDK_INT >= 29) {
                    ContentValues v = new ContentValues();
                    v.put(MediaStore.Downloads.DISPLAY_NAME, file);
                    v.put(MediaStore.Downloads.MIME_TYPE, "text/plain");
                    v.put(MediaStore.Downloads.RELATIVE_PATH,
                            Environment.DIRECTORY_DOWNLOADS + "/Lyrics Studio");
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                    if (uri == null) throw new Exception("Downloads unavailable");
                    OutputStream os = getContentResolver().openOutputStream(uri);
                    os.write(bytes);
                    os.close();
                    toast("Saved to Downloads/Lyrics Studio/" + file);
                } else {
                    File dir = new File(getExternalFilesDir(null), "Lyrics Studio");
                    if (!dir.exists()) dir.mkdirs();
                    File f = new File(dir, file);
                    FileOutputStream fo = new FileOutputStream(f);
                    fo.write(bytes);
                    fo.close();
                    toast("Saved to " + f.getAbsolutePath());
                }
            } catch (Exception e) {
                toast("Could not save: " + e.getMessage());
            }
        }

        @JavascriptInterface
        public void shareText(String text) {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_SUBJECT, "Lyrics Studio");
            i.putExtra(Intent.EXTRA_TEXT, text == null ? "" : text);
            startActivity(Intent.createChooser(i, "Share lyrics"));
        }

        @JavascriptInterface
        public void copyText(String text) {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null) {
                cm.setPrimaryClip(ClipData.newPlainText("lyrics", text == null ? "" : text));
                toast("Copied");
            }
        }
    }
}
