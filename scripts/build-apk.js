import { execSync } from 'child_process';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

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

// 1. Build web assets & compile server (triggers prebuild auto-bump)
run('npm run build');

// 3. Sync web assets and configs to native Android project
run('npx cap sync android');

// 4. Build APK with Gradle
const androidDir = path.join(projectRoot, 'android');
console.log('\n🔨 Compiling Android APK with Gradle...');

let builtApkPath = null;
try {
  run('./gradlew assembleRelease', androidDir);
  const releaseApk = path.join(androidDir, 'app/build/outputs/apk/release/app-release.apk');
  if (fs.existsSync(releaseApk)) {
    builtApkPath = releaseApk;
  }
} catch (err) {
  console.warn('⚠️ assembleRelease failed, trying assembleDebug as fallback...');
  run('./gradlew assembleDebug', androidDir);
  const debugApk = path.join(androidDir, 'app/build/outputs/apk/debug/app-debug.apk');
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
console.log(`✅ APK Build Complete! Version: v${versionData.version} (Build: ${versionData.versionCode})`);
console.log(`📦 Installed to public/install.apk (${sizeMB} MB)`);
console.log('🚀 All users will now receive the update prompt on next launch!');
console.log('🎉 ========================================\n');
