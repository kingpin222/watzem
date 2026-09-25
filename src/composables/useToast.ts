import { ref } from 'vue';

export interface ToastMessage {
  id: string;
  title: string;
  message?: string;
  type?: 'success' | 'info' | 'warning' | 'error';
  duration?: number;
}

const toasts = ref<ToastMessage[]>([]);

export function useToast() {
  const showToast = (toast: Omit<ToastMessage, 'id'>) => {
    const id = Math.random().toString(36).substring(2, 9);
    const duration = toast.duration ?? 3500;
    const newToast: ToastMessage = {
      ...toast,
      id,
      duration
    };

    toasts.value.push(newToast);

    if (duration > 0) {
      setTimeout(() => {
        dismissToast(id);
      }, duration);
    }

    return id;
  };

  const dismissToast = (id: string) => {
    toasts.value = toasts.value.filter((t) => t.id !== id);
  };

  return {
    toasts,
    showToast,
    dismissToast
  };
}
