/**
 * WATZEM Android TV & D-Pad Spatial Navigation Engine
 * Provides fluid 10-foot 2D directional navigation and smooth cinematic scrolling
 * for Android TV remotes, Google TV remotes, Fire TV remotes, and game controllers.
 */

type Direction = 'up' | 'down' | 'left' | 'right';

const FOCUSABLE_SELECTOR = [
  'button:not([disabled])',
  'a[href]',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex="0"]',
  '[role="button"]',
  '.movie-card',
  '.episode-card',
  '.cat-chip',
  '.search-cat-chip',
  '.explore-btn',
  '.explore-mode-chip',
  '.quick-year-pill',
  '.cat-year-chip',
  '.season-tab-btn',
  '.cta-watch-btn',
  '.cta-bookmark-btn',
  '.cta-trailer-btn',
  '.circle-nav-btn',
  '.back-btn',
  '.detail-back-btn',
  '.nav-tab',
  '.tv-nav-item',
  '.tv-card',
  '.btn-play',
  '.btn-details',
  '.btn-bookmark',
  '.load-more-btn',
  '.modal-close-btn',
  '.search-input',
  '.theater-server-btn',
  '.theater-next-btn',
  '.theater-episodes-btn',
  '.floating-theater-exit-btn',
  '.floating-theater-pip-btn',
  '.theater-ep-pill',
  '.menu-server-item',
  '.stop-playing-btn',
  '.ep-quick-play-btn',
  '.detail-anchor-link',
  '.section-jump-link',
  '.cast-card',
  '.keyword-chip',
  '.info-row-focusable',
  '.detail-back-to-top-btn',
  '[data-tv-focusable]'
].join(', ');

class TvSpatialNavManager {
  private currentFocused: HTMLElement | null = null;
  private lastFocusedCardId: string | null = null;
  private lastFocusedClass: string | null = null;
  private lastKnownRect: DOMRect | null = null;
  private lastScrollY = 0;
  private isDpadActive = false;
  private isInitialized = false;
  private gamepadRafId: number | null = null;
  private lastGamepadPress = 0;
  private lastCenterClickTime = 0;
  private onBackCallbacks: Array<() => boolean> = [];

  public init() {
    if (this.isInitialized || typeof window === 'undefined') return;
    this.isInitialized = true;

    // Listen to keydown events for Arrow keys and Android TV keycodes
    window.addEventListener('keydown', this.handleKeyDown.bind(this), { capture: true });

    // Sync currentFocused whenever an element receives focus (click, touch, programmatic)
    document.addEventListener('focusin', (e) => {
      const target = e.target as HTMLElement;
      if (target && target.matches && target.matches(FOCUSABLE_SELECTOR)) {
        if (this.currentFocused && this.currentFocused !== target) {
          this.currentFocused.classList.remove('tv-focused');
        }
        this.currentFocused = target;
        target.classList.add('tv-focused');

        // Track focus identity and screen coordinates for rock-solid infinite scroll focus retention
        const cardId = target.getAttribute('data-card-id');
        if (cardId) this.lastFocusedCardId = cardId;
        this.lastFocusedClass = target.className;
        this.lastKnownRect = target.getBoundingClientRect();
        this.lastScrollY = window.scrollY || document.documentElement.scrollTop;
      }
    }, { capture: true, passive: true });

    // Detect mouse/pointer usage to optionally soften D-pad highlight
    window.addEventListener('pointerdown', () => {
      this.isDpadActive = false;
      document.documentElement.classList.remove('is-dpad-active');
    }, { passive: true });

    // Start Gamepad API loop for TV remotes and Bluetooth controllers
    this.startGamepadLoop();

    // Expose global TV back & center click handlers for native Android bridge
    (window as any).__WATZEM_TV_HANDLE_BACK__ = () => this.handleBackButton();
    (window as any).__WATZEM_TV_HANDLE_CENTER__ = () => this.handleCenterAction();

    // Auto-focus initial element once DOM is ready
    if (document.readyState === 'complete') {
      this.autoFocusInitial();
    } else {
      window.addEventListener('load', () => this.autoFocusInitial(), { once: true });
    }
  }

  public registerBackHandler(callback: () => boolean): () => void {
    this.onBackCallbacks.push(callback);
    return () => {
      const idx = this.onBackCallbacks.indexOf(callback);
      if (idx !== -1) {
        this.onBackCallbacks.splice(idx, 1);
      }
    };
  }

  private activateDpadMode() {
    if (!this.isDpadActive) {
      this.isDpadActive = true;
      document.documentElement.classList.add('is-dpad-active');
    }
  }

