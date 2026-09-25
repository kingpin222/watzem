import { ref } from 'vue';

const activeHeroBackdrop = ref<string>('');
const isHeroActive = ref<boolean>(false);

export function useHeroBackground() {
  const setHeroBackdrop = (url: string) => {
    activeHeroBackdrop.value = url;
    isHeroActive.value = Boolean(url);
  };

  const clearHeroBackdrop = () => {
    activeHeroBackdrop.value = '';
    isHeroActive.value = false;
  };

  return {
    activeHeroBackdrop,
    isHeroActive,
    setHeroBackdrop,
    clearHeroBackdrop
  };
}
