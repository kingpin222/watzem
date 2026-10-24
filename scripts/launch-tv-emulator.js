import { execSync, spawn } from 'child_process';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '..');

const ANDROID_HOME = process.env.ANDROID_HOME || '/Users/jericopascual/Library/Android/sdk';
const ADB = path.join(ANDROID_HOME, 'platform-tools/adb');
const EMULATOR = path.join(ANDROID_HOME, 'emulator/emulator');

function run(command, silent = false) {
  try {
    return execSync(command, { encoding: 'utf-8', stdio: silent ? 'pipe' : 'inherit' });
  } catch (err) {
    if (!silent) console.error(err);
    return null;
  }
}

function runOutput(command) {
  try {
    return execSync(command, { encoding: 'utf-8', stdio: 'pipe' }).trim();
  } catch {
    return '';
  }
}

async function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

async function main() {
  console.log('⚡ ========================================');
  console.log('⚡ OPTIMIZED ANDROID TV LAUNCHER');
  console.log('⚡ ========================================\n');

  // 1. Check if emulator is already running
  const devicesOutput = runOutput(`"${ADB}" devices`);
  const isRunning = devicesOutput.split('\n').some((l) => l.includes('emulator-') && l.includes('device'));

  if (!isRunning) {
    console.log('🚀 Starting Television_4K with auto GPU profile...');
    const child = spawn(
      EMULATOR,
      ['-avd', 'Television_4K', '-gpu', 'auto', '-no-audio', '-no-boot-anim', '-accel', 'on'],
      { detached: true, stdio: 'ignore' }
    );
    child.unref();

    console.log('⏳ Waiting for Android TV system to complete boot...');
    let booted = false;
    for (let i = 0; i < 40; i++) {
      await sleep(1500);
      const bootCompleted = runOutput(`"${ADB}" shell getprop sys.boot_completed`);
      if (bootCompleted === '1') {
        booted = true;
        break;
      }
    }
    if (!booted) {
      console.warn('⚠️ Boot check timed out, proceeding anyway...');
    } else {
      console.log('✅ Android TV booted successfully!');
    }
  } else {
    console.log('📱 Android TV emulator is already running.');
  }

  // 2. Synchronize date & time (prevents SSL/TMDB cert expiration issues)
  const epochMs = Date.now();
  runOutput(`"${ADB}" shell "cmd alarm set-time ${epochMs}"`);
  console.log('🕒 System clock synchronized to host time.');

  // 3. Set snappy TV animations (0.5x speed)
  runOutput(`"${ADB}" shell settings put global window_animation_scale 0.5`);
  runOutput(`"${ADB}" shell settings put global transition_animation_scale 0.5`);
  runOutput(`"${ADB}" shell settings put global animator_duration_scale 0.5`);

  // 4. Install / Update Universal APK if present
  const apkCandidate = [
    path.join(projectRoot, 'android/app/build/outputs/apk/release/app-release.apk'),
    path.join(projectRoot, 'public/install.apk')
  ].find((p) => fs.existsSync(p));

  if (apkCandidate) {
    console.log(`📦 Installing latest APK from ${path.basename(apkCandidate)}...`);
    try {
      execSync(`"${ADB}" install -r "${apkCandidate}"`, { stdio: 'inherit' });
    } catch {
      console.log('🔄 Reinstalling clean APK package...');
      runOutput(`"${ADB}" uninstall com.watzem.app`);
      execSync(`"${ADB}" install "${apkCandidate}"`, { stdio: 'inherit' });
    }
  }

  // 5. Launch TV Main Activity
  console.log('\n📺 Launching WATZEM on Android TV...');
  runOutput(`"${ADB}" shell am start -n com.watzem.app/com.watzem.app.MainActivity`);
  console.log('🎉 WATZEM is now running on Android TV emulator!\n');
}

main().catch(console.error);
