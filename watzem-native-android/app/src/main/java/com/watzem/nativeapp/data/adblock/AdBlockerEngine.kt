package com.watzem.nativeapp.data.adblock

import android.net.Uri
import android.util.Log

object AdBlockerEngine {

    private const val TAG = "AdBlockerEngine"

    // Set of known ad, tracking, popup, and malware domains
    private val BLOCKED_DOMAINS = hashSetOf(
        // Google Ads & Tracking
        "googlesyndication.com",
        "googleadservices.com",
        "doubleclick.net",
        "adsbygoogle",
        "adtrafficquality.google",
        "pagead2.googlesyndication.com",

        // Video Ad Networks & Exchanges
        "connatix.com",
        "cds.connatix.com",
        "cd.connatix.com",
        "cks.connatix.com",
        "capi.connatix.com",
        "adscore.com",
        "c1.adscore.com",
        "3lift.com",
        "triplelift.net",
        "adform.net",
        "bidswitch.net",
        "bidgx.com",
        "bidgency.com",
        "criteo.com",
        "criteo.net",
        "bidr.io",
        "doubleverify.com",
        "moatads.com",
        "scorecardresearch.com",
        "sportradarserving.com",
        "spotxchange.com",
        "spotx.tv",
        "teads.tv",
        "springserve.com",
        "aniview.com",
        "primis.tech",
        "vidoomy.com",
        "targetvideo.com",
        "streamrail.net",
        "innity.com",
        "synacor.com",
        "kargo.com",

        // Popups, Popunders & Redirect Networks
        "popads.net",
        "popcash.net",
        "popmyads.com",
        "propellerads.com",
        "propellerclick.com",
        "exoclick.com",
        "trafficjunky.com",
        "realsrv.com",
        "juicyads.com",
        "adcash.com",
        "clickadu.com",
        "monetag.com",
        "hilltopads.net",
        "hilltopads.com",
        "galaksion.com",
        "richpush.co",
        "richads.co",
        "admaven.com",
        "yllix.com",
        "adoperator.com",
        "bidvertiser.com",
        "traffichunt.com",
        "clicksor.com",
        "infolinks.com",
        "revenuehits.com",
        "onclickperformance.com",
        "clksite.com",
        "deloton.com",
        "zeroredirect.com",
        "tsyndicate.com",
        "trafficstars.com",
        "ad-delivery.net",
        "adtrue.com",
        "clickthreat.net",
        "histats.com",
        "bet365.com",
        "1xbet.com",
        "directlink.me",
        "pushnotifications.co",
        "zoneid.info",
        "adsterra.com",
        "ad-score.com",
        "ad-center.com",
        "clickfunnels.com",
        "clickbank.net",
        "popmonetizer.com",
        "trafficfactory.biz",
        "ero-advertising.com",
        "leadbolt.com",
        "adsmol.com",
        "adsupply.com",
        "adzerk.net",
        "advertserve.com",
        "a-ads.com",
        "clarium.host",
        "bidgear.com",
        "adtng.com",
        "plugrush.com",
        "adtouch.me",
        "trafficforce.com",
        "trafficsignals.com",
        "smartbanner.com",
        "onclickalgo.com",
        "clickadilla.com",
        "evadav.com",
        "pushwoosh.com",
        "onesignal.com",

        // General Ad Trackers & Networks
        "adnxs.com",
        "appnexus.com",
        "cpmstar.com",
        "mgid.com",
        "taboola.com",
        "outbrain.com",
        "pubmatic.com",
        "openx.net",
        "rubiconproject.com",
        "casalemedia.com",
        "contextweb.com",
        "yieldmo.com",
        "smartadserver.com",
        "adpushup.com",
        "adkernel.com",
        "sonobi.com",
        "sovrn.com",
        "lijit.com",
        "ezoic.com",
        "revcontent.com",
        "zedo.com",
        "exponential.com",
        "conversantmedia.com",
        "media.net",
        "sharethrough.com",
        "undertone.com",
        "gumgum.com",
        "inmobi.com",
        "smaato.net",
        "quantserve.com",
        "chartbeat.com",
        "hotjar.com",
        "clarity.ms",
        "adtechus.com",
        "advertising.com",
        "adroll.com",
        "adblade.com",
        "zergnet.com",
        "serving-sys.com",
        "flashtalking.com",
        "sizmek.com",
        "integralads.com",
        "iasds.com",
        "creativecdn.com",

        // Anti-DevTools & Anti-Debugging trackers
        "disable-devtool",
        "disabledevtool",
        "anti-debug",
        "antidebug"
    )

