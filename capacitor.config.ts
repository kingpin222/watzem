import type { CapacitorConfig } from '@capacitor/cli';

const isTv = process.env.BUILD_TARGET === 'tv' || process.env.CAP_TARGET === 'tv';

const config: CapacitorConfig = isTv ? {
  appId: 'com.watzem.tv',
  appName: 'WATZEM TV',
  webDir: 'dist',
  android: {
    path: 'android-tv'
  },
  server: {
    androidScheme: 'https'
  },
  plugins: {
    StatusBar: {
      style: 'DARK',
      backgroundColor: '#07070a'
    }
  }
} : {
  appId: 'com.watzem.app',
  appName: 'WATZEM',
  webDir: 'dist',
  android: {
    path: 'android'
  },
  server: {
    androidScheme: 'https'
  },
  plugins: {
    StatusBar: {
      style: 'DARK',
      backgroundColor: '#07070a'
    }
  }
};

export default config;
