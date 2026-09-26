/**
 * WATZEM Spatial Navigation Engine for Android TV, Google TV, and Remote Controls
 * Provides 2D geometric D-pad navigation, smart candidate selection, auto-scroll,
 * Gamepad remote polling, and visual focus highlighting.
 */

type Direction = 'up' | 'down' | 'left' | 'right';

const FOCUSABLE_SELECTOR = [
  'button:not([disabled])',
  'a[href]',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex="0"]',
  '.movie-card',
  '.nav-tab',
  '.pill-btn',
  '.circle-nav-btn',
  '.btn-play',
  '.btn-details',
  '.btn-bookmark',
  '.hero-nav-arrow',
  '.theater-server-btn',
  '.theater-next-btn',
  '.theater-episodes-btn',
  '.floating-theater-exit-btn',
  '.floating-theater-pip-btn',
  '.theater-ep-pill',
  '.menu-server-item',
  '.ep-pill-btn',
  '.cast-card'
].join(', ');

class SpatialNavManager {
  private isInitialized = false;
  private currentFocused: HTMLElement | null = null;
  private isDpadActive = false;
  private isTv = false;
  private lastGamepadPress = 0;
  private gamepadRafId: number | null = null;

  public init() {
    if (this.isInitialized || typeof window === 'undefined') return;
    this.isInitialized = true;

    // Detect Android TV via native flag or user agent
    this.detectTvEnvironment();

    // Listen for D-pad keyboard / remote events
    window.addEventListener('keydown', this.handleKeyDown.bind(this), true);

    // Track pointer vs remote usage
    window.addEventListener('pointerdown', this.handlePointerDown.bind(this), { passive: true });
    window.addEventListener('focusin', this.handleFocusIn.bind(this), true);

    // Initialize Gamepad API polling for Android TV remotes
    window.addEventListener('gamepadconnected', () => {
      this.startGamepadLoop();
    });

    // Watch for route or view changes to auto-focus primary elements on TV
    window.addEventListener('popstate', () => {
      if (this.isTv || this.isDpadActive) {
        setTimeout(() => this.focusFirstVisible(), 250);
      }
    });

    this.startGamepadLoop();

    // If TV mode detected, auto-focus initial hero button
    if (this.isTv) {
      setTimeout(() => this.focusFirstVisible(), 600);
    }
  }

  public isTvDevice(): boolean {
    return this.isTv;
  }

  private detectTvEnvironment() {
    const isNativeTv = Boolean((window as any).__IS_ANDROID_TV__);
    const ua = navigator.userAgent || '';
    const isTvUa = /android.*(tv|box)|googletv|smarttv|mibox|firetv|appletv/i.test(ua);
    const hasLeanback = document.documentElement.classList.contains('is-tv-mode');

    this.isTv = isNativeTv || isTvUa || hasLeanback;

    if (this.isTv) {
      document.documentElement.classList.add('is-tv-mode');
      this.isDpadActive = true;
      document.documentElement.classList.add('is-dpad-active');
    }
  }

  private handlePointerDown() {
    // If user touches/clicks with mouse/finger, remove D-pad focus highlight
    if (!this.isTv) {
      this.isDpadActive = false;
      document.documentElement.classList.remove('is-dpad-active');
      if (this.currentFocused) {
        this.currentFocused.classList.remove('tv-focused');
      }
    }
  }

  private handleFocusIn(e: FocusEvent) {
    const target = e.target as HTMLElement | null;
    if (target && target !== document.body) {
      if (this.currentFocused && this.currentFocused !== target) {
        this.currentFocused.classList.remove('tv-focused');
      }
      this.currentFocused = target;
      if (this.isDpadActive || this.isTv) {
        target.classList.add('tv-focused');
      }
    }
  }