  private handleKeyDown(e: KeyboardEvent) {
    const key = e.key;
    const keyCode = e.keyCode || e.which;

    // TV Remote D-Pad / Arrow keys
    const isUp = key === 'ArrowUp' || keyCode === 19 || keyCode === 38;
    const isDown = key === 'ArrowDown' || keyCode === 20 || keyCode === 40;
    const isLeft = key === 'ArrowLeft' || keyCode === 21 || keyCode === 37;
    const isRight = key === 'ArrowRight' || keyCode === 22 || keyCode === 39;
    const isEnter =
      key === 'Enter' ||
      key === ' ' ||
      key === 'Select' ||
      key === 'Ok' ||
      key === 'MediaPlayPause' ||
      keyCode === 23 || // KEYCODE_DPAD_CENTER
      keyCode === 13 || // KEYCODE_ENTER
      keyCode === 66 || // KEYCODE_ENTER (Android)
      keyCode === 179;  // KEYCODE_MEDIA_PLAY_PAUSE
    const isBack = key === 'Escape' || key === 'Backspace' || keyCode === 4 || keyCode === 111 || keyCode === 27;

    // Skip handling if inside text input (allow typing) unless navigating away
    const activeEl = document.activeElement as HTMLElement | null;
    const isTyping = activeEl && (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA');

    if (isUp || isDown || (isLeft && !isTyping) || (isRight && !isTyping)) {
      e.preventDefault();
      e.stopPropagation();
      this.activateDpadMode();

      const dir: Direction = isUp ? 'up' : isDown ? 'down' : isLeft ? 'left' : 'right';
      this.navigate(dir);
      return;
    }

    if (isEnter) {
      if (isTyping) {
        return;
      }
      e.preventDefault();
      e.stopPropagation();
      this.activateDpadMode();
      this.handleCenterAction();
      return;
    }

    if (isBack) {
      const handled = this.handleBackButton();
      if (handled) {
        e.preventDefault();
        e.stopPropagation();
      }
    }
  }

  public isPlayerActive(): boolean {
    const theaterModal = document.querySelector('.fullscreen-theater-overlay, .fullscreen-theater-modal') as HTMLElement | null;
    if (theaterModal && window.getComputedStyle(theaterModal).display !== 'none') {
      return true;
    }
    const trailerBackdrop = document.querySelector('.trailer-modal-backdrop, .trailer-frame-wrap') as HTMLElement | null;
    if (trailerBackdrop && window.getComputedStyle(trailerBackdrop).display !== 'none') {
      return true;
    }
    const inlinePlayer = document.querySelector('.inline-player-wrap') as HTMLElement | null;
    if (inlinePlayer && window.getComputedStyle(inlinePlayer).display !== 'none') {
      return true;
    }
    return false;
  }

  public arePlayerButtonsShowing(): boolean {
    // 1. Fullscreen theater overlay
    const theater = document.querySelector('.fullscreen-theater-overlay, .fullscreen-theater-modal') as HTMLElement | null;
    if (theater && window.getComputedStyle(theater).display !== 'none') {
      // If .controls-hidden is present, player buttons are hidden
      if (theater.classList.contains('controls-hidden')) {
        return false;
      }

      // Check if dropdown server menu or episodes menu is open
      const openMenu = theater.querySelector('.theater-server-menu, .theater-episodes-menu') as HTMLElement | null;
      if (openMenu && window.getComputedStyle(openMenu).display !== 'none') {
        return true;
      }

      // Check if exhausted server buttons overlay is open
      const exhausted = theater.querySelector('.exhausted-screen-overlay') as HTMLElement | null;
      if (exhausted && window.getComputedStyle(exhausted).display !== 'none') {
        return true;
      }

      // Check if top controls header is visible
      const topControls = theater.querySelector('.theater-top-controls') as HTMLElement | null;
      if (topControls) {
        const style = window.getComputedStyle(topControls);
        if (style.display !== 'none' && style.visibility !== 'hidden' && parseFloat(style.opacity || '1') > 0.1) {
          return true;
        }
      }

      // Check individual player buttons (server option, change server, exit, episodes, pip)
      const buttons = theater.querySelectorAll<HTMLElement>(
        '.theater-server-btn, .theater-next-btn, .floating-theater-exit-btn, .theater-episodes-btn, .floating-theater-pip-btn'
      );
      for (let i = 0; i < buttons.length; i++) {
        const btn = buttons[i];
        const style = window.getComputedStyle(btn);
        if (style.display !== 'none' && style.visibility !== 'hidden' && parseFloat(style.opacity || '1') > 0.1) {
          return true;
        }
      }

      return false;
    }

    // 2. Inline player wrap
    const inlineWrap = document.querySelector('.inline-player-wrap') as HTMLElement | null;
    if (inlineWrap && window.getComputedStyle(inlineWrap).display !== 'none') {
      if (inlineWrap.classList.contains('controls-hidden')) {
        return false;
      }
      const inlineBar = inlineWrap.querySelector('.inline-top-bar, .server-quick-controls') as HTMLElement | null;
      if (inlineBar) {
        const style = window.getComputedStyle(inlineBar);
        if (style.display !== 'none' && style.visibility !== 'hidden' && parseFloat(style.opacity || '1') > 0.1) {
          return true;
        }
      }
      return false;
    }

    // 3. Trailer modal
    const trailer = document.querySelector('.trailer-modal-backdrop, .trailer-frame-wrap') as HTMLElement | null;
    if (trailer && window.getComputedStyle(trailer).display !== 'none') {
      const closeBtn = trailer.querySelector('.trailer-close-btn') as HTMLElement | null;
      if (closeBtn && window.getComputedStyle(closeBtn).display !== 'none') {
        return true;
      }
    }

    return false;
  }

  private isInteractivePlayerButton(el: HTMLElement | null): boolean {
    if (!el || !document.body.contains(el) || !this.isElementVisible(el)) return false;

    // Filter out full-frame touch wake layers and player containers
    if (
      el.classList.contains('fullscreen-video-frame') ||
      el.classList.contains('theater-touch-wake-zone') ||
      el.classList.contains('inline-touch-wake-zone') ||
      el.classList.contains('fullscreen-theater-overlay') ||
      el.classList.contains('inline-player-wrap') ||
      el.classList.contains('trailer-frame-wrap') ||
      el.classList.contains('trailer-modal-backdrop') ||
      el.tagName === 'BODY'
    ) {
      return false;
    }

    // Must be an actual button, menu item, or episode pill
    return (
      el.tagName === 'BUTTON' ||
      el.tagName === 'A' ||
      el.classList.contains('menu-server-item') ||
      el.classList.contains('theater-ep-pill') ||
      el.classList.contains('theater-server-btn') ||
      el.classList.contains('theater-next-btn') ||
      el.classList.contains('floating-theater-exit-btn') ||
      el.classList.contains('floating-theater-pip-btn') ||
      el.classList.contains('theater-episodes-btn') ||
      el.classList.contains('player-server-btn') ||
      el.classList.contains('player-next-btn') ||
      el.classList.contains('player-exit-btn') ||
      el.classList.contains('player-pip-btn') ||
      el.classList.contains('player-fullscreen-btn') ||
      el.classList.contains('exhausted-retry-btn') ||
      el.classList.contains('exhausted-pick-btn') ||
      el.classList.contains('trailer-close-btn')
    );
  }

  public handleCenterAction(): boolean {
    const now = performance.now();
    if (now - this.lastCenterClickTime < 280) {
      return true;
    }

    if (this.isPlayerActive()) {
      // 1. Check if dropdown menu is open (Server selector, Episodes selector)
      const openMenu = document.querySelector(
        '.theater-server-menu, .theater-episodes-menu, .player-server-menu'
      ) as HTMLElement | null;
      const isMenuOpen = openMenu && window.getComputedStyle(openMenu).display !== 'none';

      // 2. Check if exhausted mirrors overlay is open
      const exhausted = document.querySelector('.exhausted-screen-overlay') as HTMLElement | null;
      const isExhaustedOpen = exhausted && window.getComputedStyle(exhausted).display !== 'none';

      // 3. Check if user intentionally navigated focus onto an interactive control button with D-pad arrows
      const hasFocusedButton = this.isInteractivePlayerButton(this.currentFocused);

      if (isMenuOpen || isExhaustedOpen || hasFocusedButton) {
        this.lastCenterClickTime = now;
        if (hasFocusedButton && this.currentFocused) {
          this.currentFocused.focus();
          try {
            this.currentFocused.click();
          } catch {}
          return true;
        }

        // If a menu is open but no specific item was focused yet, focus the active item or first item
        if (isMenuOpen || isExhaustedOpen) {
          const container = (openMenu || exhausted) as HTMLElement;
          const activeItem = container.querySelector<HTMLElement>('.menu-server-item.active, .theater-ep-pill.active');
          if (activeItem && this.isElementVisible(activeItem)) {
            this.focusElement(activeItem);
            return true;
          }
          const focusable = this.getFocusableElements(container);
          if (focusable.length > 0) {
            this.focusElement(focusable[0]);
            return true;
          }
        }
      }

      // 4. Default: User presses D-Pad Center during video playback -> TOGGLE PLAY / PAUSE!
      this.lastCenterClickTime = now;
      this.togglePlayerPlayback();
      return true;
    }

    // Default action for standard UI elements outside the player
    if (this.currentFocused) {
      this.lastCenterClickTime = now;
      this.currentFocused.focus();
      try {
        this.currentFocused.click();
      } catch {}
      return true;
    }

    return false;
  }

  public togglePlayerPlayback(): void {
    // 1. If StreamPlayer has registered global toggle method, execute it!
    try {
      const globalToggle = (window as any).__WATZEM_TOGGLE_PLAY_PAUSE__;
      if (typeof globalToggle === 'function') {
        globalToggle();
        return;
      }
    } catch (e) {
      console.warn('[TvSpatialNav] Global togglePlayPause call failed:', e);
    }

    // 2. Fallback: Broadcast event
    window.dispatchEvent(new CustomEvent('watzem-toggle-play-pause'));

    // 3. Fallback: PostMessage commands directly to player iframes
    const iframes = document.querySelectorAll<HTMLIFrameElement>(
      '.fullscreen-iframe, .stream-iframe, .trailer-iframe, .fullscreen-theater-overlay iframe, .inline-player-wrap iframe'
    );
    iframes.forEach((iframe) => {
      try {
        const win = iframe.contentWindow;
        if (!win) return;
        win.postMessage({ type: 'toggle' }, '*');
        win.postMessage({ type: 'playpause' }, '*');
        win.postMessage({ type: 'cinesrc:toggle' }, '*');
        win.postMessage(JSON.stringify({ event: 'playToggle' }), '*');
        win.postMessage(JSON.stringify({ method: 'playPause' }), '*');
        win.postMessage(JSON.stringify({ method: 'playToggle' }), '*');
        win.postMessage(JSON.stringify({ action: 'toggle' }), '*');
        win.postMessage(JSON.stringify({ api: 'toggle' }), '*');
      } catch {}
    });

    // 4. Fallback: Native Android bridge touch simulation & synthetic click at player center
    this.clickPlayerCenter();
  }

  public clickPlayerCenter(): void {
    // 1. Locate active player frame, iframe, or container
    const playerEl = (
      document.querySelector('.fullscreen-video-frame') ||
      document.querySelector('.fullscreen-theater-overlay iframe') ||
      document.querySelector('.fullscreen-theater-modal iframe') ||
      document.querySelector('.trailer-frame-wrap iframe') ||
      document.querySelector('.stream-iframe') ||
      document.querySelector('.inline-player-wrap') ||
      document.querySelector('.trailer-modal-backdrop')
    ) as HTMLElement | null;

    let centerX = Math.round(window.innerWidth / 2);
    let centerY = Math.round(window.innerHeight / 2);
    let relX = 0.5;
    let relY = 0.5;

    if (playerEl) {
      const rect = playerEl.getBoundingClientRect();
      if (rect.width > 0 && rect.height > 0) {
        centerX = Math.round(rect.left + rect.width / 2);
        centerY = Math.round(rect.top + rect.height / 2);
        relX = centerX / (window.innerWidth || 1);
        relY = centerY / (window.innerHeight || 1);
      }
    }

    // 2. Dispatch hardware MotionEvent through Native Android Bridge into WebView & cross-origin iframes
    try {
      const bridge = (window as any).TvNativeBridge;
      if (bridge && typeof bridge.clickAt === 'function') {
        bridge.clickAt(relX, relY);
      } else if (bridge && typeof bridge.clickCenter === 'function') {
        bridge.clickCenter();
      }
    } catch (err) {
      console.warn('[TvSpatialNav] TvNativeBridge click failed:', err);
    }

    // 3. Dispatch synthetic DOM pointer and click events for HTML5 players and web targets
    try {
      const target = document.elementFromPoint(centerX, centerY) || playerEl;
      if (target) {
        target.dispatchEvent(new PointerEvent('pointerdown', { clientX: centerX, clientY: centerY, bubbles: true }));
        target.dispatchEvent(new MouseEvent('mousedown', { clientX: centerX, clientY: centerY, bubbles: true }));
        target.dispatchEvent(new PointerEvent('pointerup', { clientX: centerX, clientY: centerY, bubbles: true }));
        target.dispatchEvent(new MouseEvent('mouseup', { clientX: centerX, clientY: centerY, bubbles: true }));
        target.dispatchEvent(new MouseEvent('click', { clientX: centerX, clientY: centerY, bubbles: true }));
      }
    } catch {}

    // 4. Visual TV Ripple confirmation at center
    this.showCenterClickRipple(centerX, centerY);

    // 5. Broadcast event
    window.dispatchEvent(new CustomEvent('watzem-tv-center-click', {
      detail: { centerX, centerY, relX, relY }
    }));
  }

  private showCenterClickRipple(x: number, y: number): void {
    try {
      const existing = document.getElementById('tv-center-click-ripple');
      if (existing) existing.remove();

      const ripple = document.createElement('div');
      ripple.id = 'tv-center-click-ripple';
      ripple.className = 'tv-center-click-ripple';
      ripple.style.left = `${x}px`;
      ripple.style.top = `${y}px`;
      ripple.innerHTML = `<div class="tv-ripple-inner"><svg width="34" height="34" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><polygon points="6 4 20 12 6 20 6 4" fill="currentColor"/></svg></div>`;
      document.body.appendChild(ripple);

      requestAnimationFrame(() => {
        ripple.classList.add('tv-ripple-animate');
      });

      setTimeout(() => {
        ripple.remove();
      }, 450);
    } catch {}
  }

  public handleBackButton(): boolean {
    // 1. Run custom back handlers (player, modal, detail view, etc.)
    for (let i = this.onBackCallbacks.length - 1; i >= 0; i--) {
      try {
        if (this.onBackCallbacks[i]()) {
          return true;
        }
      } catch {}
    }

    // 2. Check for active theater player
    const theaterOverlay = document.querySelector('.fullscreen-theater-overlay, .fullscreen-theater-modal') as HTMLElement;
    if (theaterOverlay && window.getComputedStyle(theaterOverlay).display !== 'none') {
      const openServerMenu = document.querySelector('.theater-server-menu, .player-server-menu') as HTMLElement | null;
      const openEpisodesMenu = document.querySelector('.theater-episodes-menu') as HTMLElement | null;
      if (openServerMenu && window.getComputedStyle(openServerMenu).display !== 'none') {
        const closeFn = (window as any).__WATZEM_CLOSE_SERVER_MENU__;
        if (typeof closeFn === 'function') {
          closeFn();
        } else {
          const serverBtn = theaterOverlay.querySelector('.theater-server-btn, .player-server-btn') as HTMLElement | null;
          if (serverBtn) serverBtn.click();
        }
        this.clearFocus();
        return true;
      }
      if (openEpisodesMenu && window.getComputedStyle(openEpisodesMenu).display !== 'none') {
        const epBtn = theaterOverlay.querySelector('.theater-episodes-btn') as HTMLElement | null;
        if (epBtn) epBtn.click();
        this.clearFocus();
        return true;
      }

      const exitBtn = theaterOverlay.querySelector('.floating-theater-exit-btn, .close-btn') as HTMLElement;
      if (exitBtn) {
        exitBtn.click();
        return true;
      }
    }

    // 3. Check for open modals
    const modalCloseBtn = document.querySelector('.modal-card .close-btn, .app-update-modal .btn-secondary, .support-modal .btn-secondary, .trailer-close-btn') as HTMLElement;
    if (modalCloseBtn && modalCloseBtn.offsetParent !== null) {
      modalCloseBtn.click();
      return true;
    }

    // 4. If focused in content, focus back to TV Sidebar
    const sidebar = document.querySelector('.tv-sidebar') as HTMLElement;
    if (sidebar && (!this.currentFocused || !sidebar.contains(this.currentFocused))) {
      const activeNavTab = sidebar.querySelector('.tv-nav-item.active, .tv-nav-item') as HTMLElement;
      if (activeNavTab) {
        this.focusElement(activeNavTab);
        return true;
      }
    }

    // 5. If focused in content, focus back to Bottom Nav (in portrait/mobile)
    const bottomNav = document.querySelector('.bottom-nav') as HTMLElement;
    if (bottomNav && (!this.currentFocused || !bottomNav.contains(this.currentFocused))) {
      const activeNavTab = bottomNav.querySelector('.nav-tab.active, .nav-tab') as HTMLElement;
      if (activeNavTab && this.isElementVisible(activeNavTab)) {
        this.focusElement(activeNavTab);
        return true;
      }
    }

    return false;
  }

  public navigate(dir: Direction) {
    const serverMenu = document.querySelector('.theater-server-menu, .player-server-menu') as HTMLElement | null;
    const isServerMenuOpen = serverMenu && window.getComputedStyle(serverMenu).display !== 'none';

    // 1. If player is active and no menu is open, pressing UP directly opens Server Selection!
    if (this.isPlayerActive() && !isServerMenuOpen) {
      const episodesMenu = document.querySelector('.theater-episodes-menu') as HTMLElement | null;
      const isEpisodesOpen = episodesMenu && window.getComputedStyle(episodesMenu).display !== 'none';

      if (!isEpisodesOpen && dir === 'up') {
        const openFn = (window as any).__WATZEM_OPEN_SERVER_MENU__;
        if (typeof openFn === 'function') {
          openFn(true);
          return;
        }
        const sBtn = document.querySelector('.theater-server-btn, .player-server-btn') as HTMLElement | null;
        if (sBtn) {
          sBtn.click();
          return;
        }
      }
    }

    // 2. If Server Selection menu is open: provide smooth cyclic vertical navigation & wrap-around
    if (isServerMenuOpen && serverMenu) {
      const serverItems = Array.from(serverMenu.querySelectorAll<HTMLElement>('.menu-server-item')).filter(el => this.isElementVisible(el));
      if (serverItems.length > 0) {
        let currentIndex = this.currentFocused ? serverItems.indexOf(this.currentFocused) : -1;
        if (currentIndex === -1) {
          const activeItem = serverMenu.querySelector<HTMLElement>('.menu-server-item.active');
          currentIndex = activeItem ? serverItems.indexOf(activeItem) : 0;
          if (currentIndex === -1) currentIndex = 0;
          this.focusElement(serverItems[currentIndex]);
          return;
        }

        if (dir === 'down') {
          const nextIndex = currentIndex < serverItems.length - 1 ? currentIndex + 1 : 0;
          this.focusElement(serverItems[nextIndex]);
          return;
        } else if (dir === 'up') {
          const nextIndex = currentIndex > 0 ? currentIndex - 1 : serverItems.length - 1;
          this.focusElement(serverItems[nextIndex]);
          return;
        } else if (dir === 'right') {
          const epBtn = document.querySelector('.theater-episodes-btn') as HTMLElement | null;
          if (epBtn && this.isElementVisible(epBtn)) {
            const closeFn = (window as any).__WATZEM_CLOSE_SERVER_MENU__;
            if (closeFn) closeFn();
            this.focusElement(epBtn);
            return;
          }
        } else if (dir === 'left') {
          const sBtn = document.querySelector('.theater-server-btn, .player-server-btn') as HTMLElement | null;
          if (sBtn && this.isElementVisible(sBtn)) {
            this.focusElement(sBtn);
            return;
          }
        }
        return;
      }
    }

    const scope = this.getActiveScopeContainer();
    let candidates = this.getFocusableElements(scope);

    if (candidates.length === 0) {
      if (this.isPlayerActive() && !this.arePlayerButtonsShowing()) {
        const wakeZone = document.querySelector('.theater-touch-wake-zone, .inline-touch-wake-zone') as HTMLElement | null;
        if (wakeZone) {
          wakeZone.click();
        } else {
          const theater = document.querySelector('.fullscreen-theater-overlay') as HTMLElement | null;
          if (theater) {
            theater.dispatchEvent(new PointerEvent('pointermove', { bubbles: true }));
          }
        }
        setTimeout(() => {
          const newScope = this.getActiveScopeContainer();
          this.focusFirstVisible(newScope);
        }, 120);
      }
      return;
    }

    if (!this.currentFocused || !document.body.contains(this.currentFocused) || !this.isElementVisible(this.currentFocused)) {
      // 1. Try restoring focus by last card ID if available
      if (this.lastFocusedCardId) {
        const matchingCard = document.querySelector<HTMLElement>(`[data-card-id="${this.lastFocusedCardId}"]`);
        if (matchingCard && this.isElementVisible(matchingCard)) {
          this.focusElement(matchingCard);
          return;
        }
      }

      // 2. Try restoring focus to .load-more-btn if that was last focused
      if (this.lastFocusedClass?.includes('load-more-btn')) {
        const loadBtn = document.querySelector<HTMLElement>('.load-more-btn');
        if (loadBtn && this.isElementVisible(loadBtn)) {
          this.focusElement(loadBtn);
          return;
        }
      }

      // 3. Fallback: focus closest visible candidate in viewport (zero jumping to top hero banner)
      this.focusFirstVisible(scope);
      return;
    }

    const currentRect = this.currentFocused.getBoundingClientRect();
    const curCenterX = (currentRect.left + currentRect.right) / 2;
    const curCenterY = (currentRect.top + currentRect.bottom) / 2;
    const isCurrentInSidebar = !!this.currentFocused.closest('.tv-sidebar');
    const isCurrentInBottomNav = !!this.currentFocused.closest('.bottom-nav');

    // Case 1: Moving RIGHT from TV sidebar into content area
    if (isCurrentInSidebar && dir === 'right') {
      const contentCandidates = candidates.filter(el => !el.closest('.tv-sidebar'));
      if (contentCandidates.length > 0) {
        let bestContentEl: HTMLElement | null = null;
        let bestScore = Infinity;

        for (const el of contentCandidates) {
          const r = el.getBoundingClientRect();
          if (r.bottom < 40 || r.top > (window.innerHeight || 1080) - 40) continue;

          const centerY = (r.top + r.bottom) / 2;
          const isPrimary = el.classList.contains('btn-play') || el.classList.contains('cta-watch-btn');
          const bonus = isPrimary ? -400 : 0;
          const score = r.left + Math.abs(centerY - curCenterY) * 1.2 + bonus;
          if (score < bestScore) {
            bestScore = score;
            bestContentEl = el;
          }
        }

        if (bestContentEl) {
          this.focusElement(bestContentEl);
          return;
        }
      }
    }

    // Case 2: Moving LEFT from content area towards sidebar
    if (!isCurrentInSidebar && !isCurrentInBottomNav && dir === 'left') {
      const hasContentLeft = candidates.some(el => {
        if (el === this.currentFocused || el.closest('.tv-sidebar, .bottom-nav')) return false;
        const r = el.getBoundingClientRect();
        return (r.left + r.right) / 2 < curCenterX - 15;
      });

      if (!hasContentLeft) {
        const sidebar = document.querySelector('.tv-sidebar');
        if (sidebar) {
          const activeNavTab = sidebar.querySelector('.tv-nav-item.active') as HTMLElement;
          if (activeNavTab && this.isElementVisible(activeNavTab)) {
            this.focusElement(activeNavTab);
            return;
          }
          const sidebarCandidates = candidates.filter(el => el.closest('.tv-sidebar'));
          if (sidebarCandidates.length > 0) {
            sidebarCandidates.sort((a, b) => {
              const ra = a.getBoundingClientRect();
              const rb = b.getBoundingClientRect();
              return Math.abs((ra.top + ra.bottom) / 2 - curCenterY) - Math.abs((rb.top + rb.bottom) / 2 - curCenterY);
            });
            this.focusElement(sidebarCandidates[0]);
            return;
          }
        }
      }
    }

    // Case 3: Moving inside or out of Bottom Nav
    if (isCurrentInBottomNav) {
      if (dir === 'left' || dir === 'right') {
        candidates = candidates.filter(el => !!el.closest('.bottom-nav'));
      } else if (dir === 'up') {
        const contentCandidates = candidates.filter(el => !el.closest('.bottom-nav, .tv-sidebar'));
        if (contentCandidates.length > 0) {
          const nextEl = this.findBestCandidate(currentRect, contentCandidates, 'up');
          if (nextEl) {
            this.focusElement(nextEl);
            return;
          }
          // Fallback: choose visible content candidate closest to bottom of screen
          let bestEl = contentCandidates[0];
          let maxBottom = -Infinity;
          for (const el of contentCandidates) {
            const r = el.getBoundingClientRect();
            if (r.bottom > maxBottom && r.top < currentRect.top) {
              maxBottom = r.bottom;
              bestEl = el;
            }
          }
          this.focusElement(bestEl);
          return;
        }
      } else if (dir === 'down') {
        return; // Already at bottom of screen in bottom nav
      }
    }

    // Case 4: In content area moving DOWN towards Bottom Nav when at the bottom of content
    if (!isCurrentInSidebar && !isCurrentInBottomNav && dir === 'down') {
      const contentBelow = candidates.filter(el => {
        if (el === this.currentFocused || el.closest('.tv-sidebar, .bottom-nav')) return false;
        const r = el.getBoundingClientRect();
        return r.top > curCenterY + 10;
      });

      if (contentBelow.length === 0) {
        const bottomNavTabs = candidates.filter(el => !!el.closest('.bottom-nav'));
        if (bottomNavTabs.length > 0) {
          let bestTab = bottomNavTabs[0];
          let minDiff = Infinity;
          for (const tab of bottomNavTabs) {
            const tr = tab.getBoundingClientRect();
            const diff = Math.abs((tr.left + tr.right) / 2 - curCenterX);
            if (diff < minDiff) {
              minDiff = diff;
              bestTab = tab;
            }
          }
          this.focusElement(bestTab);
          return;
        }
      }
    }

    // Scope isolation:
    // When inside sidebar: UP and DOWN must stay strictly within the sidebar
    if (isCurrentInSidebar && (dir === 'up' || dir === 'down')) {
      candidates = candidates.filter(el => !!el.closest('.tv-sidebar'));
    }

    // When inside bottom nav: LEFT and RIGHT stay strictly within the bottom nav
    if (isCurrentInBottomNav && (dir === 'left' || dir === 'right')) {
      candidates = candidates.filter(el => !!el.closest('.bottom-nav'));
    }

    // When inside content: general spatial scoring stays within content
    if (!isCurrentInSidebar && !isCurrentInBottomNav) {
      candidates = candidates.filter(el => !el.closest('.tv-sidebar, .bottom-nav'));
    }

    const nextEl = this.findBestCandidate(currentRect, candidates, dir);

    if (nextEl) {
      this.focusElement(nextEl);
    } else if (this.isPlayerActive() && dir === 'down') {
      // If user is focused on top controls in player and presses DOWN:
      // Clear focus back to video body so D-Pad Center immediately toggles play/pause!
      this.clearFocus();
    }
  }

  private findBestCandidate(currentRect: DOMRect, candidates: HTMLElement[], dir: Direction): HTMLElement | null {
    let bestCandidate: HTMLElement | null = null;
    let minScore = Infinity;

    const curLeft = currentRect.left;
    const curRight = currentRect.right;
    const curTop = currentRect.top;
    const curBottom = currentRect.bottom;
    const curCenterX = (curLeft + curRight) / 2;
    const curCenterY = (curTop + curBottom) / 2;

    for (const el of candidates) {
      if (el === this.currentFocused) continue;

      const rect = el.getBoundingClientRect();
      const elCenterX = (rect.left + rect.right) / 2;
      const elCenterY = (rect.top + rect.bottom) / 2;

      let primaryDist = 0;
      let secondaryDist = 0;
      let hasOverlap = false;

      // Generous threshold to tolerate scale transforms (1.08x)
      const threshold = 10;

      switch (dir) {
        case 'right': {
          if (elCenterX <= curCenterX + threshold) continue;
          if (rect.right <= curLeft + 20) continue;

          primaryDist = Math.max(0, rect.left - curRight);
          if (primaryDist === 0) {
            primaryDist = Math.max(1, elCenterX - curCenterX);
          }

          const overlap = Math.min(curBottom, rect.bottom) - Math.max(curTop, rect.top);
          hasOverlap = overlap > 6;
          secondaryDist = Math.abs(curCenterY - elCenterY);
          break;
        }

        case 'left': {
          if (elCenterX >= curCenterX - threshold) continue;
          if (rect.left >= curRight - 20) continue;

          primaryDist = Math.max(0, curLeft - rect.right);
          if (primaryDist === 0) {
            primaryDist = Math.max(1, curCenterX - elCenterX);
          }

          const overlap = Math.min(curBottom, rect.bottom) - Math.max(curTop, rect.top);
          hasOverlap = overlap > 6;
          secondaryDist = Math.abs(curCenterY - elCenterY);
          break;
        }

        case 'down': {
          if (elCenterY <= curCenterY + threshold) continue;
          if (rect.bottom <= curTop + 20) continue;

          primaryDist = Math.max(0, rect.top - curBottom);
          if (primaryDist === 0) {
            primaryDist = Math.max(1, elCenterY - curCenterY);
          }

          const overlap = Math.min(curRight, rect.right) - Math.max(curLeft, rect.left);
          hasOverlap = overlap > 6;
          secondaryDist = Math.abs(curCenterX - elCenterX);
          break;
        }

        case 'up': {
          if (elCenterY >= curCenterY - threshold) continue;
          if (rect.top >= curBottom - 20) continue;

          primaryDist = Math.max(0, curTop - rect.bottom);
          if (primaryDist === 0) {
            primaryDist = Math.max(1, curCenterY - elCenterY);
          }

          const overlap = Math.min(curRight, rect.right) - Math.max(curLeft, rect.left);
          hasOverlap = overlap > 6;
          secondaryDist = Math.abs(curCenterX - elCenterX);
          break;
        }
      }

      // Projection scoring: massive priority for elements aligned on the same row or column
      let score: number;
      if (hasOverlap) {
        score = primaryDist + (secondaryDist * 0.4);
      } else {
        score = (primaryDist * 1.2) + (secondaryDist * 3.2);
      }

      // Container affinity bonus: prefer elements that share the same container
      if (this.currentFocused && el.parentElement === this.currentFocused.parentElement) {
        score *= 0.82;
      }

      if (score < minScore) {
        minScore = score;
        bestCandidate = el;
      }
    }

    return bestCandidate;
  }

  public scrollPageVertical(scrollOffset: number) {
    if (Math.abs(scrollOffset) < 2) return;

    // 1. Try document.scrollingElement
    const scroller = document.scrollingElement || document.body || document.documentElement;
    if (scroller && scroller.scrollHeight > scroller.clientHeight + 4) {
      scroller.scrollBy({ top: scrollOffset, behavior: 'smooth' });
    }

    // 2. Also ensure document.body and documentElement receive scroll if scroller didn't cover it
    if (document.body && document.body !== scroller && document.body.scrollHeight > document.body.clientHeight + 4) {
      document.body.scrollBy({ top: scrollOffset, behavior: 'smooth' });
    }
    if (document.documentElement && document.documentElement !== scroller && document.documentElement.scrollHeight > document.documentElement.clientHeight + 4) {
      document.documentElement.scrollBy({ top: scrollOffset, behavior: 'smooth' });
    }

    // 3. Fallback window.scrollBy
    window.scrollBy({ top: scrollOffset, behavior: 'smooth' });
  }

  public smoothScrollIntoTvView(el: HTMLElement) {
    if (!el) return;

    // 1. Skip scrolling if element is inside fixed TV sidebar or fixed bottom nav
    if (el.closest('.tv-sidebar, .bottom-nav')) {
      return;
    }

    // 2. Handle horizontal scroll containers (e.g. category chip bar, episodes rail, season tabs, cast rail)
    let parent = el.parentElement;
    while (parent && parent !== document.body && parent !== document.documentElement) {
      const style = window.getComputedStyle(parent);
      const overflowX = style.overflowX;
      const isScrollableX = (overflowX === 'auto' || overflowX === 'scroll') && parent.scrollWidth > parent.clientWidth + 4;

      if (isScrollableX) {
        const parentRect = parent.getBoundingClientRect();
        const elRect = el.getBoundingClientRect();

        const targetScrollLeft = parent.scrollLeft + (elRect.left - parentRect.left) - (parentRect.width / 2) + (elRect.width / 2);
        parent.scrollTo({
          left: Math.max(0, targetScrollLeft),
          behavior: 'smooth'
        });
        break;
      }
      parent = parent.parentElement;
    }

    // 3. Handle vertical scroll containers (modal dialogs, server lists, episode trays) OR main page
    let vContainer: HTMLElement | null = null;
    let curr: HTMLElement | null = el.parentElement;
    while (curr && curr !== document.body && curr !== document.documentElement) {
      const style = window.getComputedStyle(curr);
      const oy = style.overflowY;
      if ((oy === 'auto' || oy === 'scroll') && curr.scrollHeight > curr.clientHeight + 4) {
        vContainer = curr;
        break;
      }
      curr = curr.parentElement;
    }

    const elRect = el.getBoundingClientRect();

    if (vContainer) {
      const cRect = vContainer.getBoundingClientRect();
      const elTopRel = elRect.top - cRect.top;
      const elBottomRel = elRect.bottom - cRect.top;
      const cHeight = cRect.height;
      const padTop = 30;
      const padBottom = 30;

      if (elTopRel < padTop) {
        vContainer.scrollBy({ top: elTopRel - padTop, behavior: 'smooth' });
      } else if (elBottomRel > cHeight - padBottom) {
        vContainer.scrollBy({ top: elBottomRel - cHeight + padBottom, behavior: 'smooth' });
      }
    } else {
      // Main Page / Viewport scroll
      const viewportHeight = window.innerHeight || 540;
      const topSafeMargin = Math.max(65, Math.round(viewportHeight * 0.14));
      const bottomSafeMargin = Math.max(65, Math.round(viewportHeight * 0.14));

      if (elRect.top < topSafeMargin) {
        const scrollOffset = elRect.top - (topSafeMargin + 10);
        this.scrollPageVertical(scrollOffset);
      } else if (elRect.bottom > viewportHeight - bottomSafeMargin) {
        // Place focused item comfortably in primary view zone (38% from top)
        const targetY = Math.round(viewportHeight * 0.38);
        const scrollOffset = elRect.top - targetY;
        this.scrollPageVertical(scrollOffset);
      }
    }
  }

  public clearFocus() {
    if (this.currentFocused) {
      this.currentFocused.classList.remove('tv-focused');
      try {
        this.currentFocused.blur();
      } catch {}
      this.currentFocused = null;
    }
  }

  public focusElement(el: HTMLElement) {
    if (!el) return;

    if (this.currentFocused && this.currentFocused !== el) {
      this.currentFocused.classList.remove('tv-focused');
    }

    this.currentFocused = el;
    el.focus({ preventScroll: true });
    el.classList.add('tv-focused');

    // Update tracking for recovery
    const cardId = el.getAttribute('data-card-id');
    if (cardId) this.lastFocusedCardId = cardId;
    this.lastFocusedClass = el.className;
    this.lastKnownRect = el.getBoundingClientRect();
    this.lastScrollY = window.scrollY || document.documentElement.scrollTop;

    this.smoothScrollIntoTvView(el);
  }

  public focusFirstVisible(scopeContainer?: HTMLElement) {
    const scope = scopeContainer || this.getActiveScopeContainer();
    const candidates = this.getFocusableElements(scope);
    if (candidates.length === 0) return;

    const scrollY = window.scrollY || document.documentElement.scrollTop || this.lastScrollY;

    // When scrolled down into the list (scrollY > 250), NEVER jump back to top hero button!
    // Instead, pick candidate in the visible viewport closest to last known coordinates or viewport center
    if (scrollY > 250) {
      const viewportMidY = (window.innerHeight || 1080) / 2;
      const viewportMidX = (window.innerWidth || 1920) / 2;

      // Filter candidates visible in current viewport
      const inViewport = candidates.filter(c => {
        const r = c.getBoundingClientRect();
        return r.bottom > 20 && r.top < (window.innerHeight || 1080) - 20;
      });

      const pool = inViewport.length > 0 ? inViewport : candidates;

      let bestCandidate = pool[0];
      let minDistance = Infinity;

      for (const el of pool) {
        const r = el.getBoundingClientRect();
        const centerX = (r.left + r.right) / 2;
        const centerY = (r.top + r.bottom) / 2;

        const targetX = this.lastKnownRect ? (this.lastKnownRect.left + this.lastKnownRect.right) / 2 : viewportMidX;
        const targetY = this.lastKnownRect ? (this.lastKnownRect.top + this.lastKnownRect.bottom) / 2 : viewportMidY;

        const dist = Math.hypot(centerX - targetX, centerY - targetY);
        if (dist < minDistance) {
          minDistance = dist;
          bestCandidate = el;
        }
      }

      this.focusElement(bestCandidate);
      return;
    }

    // At top of page, prioritize primary action button (Play / Watch Now) or first card
    const primaryBtn = candidates.find(c =>
      c.classList.contains('btn-play') ||
      c.classList.contains('cta-watch-btn') ||
      c.classList.contains('btn-primary') ||
      c.classList.contains('tv-primary-btn')
    );
    this.focusElement(primaryBtn || candidates[0]);
  }

  public autoFocusInitial() {
    // Only auto-focus on initial load if in TV mode or D-pad is active
    if (!document.documentElement.classList.contains('is-tv-mode') && !this.isDpadActive) {
      return;
    }
    setTimeout(() => {
      if (!this.currentFocused || !document.body.contains(this.currentFocused)) {
        this.focusFirstVisible();
      }
    }, 250);
  }

  private getActiveScopeContainer(): HTMLElement {
    // 1. Open server menu in player takes highest priority for focused D-pad selection
    const openServerMenu = document.querySelector(
      '.theater-server-menu, .player-server-menu'
    ) as HTMLElement | null;
    if (openServerMenu && window.getComputedStyle(openServerMenu).display !== 'none') {
      return openServerMenu;
    }

    // 2. Open episodes menu in player takes next priority
    const openEpisodesMenu = document.querySelector(
      '.theater-episodes-menu'
    ) as HTMLElement | null;
    if (openEpisodesMenu && window.getComputedStyle(openEpisodesMenu).display !== 'none') {
      return openEpisodesMenu;
    }

    // 3. Fullscreen theater player overlay takes next priority
    const theaterOverlay = document.querySelector('.fullscreen-theater-overlay, .fullscreen-theater-modal') as HTMLElement;
    if (theaterOverlay && window.getComputedStyle(theaterOverlay).display !== 'none') {
      return theaterOverlay;
    }

    // 4. Active modal overlays
    const activeModal = document.querySelector('.modal-card, .app-update-modal, .support-modal, .trailer-frame-wrap') as HTMLElement;
    if (activeModal && window.getComputedStyle(activeModal).display !== 'none') {
      return activeModal;
    }

    // 5. Fallback to main document
    return document.body;
  }

  private isElementVisible(el: HTMLElement): boolean {
    if (!el || el.offsetParent === null) return false;
    const style = window.getComputedStyle(el);
    if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') {
      return false;
    }
    const rect = el.getBoundingClientRect();
    return rect.width > 6 && rect.height > 6;
  }

