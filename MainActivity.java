package ir.jetbox.app;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.webkit.WebViewAssetLoader;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {

    private static final int LOCATION_REQ = 1001;
    private static final int FILE_CHOOSER_REQ = 2001;
    private static final int BACKUP_CREATE_REQ = 3001;
    private static final int RESTORE_OPEN_REQ = 3002;

    private WebView webView;
    private ValueCallback<Uri[]> filePathCallback;
    private String pendingBackupJson;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRequestedOrientation(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        );

        webView = new WebView(this);
        setContentView(webView);

        enterImmersiveLandscape();

        WebSettings s = webView.getSettings();

        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setGeolocationEnabled(true);
        s.setAllowFileAccess(false);
        s.setAllowContentAccess(false);
        s.setBuiltInZoomControls(false);
        s.setSupportZoom(false);

        webView.addJavascriptInterface(
                new AndroidBridge(),
                "JetBoxAndroid"
        );

        WebViewAssetLoader loader =
                new WebViewAssetLoader.Builder()
                        .addPathHandler(
                                "/assets/",
                                new WebViewAssetLoader
                                        .AssetsPathHandler(this)
                        )
                        .build();

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view,
                    WebResourceRequest request
            ) {
                return loader.shouldInterceptRequest(
                        request.getUrl()
                );
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(
                    WebView view,
                    String url
            ) {
                return loader.shouldInterceptRequest(
                        Uri.parse(url)
                );
            }

            @Override
            public void onPageFinished(
                    WebView view,
                    String url
            ) {
                super.onPageFinished(view, url);

                view.evaluateJavascript(
                        "(function(){try{" +

                        "window.close=function(){" +
                        "if(window.JetBoxAndroid)" +
                        "JetBoxAndroid.exitApp();" +
                        "};" +

                        "var b=document.createElement('button');" +

                        "b.textContent='پشتیبان جت‌باکس';" +

                        "b.style.cssText=" +
                        "'position:fixed;" +
                        "top:8px;" +
                        "right:8px;" +
                        "z-index:2147483647;" +
                        "padding:9px 12px;" +
                        "border:0;" +
                        "border-radius:8px;" +
                        "background:#111;" +
                        "color:#fff;" +
                        "font-size:13px;" +
                        "opacity:.88;';" +

                        "b.onclick=function(){" +
                        "if(window.JetBoxAndroid)" +
                        "JetBoxAndroid.backupLocalData(" +
                        "JSON.stringify(localStorage));" +
                        "};" +

                        "document.body.appendChild(b);" +

                        "var r=document.createElement('button');" +

                        "r.textContent='بازیابی';" +

                        "r.style.cssText=" +
                        "'position:fixed;" +
                        "top:8px;" +
                        "right:145px;" +
                        "z-index:2147483647;" +
                        "padding:9px 12px;" +
                        "border:0;" +
                        "border-radius:8px;" +
                        "background:#444;" +
                        "color:#fff;" +
                        "font-size:13px;" +
                        "opacity:.88;';" +

                        "r.onclick=function(){" +
                        "if(window.JetBoxAndroid)" +
                        "JetBoxAndroid.restoreLocalData();" +
                        "};" +

                        "document.body.appendChild(r);" +

                        "}catch(e){}})();",
                        null
                );
            }
        });

        webView.setWebChromeClient(
                new WebChromeClient() {

                    @Override
                    public void onGeolocationPermissionsShowPrompt(
                            String origin,
                            GeolocationPermissions.Callback callback
                    ) {

                        if (
                                ContextCompat.checkSelfPermission(
                                        MainActivity.this,
                                        Manifest.permission
                                                .ACCESS_FINE_LOCATION
                                ) == PackageManager
                                        .PERMISSION_GRANTED
                                ||
                                ContextCompat.checkSelfPermission(
                                        MainActivity.this,
                                        Manifest.permission
                                                .ACCESS_COARSE_LOCATION
                                ) == PackageManager
                                        .PERMISSION_GRANTED
                        ) {

                            callback.invoke(
                                    origin,
                                    true,
                                    false
                            );

                        } else {

                            requestLocation();

                            callback.invoke(
                                    origin,
                                    false,
                                    false
                            );
                        }
                    }

                    @Override
                    public boolean onShowFileChooser(
                            WebView webView,
                            ValueCallback<Uri[]> filePathCallback,
                            FileChooserParams fileChooserParams
                    ) {

                        if (
                                MainActivity.this.filePathCallback
                                        != null
                        ) {
                            MainActivity.this
                                    .filePathCallback
                                    .onReceiveValue(null);
                        }

                        MainActivity.this.filePathCallback =
                                filePathCallback;

                        Intent intent;

                        try {

                            intent =
                                    fileChooserParams.createIntent();

                        } catch (Exception e) {

                            intent =
                                    new Intent(
                                            Intent.ACTION_OPEN_DOCUMENT
                                    );

                            intent.addCategory(
                                    Intent.CATEGORY_OPENABLE
                            );

                            intent.setType("*/*");

                            intent.putExtra(
                                    Intent.EXTRA_ALLOW_MULTIPLE,
                                    true
                            );
                        }

                        intent.addCategory(
                                Intent.CATEGORY_OPENABLE
                        );

                        try {

                            startActivityForResult(
                                    intent,
                                    FILE_CHOOSER_REQ
                            );

                        } catch (Exception e) {

                            MainActivity.this.filePathCallback =
                                    null;

                            return false;
                        }

                        return true;
                    }
                }
        );

        requestLocation();

        webView.loadUrl(
                "https://appassets.androidplatform.net/assets/index.html"
        );
    }

    private void enterImmersiveLandscape() {

        View decorView =
                getWindow().getDecorView();

        decorView.setSystemUiVisibility(

                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

                        | View.SYSTEM_UI_FLAG_FULLSCREEN

                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION

                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    private void requestLocation() {

        if (
                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED

                &&

                ContextCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(

                    this,

                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },

                    LOCATION_REQ
            );
        }
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (requestCode == FILE_CHOOSER_REQ) {

            if (filePathCallback == null) {
                return;
            }

            Uri[] results = null;

            if (
                    resultCode == RESULT_OK
                            && data != null
            ) {

                if (data.getClipData() != null) {

                    int count =
                            data.getClipData()
                                    .getItemCount();

                    results =
                            new Uri[count];

                    for (
                            int i = 0;
                            i < count;
                            i++
                    ) {

                        results[i] =
                                data.getClipData()
                                        .getItemAt(i)
                                        .getUri();
                    }

                } else if (
                        data.getData() != null
                ) {

                    results =
                            new Uri[]{
                                    data.getData()
                            };
                }
            }

            filePathCallback
                    .onReceiveValue(results);

            filePathCallback = null;

            return;
        }

        if (requestCode == BACKUP_CREATE_REQ) {

            if (
                    resultCode == RESULT_OK
                            && data != null
                            && data.getData() != null
                            && pendingBackupJson != null
            ) {

                try {

                    OutputStream out =
                            getContentResolver()
                                    .openOutputStream(
                                            data.getData()
                                    );

                    if (out != null) {

                        out.write(
                                pendingBackupJson
                                        .getBytes(
                                                StandardCharsets.UTF_8
                                        )
                        );

                        out.close();

                        Toast.makeText(
                                this,
                                "پشتیبان جت‌باکس ذخیره شد",
                                Toast.LENGTH_LONG
                        ).show();
                    }

                } catch (Exception e) {

                    Toast.makeText(
                            this,
                            "ذخیره پشتیبان ناموفق بود",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }

            pendingBackupJson = null;

            return;
        }

        if (requestCode == RESTORE_OPEN_REQ) {

            if (
                    resultCode == RESULT_OK
                            && data != null
                            && data.getData() != null
            ) {

                try {

                    InputStream in =
                            getContentResolver()
                                    .openInputStream(
                                            data.getData()
                                    );

                    ByteArrayOutputStream buffer =
                            new ByteArrayOutputStream();

                    byte[] temp =
                            new byte[8192];

                    int n;

                    while (
                            in != null
                                    && (n = in.read(temp)) != -1
                    ) {

                        buffer.write(
                                temp,
                                0,
                                n
                        );
                    }

                    if (in != null) {
                        in.close();
                    }

                    String json =
                            buffer.toString(
                                    StandardCharsets.UTF_8.name()
                            );

                    String encoded =
                            Base64.encodeToString(
                                    json.getBytes(
                                            StandardCharsets.UTF_8
                                    ),
                                    Base64.NO_WRAP
                            );

                    String script =

                            "(function(){try{" +

                            "var s=atob('" +
                            encoded +
                            "');" +

                            "var bytes=" +
                            "new Uint8Array(s.length);" +

                            "for(var i=0;i<s.length;i++)" +
                            "bytes[i]=s.charCodeAt(i);" +

                            "var json=" +
                            "new TextDecoder('utf-8')" +
                            ".decode(bytes);" +

                            "var data=JSON.parse(json);" +

                            "localStorage.clear();" +

                            "Object.keys(data).forEach(" +
                            "function(k){" +
                            "localStorage.setItem(k,data[k]);" +
                            "});" +

                            "location.reload();" +

                            "}catch(e){" +

                            "alert(" +
                            "'بازیابی پشتیبان ناموفق بود'" +
                            ");" +

                            "}})();";

                    webView.evaluateJavascript(
                            script,
                            null
                    );

                    Toast.makeText(
                            this,
                            "بازیابی اطلاعات انجام شد",
                            Toast.LENGTH_LONG
                    ).show();

                } catch (Exception e) {

                    Toast.makeText(
                            this,
                            "خواندن پشتیبان ناموفق بود",
                            Toast.LENGTH_LONG
                    ).show();
                }
            }
        }
    }

    @Override
    public void onWindowFocusChanged(
            boolean hasFocus
    ) {

        super.onWindowFocusChanged(
                hasFocus
        );

        if (hasFocus) {
            enterImmersiveLandscape();
        }
    }

    @Override
    public void onBackPressed() {

        if (
                webView != null
                        && webView.canGoBack()
        ) {

            webView.goBack();

        } else {

            super.onBackPressed();
        }
    }

    private final class AndroidBridge {

        @JavascriptInterface
        public void exitApp() {

            runOnUiThread(
                    () -> finishAffinity()
            );
        }

        @JavascriptInterface
        public void backupLocalData(
                String json
        ) {

            pendingBackupJson = json;

            Intent intent =
                    new Intent(
                            Intent.ACTION_CREATE_DOCUMENT
                    );

            intent.addCategory(
                    Intent.CATEGORY_OPENABLE
            );

            intent.setType(
                    "application/json"
            );

            intent.putExtra(
                    Intent.EXTRA_TITLE,
                    "JetBox-backup.json"
            );

            try {

                startActivityForResult(
                        intent,
                        BACKUP_CREATE_REQ
                );

            } catch (Exception e) {

                Toast.makeText(
                        MainActivity.this,
                        "امکان ساخت فایل پشتیبان نیست",
                        Toast.LENGTH_LONG
                ).show();
            }
        }

        @JavascriptInterface
        public void restoreLocalData() {

            Intent intent =
                    new Intent(
                            Intent.ACTION_OPEN_DOCUMENT
                    );

            intent.addCategory(
                    Intent.CATEGORY_OPENABLE
            );

            intent.setType(
                    "application/json"
            );

            try {

                startActivityForResult(
                        intent,
                        RESTORE_OPEN_REQ
                );

            } catch (Exception e) {

                Toast.makeText(
                        MainActivity.this,
                        "امکان انتخاب فایل پشتیبان نیست",
                        Toast.LENGTH_LONG
                ).show();
            }
        }
    }
}