  private handleKeyDown(e: KeyboardEvent) {
    const key = e.key;
    const keyCode = e.keyCode;

    // DPAD KeyCodes & Keys
    // Up: ArrowUp, 38, 19
    // Down: ArrowDown, 40, 20
    // Left: ArrowLeft, 37, 21
    // Right: ArrowRight, 39, 22
    // Center/Enter: Enter, ' ', 13, 23
    // Back: Escape, 27, 4, 8

    let direction: Direction | null = null;

    if (key === 'ArrowUp' || keyCode === 38 || keyCode === 19) {
      direction = 'up';
    } else if (key === 'ArrowDown' || keyCode === 40 || keyCode === 20) {
      direction = 'down';
    } else if (key === 'ArrowLeft' || keyCode === 37 || keyCode === 21) {
      direction = 'left';
    } else if (key === 'ArrowRight' || keyCode === 39 || keyCode === 22) {
      direction = 'right';
    }

    if (direction) {
      this.activateDpadMode();
      const handled = this.navigate(direction);
      if (handled) {
        e.preventDefault();
        e.stopPropagation();
      }
      return;
    }

    // Handle OK / Center button
    if (key === 'Enter' || key === ' ' || keyCode === 13 || keyCode === 23) {
      this.activateDpadMode();
      if (this.currentFocused) {
        // Trigger click if element is not a standard input
        if (this.currentFocused.tagName !== 'INPUT' && this.currentFocused.tagName !== 'TEXTAREA') {
          e.preventDefault();
          this.currentFocused.click();
        }
      }
      return;
    }

    // Handle Back / Escape button
    if (key === 'Escape' || keyCode === 27 || keyCode === 4) {
      this.handleBackButton(e);
    }
  }

  public activateDpadMode() {
    if (!this.isDpadActive) {
      this.isDpadActive = true;
      document.documentElement.classList.add('is-dpad-active');
    }
  }

  private handleBackButton(e?: Event) {
    // 1. Check if dropdown menu is open (Episodes or Server menu)
    const serverMenu = document.querySelector('.theater-server-menu, .player-server-menu') as HTMLElement;
    if (serverMenu && window.getComputedStyle(serverMenu).display !== 'none') {
      const serverBtn = document.querySelector('.theater-server-btn, .player-server-btn') as HTMLElement;
      if (serverBtn) serverBtn.click();
      if (e) e.preventDefault();
      return;
    }

    const episodesMenu = document.querySelector('.theater-episodes-menu') as HTMLElement;
    if (episodesMenu && window.getComputedStyle(episodesMenu).display !== 'none') {
      const epBtn = document.querySelector('.theater-episodes-btn') as HTMLElement;
      if (epBtn) epBtn.click();
      if (e) e.preventDefault();
      return;
    }

    // 2. Check if a generic modal is open (Update, Support, etc.)
    const closeBtn = document.querySelector('.modal-close-btn, .close-modal-btn, .btn-dismiss') as HTMLElement;
    if (closeBtn && window.getComputedStyle(closeBtn).display !== 'none') {
      closeBtn.click();
      if (e) e.preventDefault();
      return;
    }

    // 3. Check if fullscreen player is active
    const exitPlayerBtn = document.querySelector('.floating-theater-exit-btn') as HTMLElement;
    if (exitPlayerBtn && window.getComputedStyle(exitPlayerBtn).display !== 'none') {
      exitPlayerBtn.click();
      if (e) e.preventDefault();
      return;
    }

    // 4. Check if MovieDetailView back button is visible
    const backBtn = document.querySelector('.circle-nav-btn') as HTMLElement;
    if (backBtn && window.getComputedStyle(backBtn).display !== 'none') {
      backBtn.click();
      if (e) e.preventDefault();
      return;
    }

    // 5. If on another tab, return to Home
    const homeTab = document.querySelector('.nav-tab:first-child') as HTMLElement;
    const activeTab = document.querySelector('.nav-tab.active') as HTMLElement;
    if (homeTab && activeTab && activeTab !== homeTab) {
      homeTab.click();
      if (e) e.preventDefault();
      return;
    }
  }