  private getFocusableElements(container: HTMLElement): HTMLElement[] {
    const rawList = Array.from(container.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR));
    return rawList.filter(el => {
      if (el.getAttribute('aria-hidden') === 'true') return false;
      if (el.tabIndex < 0) return false;
      return this.isElementVisible(el);
    });
  }

  /**
   * HTML5 Gamepad API loop for Android TV remotes and controllers
   */
  private startGamepadLoop() {
    if (this.gamepadRafId !== null || typeof window === 'undefined') return;

    const pollGamepad = () => {
      const now = performance.now();
      if (now - this.lastGamepadPress > 180) {
        const gamepads = navigator.getGamepads ? navigator.getGamepads() : [];
        for (let i = 0; i < gamepads.length; i++) {
          const gp = gamepads[i];
          if (!gp) continue;

          // D-pad Directional inputs
          const upPressed = gp.buttons[12]?.pressed || (gp.axes[1] && gp.axes[1] < -0.5);
          const downPressed = gp.buttons[13]?.pressed || (gp.axes[1] && gp.axes[1] > 0.5);
          const leftPressed = gp.buttons[14]?.pressed || (gp.axes[0] && gp.axes[0] < -0.5);
          const rightPressed = gp.buttons[15]?.pressed || (gp.axes[0] && gp.axes[0] > 0.5);

          // Action & Back buttons
          const selectPressed = gp.buttons[0]?.pressed || gp.buttons[2]?.pressed;
          const backPressed = gp.buttons[1]?.pressed || gp.buttons[9]?.pressed;

          if (upPressed) {
            this.activateDpadMode();
            this.navigate('up');
            this.lastGamepadPress = now;
            break;
          } else if (downPressed) {
            this.activateDpadMode();
            this.navigate('down');
            this.lastGamepadPress = now;
            break;
          } else if (leftPressed) {
            this.activateDpadMode();
            this.navigate('left');
            this.lastGamepadPress = now;
            break;
          } else if (rightPressed) {
            this.activateDpadMode();
            this.navigate('right');
            this.lastGamepadPress = now;
            break;
          } else if (selectPressed) {
            this.activateDpadMode();
            this.handleCenterAction();
            this.lastGamepadPress = now;
            break;
          } else if (backPressed) {
            this.handleBackButton();
            this.lastGamepadPress = now;
            break;
          }
        }
      }
      this.gamepadRafId = requestAnimationFrame(pollGamepad);
    };

    this.gamepadRafId = requestAnimationFrame(pollGamepad);
  }
}

export const tvSpatialNav = new TvSpatialNavManager();
if (typeof window !== 'undefined') {
  (window as any).tvSpatialNav = tvSpatialNav;
}