    // Keywords in URLs that indicate ad banners or popup beacons
    private val BLOCKED_KEYWORDS = listOf(
        "/ads.",
        "/ad.",
        "/banner.",
        "/popunder",
        "/popads",
        "/adservice",
        "adsystem",
        "doubleclick",
        "syndication.",
        "clicktag",
        "delivery.ad",
        "track.ad",
        "pixel.ad",
        "ad_type=",
        "ad_tag="
    )

    /**
     * Checks whether a given URL should be blocked.
     */
    fun isAdUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val lower = url.lowercase()

        // Whitelist essential video streaming resources and CDNs
        if (isWhitelisted(lower)) {
            return false
        }

        // Check host match
        try {
            val uri = Uri.parse(lower)
            val host = uri.host ?: ""
            if (host.isNotEmpty()) {
                for (domain in BLOCKED_DOMAINS) {
                    if (host == domain || host.endsWith(".$domain") || host.contains(domain)) {
                        Log.d(TAG, "Blocked Ad Domain: $host (Rule: $domain)")
                        return true
                    }
                }
            }
        } catch (ignored: Exception) {}

        // Check keywords in full URL path
        for (kw in BLOCKED_KEYWORDS) {
            if (lower.contains(kw)) {
                Log.d(TAG, "Blocked Ad Keyword: $url (Keyword: $kw)")
                return true
            }
        }

