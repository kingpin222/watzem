import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue';

export interface InfiniteScrollOptions {
  distance?: number;
  threshold?: number;
}

export function useInfiniteScroll(
  onLoadMore: () => Promise<void> | void,
  canLoadMore: () => boolean,
  options: InfiniteScrollOptions = {}
) {
  const sentinelRef = ref<HTMLElement | null>(null);
  const distance = options.distance ?? 350;
  let observer: IntersectionObserver | null = null;
  let isThrottled = false;

  const triggerLoad = () => {
    if (isThrottled) return;
    if (canLoadMore()) {
      isThrottled = true;
      Promise.resolve(onLoadMore()).finally(() => {
        // Small delay to allow DOM to expand before allowing next trigger
        setTimeout(() => {
          isThrottled = false;
        }, 250);
      });
    }
  };

  const handleScroll = () => {
    if (!canLoadMore() || isThrottled) return;
    const scrollHeight = document.documentElement.scrollHeight;
    const scrollTop = window.scrollY || document.documentElement.scrollTop;
    const clientHeight = window.innerHeight || document.documentElement.clientHeight;

    if (scrollTop + clientHeight >= scrollHeight - distance) {
      triggerLoad();
    }
  };

  const initObserver = () => {
    if (observer) {
      observer.disconnect();
      observer = null;
    }

    if (typeof IntersectionObserver !== 'undefined') {
      observer = new IntersectionObserver(
        (entries) => {
          const [entry] = entries;
          if (entry && entry.isIntersecting) {
            triggerLoad();
          }
        },
        {
          root: null,
          rootMargin: `${distance}px`,
          threshold: options.threshold ?? 0.05
        }
      );

      if (sentinelRef.value) {
        observer.observe(sentinelRef.value);
      }
    }
  };

  watch(sentinelRef, (newEl, oldEl) => {
    if (oldEl && observer) {
      observer.unobserve(oldEl);
    }
    if (newEl && observer) {
      observer.observe(newEl);
    }
  });

  onMounted(() => {
    nextTick(() => {
      initObserver();
    });
    window.addEventListener('scroll', handleScroll, { passive: true });
  });

  onUnmounted(() => {
    if (observer) {
      observer.disconnect();
      observer = null;
    }
    window.removeEventListener('scroll', handleScroll);
  });

  return {
    sentinelRef,
    triggerLoad
  };
}
