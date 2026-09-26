package com.watzem.tv;

import android.app.PictureInPictureParams;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Rational;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.webkit.JsPromptResult;
import android.webkit.JsResult;
import android.webkit.WebSettings;
import android.webkit.WebView;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import com.getcapacitor.Bridge;
import com.getcapacitor.BridgeActivity;
import com.getcapacitor.BridgeWebChromeClient;
import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class MainActivity extends BridgeActivity {
    private static boolean isPlayingMedia = false;

    public static void setPlayingMedia(boolean playing) {
        isPlayingMedia = playing;
    }

    public static class CustomWebChromeClient extends BridgeWebChromeClient {
        public CustomWebChromeClient(Bridge bridge) {
            super(bridge);
        }

        @Override
        public boolean onJsBeforeUnload(WebView view, String url, String message, JsResult result) {
            if (result != null) {
                result.confirm();
            }
            return true;
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            if (result != null) {
                result.confirm();
            }
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            if (result != null) {
                result.cancel();
            }
            return true;
        }

        @Override
        public boolean onJsPrompt(WebView view, String url, String message, String defaultValue, JsPromptResult result) {
            if (result != null) {
                result.cancel();
            }
            return true;
        }
    }

    public void injectTvEnvironment() {
        runOnUiThread(() -> {
            try {
                if (this.getBridge() != null) {
                    WebView webView = this.getBridge().getWebView();
                    if (webView != null) {
                        String js = "(function() {"
                            + "  window.__IS_ANDROID_TV__ = true;"
                            + "  document.documentElement.classList.add('is-native-app');"
                            + "  document.documentElement.classList.add('is-tv-mode');"
                            + "  document.documentElement.style.setProperty('--safe-top', '0px');"
                            + "  document.documentElement.style.setProperty('--safe-bottom', '0px');"
                            + "})();";
                        webView.evaluateJavascript(js, null);
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    public void hideSystemBars() {
        runOnUiThread(() -> {
            try {
                Window window = getWindow();
                if (window != null) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

                    WindowInsetsControllerCompat controller =
                        WindowCompat.getInsetsController(window, window.getDecorView());
                    if (controller != null) {
                        controller.setSystemBarsBehavior(
                            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                        );
                        controller.hide(WindowInsetsCompat.Type.systemBars());
                    }
                    window.getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                    );

                    injectTvEnvironment();
                }
            } catch (Exception ignored) {}
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
            injectTvEnvironment();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(FullScreenPlugin.class);
        registerPlugin(AdShieldPlugin.class);
        registerPlugin(PipPlugin.class);
        registerPlugin(AppInstallerPlugin.class);
        super.onCreate(savedInstanceState);

        // Lock to landscape for TV display
        try {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
        } catch (Exception ignored) {}

        hideSystemBars();

        try {
            WebView.setWebContentsDebuggingEnabled(true);
            WebView webView = this.getBridge().getWebView();
            if (webView != null) {
                WebSettings settings = webView.getSettings();
                settings.setMediaPlaybackRequiresUserGesture(false);
                settings.setJavaScriptCanOpenWindowsAutomatically(false);
                settings.setSupportMultipleWindows(false);
                settings.setTextZoom(100);

                // Enable D-pad and TV remote navigation focus
                webView.setFocusable(true);
                webView.setFocusableInTouchMode(true);
                webView.requestFocus();

                webView.setWebChromeClient(new CustomWebChromeClient(this.getBridge()));

                webView.setDownloadListener((url, userAgent, contentDisposition, mimetype, contentLength) -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        startActivity(intent);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                webView.postDelayed(this::injectTvEnvironment, 300);
                webView.postDelayed(this::injectTvEnvironment, 1000);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if (this.getBridge() != null) {
            WebView webView = this.getBridge().getWebView();
            if (webView != null && !webView.hasFocus()) {
                webView.requestFocus();
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        if (this.getBridge() != null) {
            WebView webView = this.getBridge().getWebView();
            if (webView != null) {
                webView.evaluateJavascript(
                    "(function() { return window.__WATZEM_TV_HANDLE_BACK__ ? window.__WATZEM_TV_HANDLE_BACK__() : false; })()",
                    result -> {
                        if ("false".equals(result) || "null".equals(result)) {
                            runOnUiThread(MainActivity.super::onBackPressed);
                        }
                    }
                );
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    public void onResume() {
        super.onResume();
        hideSystemBars();
        injectTvEnvironment();
        try {
            WebView webView = this.getBridge().getWebView();
            if (webView != null) {
                WebSettings settings = webView.getSettings();
                settings.setMediaPlaybackRequiresUserGesture(false);
                settings.setJavaScriptCanOpenWindowsAutomatically(false);
                settings.setSupportMultipleWindows(false);
                settings.setTextZoom(100);
                webView.postDelayed(this::injectTvEnvironment, 300);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (isPlayingMedia && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                    PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
                    builder.setAspectRatio(new Rational(16, 9));
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        builder.setAutoEnterEnabled(true);
                    }
                    enterPictureInPictureMode(builder.build());
                }
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);
        try {
            PipPlugin.notifyPipChange(isInPictureInPictureMode);
        } catch (Exception ignored) {}
    }

    @CapacitorPlugin(name = "SystemBars")
    public static class FullScreenPlugin extends Plugin {
        @PluginMethod
        public void setImmersiveFullscreen(PluginCall call) {
            getActivity().runOnUiThread(() -> {
                try {
                    MainActivity act = (MainActivity) getActivity();
                    if (act != null) {
                        act.hideSystemBars();
                    }
                    call.resolve();
                } catch (Exception e) {
                    call.reject(e.getMessage());
                }
            });
        }

        @PluginMethod
        public void exitImmersiveFullscreen(PluginCall call) {
            getActivity().runOnUiThread(() -> {
                try {
                    MainActivity act = (MainActivity) getActivity();
                    if (act != null) {
                        act.hideSystemBars();
                    }
                    call.resolve();
                } catch (Exception e) {
                    call.reject(e.getMessage());
                }
            });
        }

        @PluginMethod
        public void getSafeInsets(PluginCall call) {
            JSObject ret = new JSObject();
            ret.put("top", 0);
            call.resolve(ret);
        }

        @PluginMethod
        public void isAndroidTv(PluginCall call) {
            JSObject ret = new JSObject();
            ret.put("isTv", true);
            call.resolve(ret);
        }
    }

    @CapacitorPlugin(name = "AdShield")
    public static class AdShieldPlugin extends Plugin {
        private static final Set<String> ALLOWED_HOSTS = new HashSet<>(Arrays.asList(
            "localhost",
            "127.0.0.1",
            "watzem.online",
            "vidlink.pro",
            "vidsrc.in",
            "vidsrc.pm",
            "vidsrc.me",
            "vidsrc.to",
            "vidsrc.xyz",
            "vidsrc.net",
            "vidsrc.cc",
            "cinesrc.st",
            "cineflix.st",
            "a.cineflix.st",
            "a2.cineflix.st",
            "web.nxsha.app",
            "nxsha.app",
            "videasy.to",
            "player.videasy.to",
            "autoembed.co",
            "player.autoembed.co",
            "nextgencloudfabric.com",
            "cloudflare.com",
            "challenges.cloudflare.com",
            "turnstile.workers.dev",
            "cloudflareinsights.com",
            "cloudflarestorage.com",
            "moviesapi.vip",
            "player.moviesapi.vip",
            "opensubtitles.org",
            "vsembed.ru",
            "2embed.cc",
            "2embed.to",
            "2embed.org",
            "vidsrc.io",
            "multiembed.mov",
            "multiembed.to",
            "image.tmdb.org",
            "api.themoviedb.org",
            "themoviedb.org",
            "yts.mx",
            "youtube.com",
            "youtube-nocookie.com",
            "youtu.be",
            "googlevideo.com",
            "ytimg.com"
        ));

        @Override
        public Boolean shouldOverrideLoad(Uri url) {
            if (url == null) {
                return Boolean.TRUE;
            }

            String scheme = url.getScheme();
            if (scheme == null) {
                return Boolean.TRUE;
            }

            // Allow magnet links to launch user's torrent app
            if ("magnet".equalsIgnoreCase(scheme)) {
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, url);
                    getContext().startActivity(intent);
                } catch (Exception ignored) {}
                return Boolean.TRUE;
            }

            String host = url.getHost();

            if (host != null && (host.equals("youtube.com") || host.endsWith(".youtube.com") || host.equals("youtu.be"))) {
                String path = url.getPath();
                if (path != null && (path.startsWith("/watch") || (host.equals("youtu.be") && path.length() > 1))) {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, url);
                        getContext().startActivity(intent);
                        return Boolean.TRUE;
                    } catch (Exception ignored) {}
                }
            }

            if ("data".equalsIgnoreCase(scheme) || "blob".equalsIgnoreCase(scheme)) {
                return Boolean.FALSE;
            }

            if (host != null) {
                host = host.toLowerCase();
                for (String allowed : ALLOWED_HOSTS) {
                    if (host.equals(allowed) || host.endsWith("." + allowed)) {
                        return Boolean.FALSE;
                    }
                }
            }

            return Boolean.TRUE;
        }
    }

    @CapacitorPlugin(name = "PipPlugin")
    public static class PipPlugin extends Plugin {
        private static PipPlugin instance;

        @Override
        public void load() {
            super.load();
            instance = this;
        }

        public static void notifyPipChange(boolean inPip) {
            if (instance != null) {
                JSObject data = new JSObject();
                data.put("inPip", inPip);
                instance.notifyListeners("pipChange", data);
            }
        }

        @PluginMethod
        public void isPipSupported(PluginCall call) {
            boolean supported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                && getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE);
            JSObject ret = new JSObject();
            ret.put("supported", supported);
            call.resolve(ret);
        }

        @PluginMethod
        public void setAutoPip(PluginCall call) {
            boolean enabled = Boolean.TRUE.equals(call.getBoolean("enabled", true));
            MainActivity.setPlayingMedia(enabled);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getActivity().runOnUiThread(() -> {
                    try {
                        PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
                        builder.setAspectRatio(new Rational(16, 9));
                        builder.setAutoEnterEnabled(enabled);
                        getActivity().setPictureInPictureParams(builder.build());
                    } catch (Exception ignored) {}
                });
            }
            call.resolve();
        }

        @PluginMethod
        public void enterPip(PluginCall call) {
            getActivity().runOnUiThread(() -> {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
                            PictureInPictureParams.Builder builder = new PictureInPictureParams.Builder();
                            builder.setAspectRatio(new Rational(16, 9));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                builder.setAutoEnterEnabled(true);
                            }
                            boolean success = getActivity().enterPictureInPictureMode(builder.build());
                            JSObject ret = new JSObject();
                            ret.put("success", success);
                            call.resolve(ret);
                            return;
                        }
                    }
                    JSObject ret = new JSObject();
                    ret.put("success", false);
                    call.resolve(ret);
                } catch (Exception e) {
                    call.reject(e.getMessage());
                }
            });
        }
    }
}