        return false
    }

    private fun isWhitelisted(lowerUrl: String): Boolean {
        // Allow video manifests, segments, subtitles, and player libraries
        return lowerUrl.endsWith(".m3u8") ||
                lowerUrl.contains(".m3u8?") ||
                lowerUrl.endsWith(".ts") ||
                lowerUrl.contains(".ts?") ||
                lowerUrl.endsWith(".mp4") ||
                lowerUrl.contains(".mp4?") ||
                lowerUrl.endsWith(".vtt") ||
                lowerUrl.endsWith(".srt") ||
                lowerUrl.contains("tmdb.org") ||
                lowerUrl.contains("themoviedb.org") ||
                lowerUrl.contains("recaptcha") ||
                lowerUrl.contains("fonts.googleapis") ||
                lowerUrl.contains("fonts.gstatic")
    }

    /**
     * Injected JavaScript that runs inside the WebView before and after content loads.
     * Blocks popups, window.open, transparent clickjacking overlays, rogue anchor clicks,
     * and auto-skips or hides ad banners.
     */
    val INJECTED_ADBLOCK_JS = """
        (function() {
            if (window.__watzemAdBlockInjected) return;
            window.__watzemAdBlockInjected = true;

            try {
                // 1. Neutralize window.open popups
                var dummyWindow = {
                    closed: true,
                    length: 0,
                    opener: null,
                    parent: window,
                    top: window,
                    window: null,
                    name: '',
                    location: { href: 'about:blank', replace: function(){}, reload: function(){}, assign: function(){} },
                    document: { write: function(){}, open: function(){}, close: function(){} },
                    focus: function(){},
                    blur: function(){},
                    close: function(){},
                    postMessage: function(){},
                    addEventListener: function(){},
                    removeEventListener: function(){}
                };

                window.open = function(url, target, features) {
                    console.warn('[WATZEM AdBlock] Neutralized popup window.open -> ' + url);
                    return dummyWindow;
                };

                // Neutralize modal scareware dialogs
                window.alert = function() {};
                window.confirm = function() { return true; };
                window.prompt = function() { return null; };

                // 2. Prevent programmatic anchor click hijacking
                var origClick = HTMLAnchorElement.prototype.click;
                HTMLAnchorElement.prototype.click = function() {
                    var target = this.getAttribute('target');
                    var href = (this.getAttribute('href') || '').toLowerCase();
                    if (target === '_blank' || href.indexOf('http') === 0) {
                        console.warn('[WATZEM AdBlock] Blocked synthetic ad link click -> ' + href);
                        return;
                    }
                    return origClick.apply(this, arguments);
                };

                // 3. Document Click Hijack Protection (Capture phase)
                document.addEventListener('click', function(e) {
                    var el = e.target;
                    while (el && el !== document.body && el !== document.documentElement) {
                        if (el.tagName === 'A') {
                            var href = (el.getAttribute('href') || '').toLowerCase();
                            var target = el.getAttribute('target');
                            if (target === '_blank' && !href.includes('blob:') && !href.includes('magnet:')) {
                                e.preventDefault();
                                e.stopImmediatePropagation();
                                console.warn('[WATZEM AdBlock] Neutralized popup link click -> ' + href);
                                return false;
                            }
                        }
                        el = el.parentElement;
                    }
                }, true);

                // 4. Cosmetic Ad Hiding CSS
                var style = document.createElement('style');
                style.id = 'watzem-adblock-styles';
                style.textContent = `
                    [class*="popunder"], [id*="popunder"],
                    [class*="popup-ad"], [id*="popup-ad"],
                    [class*="ad-banner"], [id*="ad-banner"],
                    [class*="ad-overlay"], [id*="ad-overlay"],
                    .adsbygoogle, [data-ad-client],
                    iframe[src*="ad."], iframe[id*="ad."],
                    iframe[src*="popads"], iframe[src*="exoclick"],
                    iframe[src*="propeller"],
                    div[style*="z-index: 2147483647"]:not(:has(video)):not(:has(iframe)),
                    div[style*="z-index: 999999"]:not(:has(video)):not(:has(iframe)) {
                        display: none !important;
                        visibility: hidden !important;
                        pointer-events: none !important;
                        width: 0 !important;
                        height: 0 !important;
                    }
                `;
                (document.head || document.documentElement).appendChild(style);

                // 5. Remove transparent clickjacking overlay divs
                function removeClickjackOverlays() {
                    try {
                        var allDivs = document.querySelectorAll('div, a, span');
                        for (var i = 0; i < allDivs.length; i++) {
                            var node = allDivs[i];
                            if (!node || node.tagName === 'VIDEO' || node.querySelector('video') || node.querySelector('iframe')) continue;
                            
                            var cs = window.getComputedStyle(node);
                            if (cs.position === 'fixed' || cs.position === 'absolute') {
                                var z = parseInt(cs.zIndex, 10) || 0;
                                var isBig = (parseInt(cs.width, 10) >= window.innerWidth * 0.75) &&
                                            (parseInt(cs.height, 10) >= window.innerHeight * 0.75);
                                var isTransparent = cs.opacity === '0' || cs.backgroundColor === 'transparent' || cs.backgroundColor.indexOf('rgba(0, 0, 0, 0)') !== -1;
                                
                                if (z > 50 && isBig && isTransparent) {
                                    console.warn('[WATZEM AdBlock] Removed transparent clickjack overlay');
                                    node.remove();
                                }
                            }
                        }
                    } catch(e) {}
                }

                // Run immediately and periodically during stream initialization
                removeClickjackOverlays();
                setInterval(removeClickjackOverlays, 1000);

                // 6. MutationObserver to prune ad scripts and popunder iframes on insertion
                var observer = new MutationObserver(function(mutations) {
                    for (var i = 0; i < mutations.length; i++) {
                        var added = mutations[i].addedNodes;
                        for (var j = 0; j < added.length; j++) {
                            var n = added[j];
                            if (!n || !n.tagName) continue;

                            if (n.tagName === 'IFRAME') {
                                var src = (n.src || '').toLowerCase();
                                if (src.indexOf('popads') !== -1 || src.indexOf('exoclick') !== -1 || src.indexOf('propeller') !== -1 || src.indexOf('adsterra') !== -1) {
                                    console.warn('[WATZEM AdBlock] Pruned ad iframe: ' + src);
                                    n.remove();
                                }
                            }
                        }
                    }
                });

                if (document.documentElement) {
                    observer.observe(document.documentElement, { childList: true, subtree: true });
                }

                console.info('[WATZEM AdBlock] Active: Popups neutralized, clickjacking overlays removed, ad network requests blocked.');
            } catch (err) {
                console.error('[WATZEM AdBlock] Init error:', err);
            }
        })();
    """.trimIndent()
}

