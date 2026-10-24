import type { CapacitorConfig } from '@capacitor/cli';

const config: CapacitorConfig = {
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