  public navigate(direction: Direction): boolean {
    const scope = this.getActiveScopeContainer();
    const candidates = this.getFocusableElements(scope);

    if (candidates.length === 0) return false;

    // If nothing currently focused, pick the best initial target
    if (!this.currentFocused || !document.contains(this.currentFocused) || !candidates.includes(this.currentFocused)) {
      this.focusElement(candidates[0]);
      return true;
    }

    const currentRect = this.currentFocused.getBoundingClientRect();
    const currentCenter = {
      x: currentRect.left + currentRect.width / 2,
      y: currentRect.top + currentRect.height / 2
    };

    let bestCandidate: HTMLElement | null = null;
    let minScore = Infinity;

    for (const cand of candidates) {
      if (cand === this.currentFocused) continue;

      const candRect = cand.getBoundingClientRect();
      const candCenter = {
        x: candRect.left + candRect.width / 2,
        y: candRect.top + candRect.height / 2
      };

      const dx = candCenter.x - currentCenter.x;
      const dy = candCenter.y - currentCenter.y;

      let isEligible = false;
      let primaryDist = 0;
      let orthogonalDist = 0;
      let hasOverlap = false;

      switch (direction) {
        case 'right':
          isEligible = candRect.left >= currentRect.left + 2 && dx > 0;
          primaryDist = dx;
          orthogonalDist = Math.abs(dy);
          hasOverlap = candRect.top < currentRect.bottom && candRect.bottom > currentRect.top;
          break;

        case 'left':
          isEligible = candRect.right <= currentRect.right - 2 && dx < 0;
          primaryDist = Math.abs(dx);
          orthogonalDist = Math.abs(dy);
          hasOverlap = candRect.top < currentRect.bottom && candRect.bottom > currentRect.top;
          break;

        case 'down':
          isEligible = candRect.top >= currentRect.top + 2 && dy > 0;
          primaryDist = dy;
          orthogonalDist = Math.abs(dx);
          hasOverlap = candRect.left < currentRect.right && candRect.right > currentRect.left;
          break;

        case 'up':
          isEligible = candRect.bottom <= currentRect.bottom - 2 && dy < 0;
          primaryDist = Math.abs(dy);
          orthogonalDist = Math.abs(dx);
          hasOverlap = candRect.left < currentRect.right && candRect.right > currentRect.left;
          break;
      }

      if (isEligible) {
        // Spatial score formula: heavily penalize distance in orthogonal axis, reward direct visual line-of-sight
        let score = primaryDist + orthogonalDist * 2.2;
        if (hasOverlap) {
          score -= 15;
        }

        if (score < minScore) {
          minScore = score;
          bestCandidate = cand;
        }
      }
    }

    if (bestCandidate) {
      this.focusElement(bestCandidate);
      return true;
    }

    return false;
  }

  public focusElement(el: HTMLElement) {
    if (this.currentFocused && this.currentFocused !== el) {
      this.currentFocused.classList.remove('tv-focused');
    }

    this.currentFocused = el;
    el.focus({ preventScroll: true });
    el.classList.add('tv-focused');

    // Smooth scroll into visible view with padding margin
    try {
      el.scrollIntoView({
        behavior: 'smooth',
        block: 'nearest',
        inline: 'nearest'
      });
    } catch {}
  }

  public focusFirstVisible() {
    const scope = this.getActiveScopeContainer();
    const candidates = this.getFocusableElements(scope);
    if (candidates.length > 0) {
      // Prioritize Watch Now or Play button if visible
      const playBtn = candidates.find(c => c.classList.contains('btn-play') || c.classList.contains('btn-primary'));
      this.focusElement(playBtn || candidates[0]);
    }
  }

  private getActiveScopeContainer(): HTMLElement {
    // 1. Fullscreen theater player has priority
    const theaterOverlay = document.querySelector('.fullscreen-theater-overlay') as HTMLElement;
    if (theaterOverlay && window.getComputedStyle(theaterOverlay).display !== 'none') {
      return theaterOverlay;
    }

    // 2. Active modal overlays
    const activeModal = document.querySelector('.modal-card, .app-update-modal, .support-modal') as HTMLElement;
    if (activeModal && window.getComputedStyle(activeModal).display !== 'none') {
      return activeModal;
    }

    // 3. Fallback to main document body
    return document.body;
  }

  private getFocusableElements(container: HTMLElement): HTMLElement[] {
    const rawList = Array.from(container.querySelectorAll<HTMLElement>(FOCUSABLE_SELECTOR));
    return rawList.filter(el => {
      if (el.getAttribute('aria-hidden') === 'true') return false;
      if (el.tabIndex < 0) return false;

      const style = window.getComputedStyle(el);
      if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') {
        return false;
      }

      const rect = el.getBoundingClientRect();
      return rect.width > 6 && rect.height > 6;
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

          // D-pad Buttons
          const upPressed = gp.buttons[12]?.pressed || (gp.axes[1] && gp.axes[1] < -0.5);
          const downPressed = gp.buttons[13]?.pressed || (gp.axes[1] && gp.axes[1] > 0.5);
          const leftPressed = gp.buttons[14]?.pressed || (gp.axes[0] && gp.axes[0] < -0.5);
          const rightPressed = gp.buttons[15]?.pressed || (gp.axes[0] && gp.axes[0] > 0.5);

          // Action Buttons: Button 0 (A/Select), Button 1 (B/Back)
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

export const spatialNav = new SpatialNavManager();
