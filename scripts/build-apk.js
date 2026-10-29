import { execSync } from 'child_process';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { uploadRelease } from './upload-release.js';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '..');

function run(command, cwd = projectRoot) {
  console.log(`\n▶️ Executing: ${command}`);
  execSync(command, { cwd, stdio: 'inherit' });
}

console.log('🚀 ========================================');
console.log('🚀 STARTING FULL APK BUILD WITH AUTO-BUMP');
console.log('🚀 ========================================\n');

// Helper to clean any leaked APK files from web and android assets
function cleanAssetApks() {
  const targets = [
    path.join(projectRoot, 'dist/install.apk'),
    path.join(projectRoot, 'android/app/src/main/assets/public/install.apk')
  ];
  for (const t of targets) {
    if (fs.existsSync(t)) {
      fs.unlinkSync(t);
      console.log(`🧹 Cleaned nested APK from: ${path.relative(projectRoot, t)}`);
    }
  }

  // Also scan android/app/src/main/assets/public for any *.apk
  const androidAssetsDir = path.join(projectRoot, 'android/app/src/main/assets/public');
  if (fs.existsSync(androidAssetsDir)) {
    const files = fs.readdirSync(androidAssetsDir);
    for (const file of files) {
      if (file.endsWith('.apk')) {
        fs.unlinkSync(path.join(androidAssetsDir, file));
        console.log(`🧹 Removed APK artifact from Android assets: ${file}`);
      }
    }
  }
}

// 0. Pre-clean to avoid bundling old APK into dist
cleanAssetApks();

// 1. Build web assets & compile server (triggers prebuild auto-bump)
run('npm run build');

// 2. Ensure dist doesn't have an APK before sync
cleanAssetApks();

// 3. Sync web assets and configs to native Android project
run('npx cap sync android');

// 3.1 Guarantee no APK exists inside Android assets before compiling
cleanAssetApks();

// 4. Build Native APK with Gradle
const nativeAndroidDir = path.join(projectRoot, 'watzem-native-android');
console.log('\n🔨 Compiling Native Android APK (Jetpack Compose) with Gradle...');

let builtApkPath = null;
try {
  run('./gradlew assembleRelease', nativeAndroidDir);
  const releaseApk = path.join(nativeAndroidDir, 'app/build/outputs/apk/release/app-release.apk');
  if (fs.existsSync(releaseApk)) {
    builtApkPath = releaseApk;
  }
} catch (err) {
  console.warn('⚠️ assembleRelease failed, trying assembleDebug as fallback...');
  run('./gradlew assembleDebug', nativeAndroidDir);
  const debugApk = path.join(nativeAndroidDir, 'app/build/outputs/apk/debug/app-debug.apk');
  if (fs.existsSync(debugApk)) {
    builtApkPath = debugApk;
  }
}

if (!builtApkPath || !fs.existsSync(builtApkPath)) {
  console.error('❌ Failed to locate compiled APK!');
  process.exit(1);
}

// 5. Copy compiled APK to public/install.apk
const publicApkPath = path.join(projectRoot, 'public/install.apk');
fs.copyFileSync(builtApkPath, publicApkPath);

// Also copy to client dist if it exists
const distApkPath = path.join(projectRoot, 'dist/install.apk');
if (fs.existsSync(path.dirname(distApkPath))) {
  fs.copyFileSync(builtApkPath, distApkPath);
}

const stats = fs.statSync(publicApkPath);
const sizeMB = (stats.size / (1024 * 1024)).toFixed(2);

// Read updated version
const versionData = JSON.parse(fs.readFileSync(path.join(projectRoot, 'version.json'), 'utf-8'));

console.log('\n🎉 ========================================');
console.log(`✅ Local APK Build Complete! Version: v${versionData.version} (Build: ${versionData.versionCode})`);
console.log(`📦 Installed to public/install.apk (${sizeMB} MB)`);
console.log('🎉 ========================================\n');

// 6. Direct GitHub Release upload
await uploadRelease();

