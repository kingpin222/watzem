package com.watzem.nativeapp.data.stream

import android.net.Uri
import android.util.Log
import android.webkit.JavascriptInterface

data class ExtractedStream(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val source: String = "sniffer"
)

object StreamExtractor {

    private const val TAG = "StreamExtractor"

    const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36"

    private val VIDEO_URL_REGEX = Regex(
        """\.(m3u8|mpd|mp4|webm|mkv)($|\?)|m3u8|mpegurl|\.mpd($|\?)|(/|_?)stream[/?]|/hls/|/dash/|/manifest/|/stream/|/playlist/|/source/""",
        RegexOption.IGNORE_CASE
    )

    fun unwrapProxyUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            val embeddedUrl = uri.getQueryParameter("url") ?: uri.getQueryParameter("src") ?: uri.getQueryParameter("file")
            if (!embeddedUrl.isNullOrBlank() && (embeddedUrl.startsWith("http://", ignoreCase = true) || embeddedUrl.startsWith("https://", ignoreCase = true))) {
                embeddedUrl
            } else {
                url
            }
        } catch (_: Exception) {
            url
        }
    }

    private val SKIP_URL_REGEX = Regex(
        """\.(jpe?g|png|gif|webp|svg|ico|css|woff2?|js|vtt|srt)($|\?)""",
        RegexOption.IGNORE_CASE
    )

    private val SKIP_HOST_REGEX = Regex(
        """image\.tmdb\.org|wsrv\.nl|googletagmanager|doubleclick|adsystem|google-analytics|clarity\.ms|yandex|recaptcha|hcaptcha""",
        RegexOption.IGNORE_CASE
    )

    fun isVideoStreamUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val trimmed = url.trim()
        if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return false
        }
        if (SKIP_URL_REGEX.containsMatchIn(trimmed)) return false
        if (SKIP_HOST_REGEX.containsMatchIn(trimmed)) return false
        return VIDEO_URL_REGEX.containsMatchIn(trimmed)
    }

    fun buildPlaybackHeaders(pageUrl: String, requestHeaders: Map<String, String> = emptyMap()): Map<String, String> {
        val uri = try { Uri.parse(pageUrl) } catch (_: Exception) { null }
        val scheme = uri?.scheme ?: "https"
        val host = uri?.host ?: ""
        val defaultOrigin = if (host.isNotEmpty()) "$scheme://$host" else pageUrl

        val headers = mutableMapOf(
            "User-Agent" to USER_AGENT
        )

        // Prioritize exact Referer & Origin from the actual browser request
        val referer = requestHeaders["Referer"] ?: requestHeaders["referer"]
        if (!referer.isNullOrBlank()) {
            headers["Referer"] = referer
            try {
                val refUri = Uri.parse(referer)
                headers["Origin"] = "${refUri.scheme}://${refUri.host}"
            } catch (_: Exception) {
                headers["Origin"] = defaultOrigin
            }
        } else {
            headers["Referer"] = if (pageUrl.endsWith("/")) pageUrl else "$pageUrl/"
            headers["Origin"] = defaultOrigin
        }

        val origin = requestHeaders["Origin"] ?: requestHeaders["origin"]
        if (!origin.isNullOrBlank()) {
            headers["Origin"] = origin
        }

        try {
            val cookie = android.webkit.CookieManager.getInstance().getCookie(referer ?: pageUrl)
            if (!cookie.isNullOrBlank()) {
                headers["Cookie"] = cookie
            }
        } catch (_: Exception) {}

        return headers
    }

    class StreamBridge(
        private val onStreamFound: (ExtractedStream) -> Unit,
        private val onLog: (String) -> Unit = {}
    ) {
        @JavascriptInterface
        fun onStreamFound(url: String, referer: String?, origin: String?) {
            val unwrappedUrl = unwrapProxyUrl(url)
            Log.d(TAG, "Bridge captured stream: $unwrappedUrl (raw=$url, referer=$referer)")
            val headers = mutableMapOf<String, String>()
            if (!referer.isNullOrBlank()) headers["Referer"] = referer
            if (!origin.isNullOrBlank()) headers["Origin"] = origin
            headers["User-Agent"] = USER_AGENT

            try {
                val cookieMgr = android.webkit.CookieManager.getInstance()
                val cookie = cookieMgr.getCookie(unwrappedUrl)
                    ?: if (!referer.isNullOrBlank()) cookieMgr.getCookie(referer) else null
                if (!cookie.isNullOrBlank()) {
                    headers["Cookie"] = cookie
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed retrieving cookies: ${e.message}")
            }

            onStreamFound(ExtractedStream(url = unwrappedUrl, headers = headers, source = "js_bridge"))
        }

        @JavascriptInterface
        fun log(msg: String) {
            Log.v(TAG, "[WebJS] $msg")
            onLog(msg)
        }
    }

    /**
     * Injected early before content load in WebView to intercept HLS (.m3u8), DASH (.mpd),
     * and MP4 direct stream sources across all nested frames, XHR, Fetch, and media elements.
     */
    val INJECTED_SNIFFER_JS: String = """
        (function() {
            if (window.__watzemSnifferInstalled) return;
            window.__watzemSnifferInstalled = true;

            function postStream(url, source) {
                if (!url || window.__watzemFound) return;
                window.__watzemFound = true;
                try {
                    if (window.WatzemStreamBridge && window.WatzemStreamBridge.onStreamFound) {
                        window.WatzemStreamBridge.onStreamFound(url, location.href, location.origin);
                    }
                } catch(e) {}
            }

            function log(m) {
                try {
                    if (window.WatzemStreamBridge && window.WatzemStreamBridge.log) {
                        window.WatzemStreamBridge.log(String(m));
                    }
                } catch(e) {}
            }

            var VIDEO_RE = /\.(m3u8|mpd|mp4|webm|mkv)($|\?)|m3u8|mpegurl|\.mpd($|\?)|(\/|_?)stream[/?]|\/hls\/|\/dash\/|\/manifest\/|\/playlist\/|\/source\//i;
            var SKIP_RE = /\.(jpe?g|png|gif|webp|svg|ico|css|woff2?|js|vtt|srt)($|\?)/i;
            var SKIP_HOST_RE = /image\.tmdb\.org|wsrv\.nl|googletagmanager|doubleclick|adsystem|google-analytics|clarity\.ms|yandex/i;

            function toAbsolute(u) {
                if (typeof u !== 'string') return '';
                var tr = u.trim();
                if (/^https?:\/\//i.test(tr)) return tr;
                if (tr.charAt(0) === '/') return location.origin + tr;
                return '';
            }

            function isHttp(u) { return typeof u === 'string' && /^https?:\/\//i.test(u); }
            function isSkippable(u) { return !isHttp(u) || SKIP_RE.test(u) || SKIP_HOST_RE.test(u); }

            function checkUrl(rawUrl, src) {
                var u = toAbsolute(rawUrl);
                if (isSkippable(u)) return;
                if (VIDEO_RE.test(u)) {
                    log('Detected video in ' + src + ': ' + u);
                    postStream(u, src);
                }
            }

            function walkJson(node) {
                if (!node || window.__watzemFound) return;
                if (typeof node === 'string') {
                    checkUrl(node, 'json_str');
                    return;
                }
                if (Array.isArray(node)) {
                    for (var i = 0; i < node.length && !window.__watzemFound; i++) walkJson(node[i]);
                    return;
                }
                if (typeof node !== 'object') return;

                if (typeof node.playlist === 'string') {
                    checkUrl(node.playlist, 'json.playlist');
                }
                if (typeof node.file === 'string') {
                    checkUrl(node.file, 'json.file');
                }
                if (typeof node.url === 'string') {
                    var absU = toAbsolute(node.url);
                    if (isHttp(absU)) {
                        var kind = String(node.type || node.kind || node.format || '').toLowerCase();
                        if (VIDEO_RE.test(absU) || /^(hls|dash|mp4|mpegurl|m3u8|mpd)$/i.test(kind)) {
                            postStream(absU, 'json.url');
                        }
                    }
                }
                for (var k in node) {
                    if (!Object.prototype.hasOwnProperty.call(node, k)) continue;
                    if (k === 'playlist' || k === 'file' || k === 'url') continue;
                    walkJson(node[k]);
                }
            }

            function inspectText(txt, url, src) {
                if (!txt || window.__watzemFound) return;
                var trimmed = String(txt).trim();
                if (trimmed.indexOf('#EXTM3U') === 0) {
                    log('Detected HLS playlist manifest from ' + src);
                    postStream(url, src + '_hls');
                    return;
                }
                if (trimmed.indexOf('<') === 0 && /<MPD[\s>]/i.test(trimmed.slice(0, 400))) {
                    log('Detected DASH MPD manifest from ' + src);
                    postStream(url, src + '_dash');
                    return;
                }
                if (trimmed.charAt(0) === '{' || trimmed.charAt(0) === '[') {
                    try { walkJson(JSON.parse(trimmed)); } catch(e) {}
                }
            }

            // Hook XMLHttpRequest
            var origXhrOpen = XMLHttpRequest.prototype.open;
            XMLHttpRequest.prototype.open = function() {
                this.addEventListener('load', function() {
                    try {
                        var resUrl = this.responseURL;
                        if (!resUrl) return;
                        checkUrl(resUrl, 'xhr_url');
                        var txt = typeof this.responseText === 'string' ? this.responseText : '';
                        inspectText(txt, resUrl, 'xhr_body');
                    } catch(e) {}
                });
                origXhrOpen.apply(this, arguments);
            };

            // Hook Fetch
            if (window.fetch) {
                var origFetch = window.fetch;
                window.fetch = function(input, init) {
                    var reqUrl = typeof input === 'string' ? input : (input && input.url);
                    return origFetch.apply(this, arguments).then(function(resp) {
                        try {
                            var rUrl = resp.url || reqUrl;
                            checkUrl(rUrl, 'fetch_url');
                            var ct = '';
                            try { ct = (resp.headers && resp.headers.get('content-type')) || ''; } catch(e) {}
                            if (!ct || /json|mpegurl|m3u8|dash|mpd|text|octet-stream|video/i.test(ct) || /\/api\//i.test(rUrl || '')) {
                                resp.clone().text().then(function(t) {
                                    inspectText(t, rUrl, 'fetch_body');
                                }).catch(function() {});
                            }
                        } catch(e) {}
                        return resp;
                    });
                };
            }

            // Hook <video> / <audio> src
            try {
                var mediaProto = window.HTMLMediaElement && window.HTMLMediaElement.prototype;
                if (mediaProto && !mediaProto.__watzemHooked) {
                    mediaProto.__watzemHooked = true;
                    var srcDesc = Object.getOwnPropertyDescriptor(mediaProto, 'src');
                    if (srcDesc && srcDesc.set) {
                        Object.defineProperty(mediaProto, 'src', {
                            configurable: true,
                            enumerable: srcDesc.enumerable,
                            get: function() { return srcDesc.get.call(this); },
                            set: function(v) {
                                checkUrl(String(v || ''), 'video.src');
                                srcDesc.set.call(this, v);
                            }
                        });
                    }
                    var origSetAttr = mediaProto.setAttribute;
                    mediaProto.setAttribute = function(name, value) {
                        if (String(name).toLowerCase() === 'src') {
                            checkUrl(String(value || ''), 'video.setAttribute');
                        }
                        return origSetAttr.apply(this, arguments);
                    };
                }
            } catch(e) {}

            // Hook VideoJS src if present
            function hookVideoJs() {
                try {
                    var vjs = window.videojs;
                    if (!vjs || vjs.__watzemHooked) return;
                    var Player = vjs.getComponent && vjs.getComponent('Player');
                    if (!Player || !Player.prototype || typeof Player.prototype.src !== 'function') return;
                    vjs.__watzemHooked = true;
                    var origVjsSrc = Player.prototype.src;
                    Player.prototype.src = function(source) {
                        try {
                            var list = Array.isArray(source) ? source : (source ? [source] : []);
                            for (var i = 0; i < list.length; i++) {
                                var s = list[i];
                                var u = typeof s === 'string' ? s : (s && (s.src || s.url));
                                if (u) checkUrl(String(u), 'videojs.src');
                            }
                        } catch(e) {}
                        return origVjsSrc.apply(this, arguments);
                    };
                } catch(e) {}
            }

            // Play button auto-clicker
            var PLAY_SELECTORS = [
                '#bigplay',
                '#overlay.act',
                '.playbtnx',
                '.vjs-big-play-button',
                '.jw-icon-playback',
                '.jw-display-icon-container',
                '.plyr__control--overlaid',
                '.mejs-overlay-play',
                '.mejs-overlay-button',
                '.fluid_initial_play',
                '.fluid_initial_play_button',
                '.clappr-play-wrapper',
                '.vjs-poster',
                'button.vjs-big-play-button',
                '[aria-label*="play" i]',
                '[title*="play" i]'
            ];

            function autoClickPlay() {
                if (window.__watzemFound) return;
                hookVideoJs();
                for (var i = 0; i < PLAY_SELECTORS.length; i++) {
                    var btn = document.querySelector(PLAY_SELECTORS[i]);
                    if (btn) {
                        try {
                            btn.click();
                            log('Auto-tapped play button: ' + PLAY_SELECTORS[i]);
                        } catch(e) {}
                    }
                }
                var vids = document.querySelectorAll('video');
                for (var j = 0; j < vids.length; j++) {
                    var v = vids[j];
                    var u = v.currentSrc || v.getAttribute('src');
                    if (u && !v.paused) checkUrl(u, 'video.playing');
                    try {
                        v.muted = true;
                        v.play();
                    } catch(e) {}
                }
            }

            setInterval(autoClickPlay, 800);
            log('Watzem stream sniffer active');
        })();
    """.trimIndent()
}

