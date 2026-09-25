import { Haptics, ImpactStyle, NotificationType } from '@capacitor/haptics';
import { Clipboard } from '@capacitor/clipboard';

export function useHaptics() {
  const triggerLight = async () => {
    try {
      await Haptics.impact({ style: ImpactStyle.Light });
    } catch {
      // Browser fallback (if navigator.vibrate is supported)
      if (typeof navigator !== 'undefined' && navigator.vibrate) {
        navigator.vibrate(10);
      }
    }
  };

  const triggerMedium = async () => {
    try {
      await Haptics.impact({ style: ImpactStyle.Medium });
    } catch {
      if (typeof navigator !== 'undefined' && navigator.vibrate) {
        navigator.vibrate(25);
      }
    }
  };

  const triggerSuccess = async () => {
    try {
      await Haptics.notification({ type: NotificationType.Success });
    } catch {
      if (typeof navigator !== 'undefined' && navigator.vibrate) {
        navigator.vibrate([15, 30, 20]);
      }
    }
  };

  const copyText = async (text: string): Promise<boolean> => {
    try {
      await Clipboard.write({ string: text });
      await triggerSuccess();
      return true;
    } catch {
      try {
        if (navigator.clipboard) {
          await navigator.clipboard.writeText(text);
          await triggerSuccess();
          return true;
        }
        const ta = document.createElement('textarea');
        ta.value = text;
        ta.style.position = 'fixed';
        ta.style.opacity = '0';
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
        await triggerSuccess();
        return true;
      } catch (err) {
        console.error('Failed to copy to clipboard', err);
        return false;
      }
    }
  };

  return {
    triggerLight,
    triggerMedium,
    triggerSuccess,
    copyText
  };
}
