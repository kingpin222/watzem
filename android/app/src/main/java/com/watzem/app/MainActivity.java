package com.watzem.app;

import android.app.PictureInPictureParams;
import android.app.UiModeManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Rational;
import android.view.DisplayCutout;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
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
            // Automatically confirm navigation and suppress "Confirm Navigation / Changes you made may not be saved" dialogs
            if (result != null) {
                result.confirm();
            }
            return true;
        }

        @Override
        public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
            // Suppress nuisance / rogue ad alerts from embedded players
            if (result != null) {
                result.confirm();
            }
            return true;
        }

        @Override
        public boolean onJsConfirm(WebView view, String url, String message, JsResult result) {
            // Suppress nuisance confirm dialogs from embedded players
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

        @Override
        public void onShowCustomView(View view, CustomViewCallback callback) {
            // Dismiss custom view callback safely to prevent OpenGL ES in-process GPU crashes
            if (callback != null) {
                try {
                    callback.onCustomViewHidden();
                } catch (Exception ignored) {}
            }
        }

        @Override
        public void onHideCustomView() {
            // Safe no-op
        }
    }

    public boolean isLandscape() {
        return getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
    }

    public int[] getSafeInsetsDp() {
        int topPx = 0;
        int bottomPx = 0;
        int leftPx = 0;
        int rightPx = 0;
        boolean isLand = isLandscape();

        try {
            if (!isLand) {
                int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
                if (resId > 0) {
                    topPx = getResources().getDimensionPixelSize(resId);
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                WindowInsets insets = getWindow().getDecorView().getRootWindowInsets();
                if (insets != null) {
                    DisplayCutout cutout = insets.getDisplayCutout();
                    if (cutout != null) {
                        topPx = Math.max(topPx, cutout.getSafeInsetTop());
                        bottomPx = Math.max(bottomPx, cutout.getSafeInsetBottom());
                        leftPx = Math.max(leftPx, cutout.getSafeInsetLeft());
                        rightPx = Math.max(rightPx, cutout.getSafeInsetRight());
                    }
                }
            }
        } catch (Exception ignored) {}

        float density = getResources().getDisplayMetrics().density;
        if (density <= 0) density = 1.0f;

        int topDp = Math.round(topPx / density);
        int bottomDp = Math.round(bottomPx / density);
        int leftDp = Math.round(leftPx / density);
        int rightDp = Math.round(rightPx / density);

        if (!isLand) {
            topDp = Math.max(topDp, 36);
            leftDp = 0;
            rightDp = 0;
        }

        return new int[]{ topDp, bottomDp, leftDp, rightDp };
    }

    public int getSafeTopInsetDp() {
        return getSafeInsetsDp()[0];
    }

    public void injectSafeInsets() {
        runOnUiThread(() -> {
            try {
                if (this.getBridge() != null) {
                    WebView webView = this.getBridge().getWebView();
                    if (webView != null) {
                        int[] insets = getSafeInsetsDp();
                        int safeTop = insets[0];
                        int safeBottom = insets[1];
                        int safeLeft = insets[2];
                        int safeRight = insets[3];
                        boolean isLand = isLandscape();
                        String js = "(function() {"
                            + "  var root = document.documentElement;"
                            + "  root.style.setProperty('--safe-top', '" + safeTop + "px');"
                            + "  root.style.setProperty('--safe-bottom', '" + safeBottom + "px');"
                            + "  root.style.setProperty('--safe-left', '" + safeLeft + "px');"
                            + "  root.style.setProperty('--safe-right', '" + safeRight + "px');"
                            + "  window.__ANDROID_SAFE_TOP__ = " + safeTop + ";"
                            + "  window.__ANDROID_SAFE_BOTTOM__ = " + safeBottom + ";"
                            + "  window.__ANDROID_SAFE_LEFT__ = " + safeLeft + ";"
                            + "  window.__ANDROID_SAFE_RIGHT__ = " + safeRight + ";"
                            + "  window.__ANDROID_ORIENTATION__ = '" + (isLand ? "landscape" : "portrait") + "';"
                            + "  root.classList.add('is-native-app');"
                            + "  root.classList.toggle('is-landscape', " + isLand + ");"
                            + "  root.classList.toggle('is-portrait', " + (!isLand) + ");"
                            + "})();";
                        webView.evaluateJavascript(js, null);
                    }
                }
            } catch (Exception ignored) {}
        });
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        hideSystemBars();
        injectSafeInsets();
        if (getWindow() != null && getWindow().getDecorView() != null) {
            getWindow().getDecorView().postDelayed(this::injectSafeInsets, 60);
            getWindow().getDecorView().postDelayed(this::injectSafeInsets, 250);
        }
    }

    public void hideSystemBars() {
        runOnUiThread(() -> {
            try {
                Window window = getWindow();
                if (window != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        window.getAttributes().layoutInDisplayCutoutMode =
                            WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
                    }
                    window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);

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

                    injectSafeInsets();
                }
            } catch (Exception ignored) {}
        });
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            hideSystemBars();
            injectSafeInsets();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(FullScreenPlugin.class);
        registerPlugin(AdShieldPlugin.class);
        registerPlugin(PipPlugin.class);
        registerPlugin(AppInstallerPlugin.class);
        super.onCreate(savedInstanceState);

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
                webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);

                // Enable D-pad remote navigation focus and touch bridge
                webView.setFocusable(true);
                webView.setFocusableInTouchMode(true);
                webView.addJavascriptInterface(new TvNativeBridge(), "TvNativeBridge");

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
                boolean isTv = getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK)
                    || ((UiModeManager) getSystemService(UI_MODE_SERVICE)).getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
                if (isTv) {
                    webView.evaluateJavascript("window.__IS_ANDROID_TV__ = true; document.documentElement.classList.add('is-tv-mode');", null);
                }

                webView.postDelayed(this::injectSafeInsets, 300);
                webView.postDelayed(this::injectSafeInsets, 1000);
            }
        } catch (Exception ignored) {}
    }

    public class TvNativeBridge {
        @JavascriptInterface
        public void clickCenter() {
            simulateTouchClick(0.5f, 0.5f);
        }

        @JavascriptInterface
        public void clickAt(final float relX, final float relY) {
            simulateTouchClick(relX, relY);
        }
    }

    public void simulateTouchClick(final float relX, final float relY) {
        runOnUiThread(() -> {
            try {
                if (getBridge() == null) return;
                WebView webView = getBridge().getWebView();
                if (webView == null) return;

                int width = webView.getWidth();
                int height = webView.getHeight();
                if (width <= 0 || height <= 0) return;

                float x = width * Math.max(0.01f, Math.min(0.99f, relX));
                float y = height * Math.max(0.01f, Math.min(0.99f, relY));

                long downTime = SystemClock.uptimeMillis();
                MotionEvent eventDown = MotionEvent.obtain(
                    downTime, downTime, MotionEvent.ACTION_DOWN, x, y, 0
                );
                eventDown.setSource(InputDevice.SOURCE_TOUCHSCREEN);
                webView.dispatchTouchEvent(eventDown);
                eventDown.recycle();

                webView.postDelayed(() -> {
                    try {
                        long upTime = SystemClock.uptimeMillis();
                        MotionEvent eventUp = MotionEvent.obtain(
                            downTime, upTime, MotionEvent.ACTION_UP, x, y, 0
                        );
                        eventUp.setSource(InputDevice.SOURCE_TOUCHSCREEN);
                        webView.dispatchTouchEvent(eventUp);
                        eventUp.recycle();
                    } catch (Exception ignored) {}
                }, 50);
            } catch (Exception ignored) {}
        });
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
    public void onResume() {
        super.onResume();
        hideSystemBars();
        injectSafeInsets();
        try {
            WebView webView = this.getBridge().getWebView();
            if (webView != null) {
                WebSettings settings = webView.getSettings();
                settings.setMediaPlaybackRequiresUserGesture(false);
                settings.setJavaScriptCanOpenWindowsAutomatically(false);
                settings.setSupportMultipleWindows(false);
                settings.setTextZoom(100);
                webView.postDelayed(this::injectSafeInsets, 300);
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
                    // Maintain global fullscreen permanently (no system bars)
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
            MainActivity act = (MainActivity) getActivity();
            int[] insets = act != null ? act.getSafeInsetsDp() : new int[]{36, 0, 0, 0};
            JSObject ret = new JSObject();
            ret.put("top", insets[0]);
            ret.put("bottom", insets[1]);
            ret.put("left", insets[2]);
            ret.put("right", insets[3]);
            ret.put("isLandscape", act != null && act.isLandscape());
            call.resolve(ret);
        }

        @PluginMethod
        public void isAndroidTv(PluginCall call) {
            boolean isTv = getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK)
                || ((UiModeManager) getActivity().getSystemService(UI_MODE_SERVICE)).getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
            JSObject ret = new JSObject();
            ret.put("isTv", isTv);
            call.resolve(ret);
        }
    }

    @CapacitorPlugin(name = "AdShield")
    public static class AdShieldPlugin extends Plugin {
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

            // Allow explicit YouTube watch links to launch native YouTube app if requested
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

            // Allow internal data and blob schemes
            if ("data".equalsIgnoreCase(scheme) || "blob".equalsIgnoreCase(scheme)) {
                return Boolean.FALSE;
            }

            // Top-level navigation must ONLY be permitted for localhost, 127.0.0.1, and watzem.online.
            // Under NO circumstances allow third-party stream servers (or ad scripts) to navigate
            // the main app WebView away from the Watzem SPA!
            if (host != null) {
                host = host.toLowerCase();
                if (host.equals("localhost") || host.equals("127.0.0.1") || host.endsWith(".watzem.online") || host.equals("watzem.online")) {
                    return Boolean.FALSE;
                }
            }

            // Block all other URLs: ad redirects, popunders, market:// links, rogue apps, stream embeds attempting top-level navigation
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
