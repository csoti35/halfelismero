package hu.csoti35.akvarisztika;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.webkit.GeolocationPermissions;
import android.webkit.PermissionRequest;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Az Akvarisztika (Lovable) webalkalmazás Android-kerete: kamera, galéria, helymeghatározás. */
public class MainActivity extends Activity {
    private static final int REQ_FILE = 10;
    private static final int REQ_CAMERA_PERM = 11;
    private static final int REQ_LOCATION_PERM = 12;

    /** Elrejti az „Edit with Lovable” jelvényt (most és ha később újra megjelenne). */
    private static final String HIDE_BADGE_JS =
        "(function(){" +
        "var css='#lovable-badge,a[href*=\\'lovable.dev\\'],a[href*=\\'lovable.app/?utm\\'],[id*=lovable-badge]{display:none!important;visibility:hidden!important}';" +
        "if(!document.getElementById('hide-lovable')){var st=document.createElement('style');st.id='hide-lovable';st.textContent=css;(document.head||document.documentElement).appendChild(st);}" +
        "var pend=false;function sweep(){pend=false;document.querySelectorAll('a').forEach(function(el){var t=(el.textContent||'').trim();" +
        "if(/Edit with\\s*Lovable/i.test(t)&&t.length<60){var box=(el.parentElement&&el.parentElement.children.length<=3&&getComputedStyle(el.parentElement).position==='fixed')?el.parentElement:el;box.style.setProperty('display','none','important');}});}" +
        "sweep();new MutationObserver(function(){if(!pend){pend=true;setTimeout(sweep,300);}}).observe(document.documentElement,{childList:true,subtree:true});" +
        "})();";

    private WebView web;
    private String homeHost;
    private ValueCallback<Uri[]> fileCallback;
    private WebChromeClient.FileChooserParams pendingParams;
    private Uri cameraUri;
    private GeolocationPermissions.Callback geoCallback;
    private String geoOrigin;
    private PermissionRequest pendingMediaRequest;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(Color.parseColor("#061a1f"));
        getWindow().setNavigationBarColor(Color.parseColor("#061a1f"));

        String url = getString(R.string.app_url);
        homeHost = Uri.parse(url).getHost();

        web = new WebView(this);
        web.setBackgroundColor(Color.parseColor("#061a1f"));
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setGeolocationEnabled(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setAllowFileAccess(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);

        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                Uri u = request.getUrl();
                String host = u.getHost();
                String scheme = u.getScheme();
                if (("https".equals(scheme) || "http".equals(scheme)) && host != null && homeHost != null
                        && (host.equals(homeHost) || host.endsWith(".lovable.app") || host.endsWith(".supabase.co"))) {
                    return false; // az alkalmazáson belül marad
                }
                openExternal(u); // térkép, keresés, telefonszám stb. külső alkalmazásban
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String pageUrl) {
                super.onPageFinished(view, pageUrl);
                view.evaluateJavascript(HIDE_BADGE_JS, null);
            }
        });

        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                pendingParams = params;
                boolean wantsCamera = params.isCaptureEnabled();
                if (wantsCamera && checkSelfPermissionCompat(Manifest.permission.CAMERA)) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERM);
                    return true;
                }
                launchPicker(params);
                return true;
            }

            @Override
            public void onGeolocationPermissionsShowPrompt(String origin, GeolocationPermissions.Callback callback) {
                if (!checkSelfPermissionCompat(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    callback.invoke(origin, true, false);
                    return;
                }
                geoOrigin = origin;
                geoCallback = callback;
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQ_LOCATION_PERM);
            }

            @Override
            public void onPermissionRequest(PermissionRequest request) {
                runOnUiThread(() -> {
                    if (checkSelfPermissionCompat(Manifest.permission.CAMERA)) {
                        pendingMediaRequest = request;
                        requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAMERA_PERM);
                    } else {
                        request.grant(request.getResources());
                    }
                });
            }
        });

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl(url);
    }

    /** true, ha az engedély még NINCS megadva. */
    private boolean checkSelfPermissionCompat(String perm) {
        return Build.VERSION.SDK_INT >= 23 && checkSelfPermission(perm) != PackageManager.PERMISSION_GRANTED;
    }

    private void launchPicker(WebChromeClient.FileChooserParams params) {
        Intent camera = null;
        try {
            if (!checkSelfPermissionCompat(Manifest.permission.CAMERA)) {
                File dir = new File(getCacheDir(), "photos");
                if (!dir.exists()) dir.mkdirs();
                File photo = new File(dir, "foto_" + System.currentTimeMillis() + ".jpg");
                cameraUri = FileProvider.getUriForFile(this, getPackageName() + ".files", photo);
                camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                camera.putExtra(MediaStore.EXTRA_OUTPUT, cameraUri);
                camera.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
        } catch (Exception e) {
            camera = null;
            cameraUri = null;
        }

        Intent toStart;
        if (params != null && params.isCaptureEnabled() && camera != null) {
            toStart = camera; // „Fotó” gomb: rögtön a kamera
        } else {
            Intent gallery = new Intent(Intent.ACTION_GET_CONTENT);
            gallery.addCategory(Intent.CATEGORY_OPENABLE);
            gallery.setType("image/*");
            toStart = Intent.createChooser(gallery, "Kép kiválasztása");
            if (camera != null) toStart.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{camera});
        }
        try {
            startActivityForResult(toStart, REQ_FILE);
        } catch (ActivityNotFoundException e) {
            if (fileCallback != null) fileCallback.onReceiveValue(null);
            fileCallback = null;
            Toast.makeText(this, "Nem található kamera vagy galéria alkalmazás.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_FILE || fileCallback == null) return;
        Uri[] result = null;
        if (resultCode == RESULT_OK) {
            if (data != null && data.getData() != null) {
                result = new Uri[]{data.getData()};
            } else if (data != null && data.getClipData() != null) {
                List<Uri> list = new ArrayList<>();
                for (int i = 0; i < data.getClipData().getItemCount(); i++) list.add(data.getClipData().getItemAt(i).getUri());
                result = list.toArray(new Uri[0]);
            } else if (cameraUri != null) {
                result = new Uri[]{cameraUri};
            }
        }
        fileCallback.onReceiveValue(result);
        fileCallback = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean granted = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
        if (requestCode == REQ_CAMERA_PERM) {
            if (pendingMediaRequest != null) {
                if (granted) pendingMediaRequest.grant(pendingMediaRequest.getResources());
                else pendingMediaRequest.deny();
                pendingMediaRequest = null;
            }
            if (fileCallback != null) launchPicker(pendingParams); // engedély nélkül a galéria nyílik
        } else if (requestCode == REQ_LOCATION_PERM && geoCallback != null) {
            geoCallback.invoke(geoOrigin, granted, false);
            geoCallback = null;
        }
    }

    private void openExternal(Uri u) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, u));
        } catch (Exception e) {
            Toast.makeText(this, "Nem sikerült megnyitni: " + u, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBackPressed() {
        if (web != null && web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (web != null) web.saveState(outState);
    }
}
