/**
 * WATZEM Android TV & D-Pad Spatial Navigation Engine
 * Provides fluid 10-foot 2D directional navigation for Android TV remotes,
 * Google TV remotes, Fire TV remotes, and game controllers.
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
  '.nav-tab',
  '.tv-nav-item',
  '.tv-card',
  '.btn-play',
  '.btn-details',
  '.btn-bookmark',
  '.theater-server-btn',
  '.theater-next-btn',
  '.theater-episodes-btn',
  '.floating-theater-exit-btn',
  '.floating-theater-pip-btn',
  '.theater-ep-pill',
  '.menu-server-item',
  '[data-tv-focusable]'
].join(', ');

class TvSpatialNavManager {
  private currentFocused: HTMLElement | null = null;
  private isDpadActive = false;
  private isInitialized = false;
  private gamepadRafId: number | null = null;
  private lastGamepadPress = 0;
  private onBackCallbacks: Array<() => boolean> = [];

  public init() {
    if (this.isInitialized || typeof window === 'undefined') return;
    this.isInitialized = true;

    // Listen to keydown events for Arrow keys and Android TV keycodes
    window.addEventListener('keydown', this.handleKeyDown.bind(this), { capture: true });

    // Detect mouse/pointer usage to optionally soften D-pad highlight
    window.addEventListener('pointerdown', () => {
      this.isDpadActive = false;
      document.documentElement.classList.remove('is-dpad-active');
    }, { passive: true });

    // Start Gamepad API loop for TV remotes and Bluetooth controllers
    this.startGamepadLoop();

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
    const isEnter = key === 'Enter' || key === ' ' || keyCode === 23 || keyCode === 13 || keyCode === 66;
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
      this.activateDpadMode();
      if (this.currentFocused && document.activeElement !== this.currentFocused) {
        this.currentFocused.focus();
      }
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

  public handleBackButton(): boolean {
    // 1. Run custom back handlers (player, modal, etc.)
    for (let i = this.onBackCallbacks.length - 1; i >= 0; i--) {
      try {
        if (this.onBackCallbacks[i]()) {
          return true;
        }
      } catch {}
    }

    // 2. Check for active theater player
    const theaterOverlay = document.querySelector('.fullscreen-theater-overlay') as HTMLElement;
    if (theaterOverlay && window.getComputedStyle(theaterOverlay).display !== 'none') {
      const exitBtn = theaterOverlay.querySelector('.floating-theater-exit-btn, .close-btn') as HTMLElement;
      if (exitBtn) {
        exitBtn.click();
        return true;
      }
    }

    // 3. Check for open modals
    const modalCloseBtn = document.querySelector('.modal-card .close-btn, .app-update-modal .btn-secondary, .support-modal .btn-secondary') as HTMLElement;
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

    return false;
  }

  public navigate(dir: Direction) {
    const scope = this.getActiveScopeContainer();
    const candidates = this.getFocusableElements(scope);

    if (candidates.length === 0) return;

    if (!this.currentFocused || !document.body.contains(this.currentFocused) || !this.isElementVisible(this.currentFocused)) {
      this.focusFirstVisible(scope);
      return;
    }

    const currentRect = this.currentFocused.getBoundingClientRect();
    const nextEl = this.findBestCandidate(currentRect, candidates, dir);

    if (nextEl) {
      this.focusElement(nextEl);
    }
  }

  private findBestCandidate(currentRect: DOMRect, candidates: HTMLElement[], dir: Direction): HTMLElement | null {
    let bestCandidate: HTMLElement | null = null;
    let minScore = Infinity;

    const curCenterX = currentRect.left + currentRect.width / 2;
    const curCenterY = currentRect.top + currentRect.height / 2;

    for (const el of candidates) {
      if (el === this.currentFocused) continue;

      const rect = el.getBoundingClientRect();
      const elCenterX = rect.left + rect.width / 2;
      const elCenterY = rect.top + rect.height / 2;

      let primaryDist = 0;
      let secondaryDist = 0;
      let isValidDir = false;

      // Allow 4px margin of tolerance for elements in same row/column
      const margin = 4;

      switch (dir) {
        case 'up':
          isValidDir = rect.bottom <= currentRect.top + margin && elCenterY < curCenterY;
          primaryDist = curCenterY - elCenterY;
          secondaryDist = Math.abs(curCenterX - elCenterX);
          break;
        case 'down':
          isValidDir = rect.top >= currentRect.bottom - margin && elCenterY > curCenterY;
          primaryDist = elCenterY - curCenterY;
          secondaryDist = Math.abs(curCenterX - elCenterX);
          break;
        case 'left':
          isValidDir = rect.right <= currentRect.left + margin && elCenterX < curCenterX;
          primaryDist = curCenterX - elCenterX;
          secondaryDist = Math.abs(curCenterY - elCenterY);
          break;
        case 'right':
          isValidDir = rect.left >= currentRect.right - margin && elCenterX > curCenterX;
          primaryDist = elCenterX - curCenterX;
          secondaryDist = Math.abs(curCenterY - elCenterY);
          break;
      }

      if (!isValidDir) continue;

      // Primary axis distance has weight 1.0, secondary has weight 2.6 to prevent erratic jumping
      const score = primaryDist + (secondaryDist * 2.6);

      if (score < minScore) {
        minScore = score;
        bestCandidate = el;
      }
    }

    return bestCandidate;
  }

  public focusElement(el: HTMLElement) {
    if (!el) return;

    if (this.currentFocused && this.currentFocused !== el) {
      this.currentFocused.classList.remove('tv-focused');
    }

    this.currentFocused = el;
    el.focus({ preventScroll: true });
    el.classList.add('tv-focused');

    // Smooth scroll into visible view
    try {
      el.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest',
        inline: 'center'
      });
    } catch {}
  }

  public focusFirstVisible(scopeContainer?: HTMLElement) {
    const scope = scopeContainer || this.getActiveScopeContainer();
    const candidates = this.getFocusableElements(scope);
    if (candidates.length > 0) {
      // Prioritize primary action button (Play / Watch Now) or first card
      const primaryBtn = candidates.find(c =>
        c.classList.contains('btn-play') ||
        c.classList.contains('btn-primary') ||
        c.classList.contains('tv-primary-btn')
      );
      this.focusElement(primaryBtn || candidates[0]);
    }
  }

  public autoFocusInitial() {
    setTimeout(() => {
      if (!this.currentFocused || !document.body.contains(this.currentFocused)) {
        this.focusFirstVisible();
      }
    }, 250);
  }

  private getActiveScopeContainer(): HTMLElement {
    // 1. Fullscreen theater player overlay takes highest priority
    const theaterOverlay = document.querySelector('.fullscreen-theater-overlay') as HTMLElement;
    if (theaterOverlay && window.getComputedStyle(theaterOverlay).display !== 'none') {
      return theaterOverlay;
    }

    // 2. Active modal overlays
    const activeModal = document.querySelector('.modal-card, .app-update-modal, .support-modal') as HTMLElement;
    if (activeModal && window.getComputedStyle(activeModal).display !== 'none') {
      return activeModal;
    }

    // 3. Fallback to main document
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
      if (now - this.lastGamepadPress > 170) {
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
            if (this.currentFocused) {
              this.currentFocused.click();
            }
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
