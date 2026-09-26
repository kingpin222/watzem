import { execSync } from 'child_process';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '..');

function run(command, cwd = projectRoot, env = {}) {
  console.log(`\n▶️ Executing: ${command}`);
  execSync(command, { cwd, stdio: 'inherit', env: { ...process.env, ...env } });
}

console.log('📺 ========================================');
console.log('📺 STARTING ANDROID TV APK BUILD (STANDALONE)');
console.log('📺 ========================================\n');

// 0. Helper to clean any leaked APK files from web and android-tv assets
function cleanAssetApks() {
  const targets = [
    path.join(projectRoot, 'dist/watzem-tv.apk'),
    path.join(projectRoot, 'android-tv/app/src/main/assets/public/watzem-tv.apk'),
    path.join(projectRoot, 'android-tv/app/src/main/assets/public/install.apk')
  ];
  for (const t of targets) {
    if (fs.existsSync(t)) {
      fs.unlinkSync(t);
      console.log(`🧹 Cleaned APK from: ${path.relative(projectRoot, t)}`);
    }
  }

  const tvAssetsDir = path.join(projectRoot, 'android-tv/app/src/main/assets/public');
  if (fs.existsSync(tvAssetsDir)) {
    const files = fs.readdirSync(tvAssetsDir);
    for (const file of files) {
      if (file.endsWith('.apk')) {
        fs.unlinkSync(path.join(tvAssetsDir, file));
        console.log(`🧹 Removed APK artifact from Android TV assets: ${file}`);
      }
    }
  }
}

cleanAssetApks();

// 1. Build web assets & compile server
run('npm run build');

cleanAssetApks();

// 2. Sync web assets directly to native Android TV project (android-tv)
run('npx cap sync android', projectRoot, { BUILD_TARGET: 'tv' });

cleanAssetApks();

// 3. Build APK with Gradle in android-tv directory
const androidTvDir = path.join(projectRoot, 'android-tv');
console.log('\n🔨 Compiling Android TV APK with Gradle in android-tv/...');

let builtApkPath = null;
try {
  run('./gradlew assembleRelease', androidTvDir);
  const releaseApk = path.join(androidTvDir, 'app/build/outputs/apk/release/app-release.apk');
  if (fs.existsSync(releaseApk)) {
    builtApkPath = releaseApk;
  }
} catch (err) {
  console.warn('⚠️ assembleRelease failed, trying assembleDebug as fallback...');
  run('./gradlew assembleDebug', androidTvDir);
  const debugApk = path.join(androidTvDir, 'app/build/outputs/apk/debug/app-debug.apk');
  if (fs.existsSync(debugApk)) {
    builtApkPath = debugApk;
  }
}

if (!builtApkPath || !fs.existsSync(builtApkPath)) {
  console.error('❌ Failed to locate compiled Android TV APK!');
  process.exit(1);
}

// 4. Copy compiled APK to public/watzem-tv.apk
const publicApkPath = path.join(projectRoot, 'public/watzem-tv.apk');
fs.copyFileSync(builtApkPath, publicApkPath);

// Also copy to client dist if it exists
const distApkPath = path.join(projectRoot, 'dist/watzem-tv.apk');
if (fs.existsSync(path.dirname(distApkPath))) {
  fs.copyFileSync(builtApkPath, distApkPath);
}

const stats = fs.statSync(publicApkPath);
const sizeMB = (stats.size / (1024 * 1024)).toFixed(2);

// Read current version
const versionData = JSON.parse(fs.readFileSync(path.join(projectRoot, 'version.json'), 'utf-8'));

console.log('\n🎉 ========================================');
console.log(`✅ Android TV APK Build Complete! Version: v${versionData.version} (Build: ${versionData.versionCode})`);
console.log(`📦 Installed to public/watzem-tv.apk (${sizeMB} MB)`);
console.log(`📺 Target Package: com.watzem.tv (Leanback Launcher Ready)`);
console.log('🎉 ========================================\n');
