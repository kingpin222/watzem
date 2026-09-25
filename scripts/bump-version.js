import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '..');

const versionJsonPath = path.join(projectRoot, 'version.json');
const packageJsonPath = path.join(projectRoot, 'package.json');
const gradlePath = path.join(projectRoot, 'android/app/build.gradle');

// 1. Read version.json
if (!fs.existsSync(versionJsonPath)) {
  console.error('❌ version.json not found at:', versionJsonPath);
  process.exit(1);
}

const versionData = JSON.parse(fs.readFileSync(versionJsonPath, 'utf-8'));

// 2. Increment versionCode and version
const oldCode = Number(versionData.versionCode) || 1;
const newCode = oldCode + 1;

const currentVersion = String(versionData.version || '1.0.0');
const parts = currentVersion.split('.').map(Number);
if (parts.length >= 3) {
  parts[2] = (parts[2] || 0) + 1;
} else if (parts.length === 2) {
  parts.push(1);
} else {
  parts.push(0, 1);
}
const newVersion = parts.join('.');

const today = new Date().toISOString().split('T')[0];

versionData.version = newVersion;
versionData.versionCode = newCode;
// Ensure downloadUrl points to GitHub Releases
if (!versionData.downloadUrl || versionData.downloadUrl === '/download/app') {
  versionData.downloadUrl = 'https://github.com/kingpin222/watzem/releases/latest/download/app-release.apk';
}

fs.writeFileSync(versionJsonPath, JSON.stringify(versionData, null, 2) + '\n');
console.log(`📦 [Auto-Bump] version.json updated: v${newVersion} (code: ${newCode})`);

// 3. Update public/version.json & dist/version.json
const publicVersionPath = path.join(projectRoot, 'public/version.json');
fs.writeFileSync(publicVersionPath, JSON.stringify(versionData, null, 2) + '\n');

const distVersionPath = path.join(projectRoot, 'dist/version.json');
if (fs.existsSync(path.dirname(distVersionPath))) {
  fs.writeFileSync(distVersionPath, JSON.stringify(versionData, null, 2) + '\n');
}

// 4. Update update-server/version.json
const updateServerVersionPath = path.join(projectRoot, 'update-server/version.json');
if (fs.existsSync(path.dirname(updateServerVersionPath))) {
  fs.writeFileSync(updateServerVersionPath, JSON.stringify(versionData, null, 2) + '\n');
}

// 5. Update package.json
if (fs.existsSync(packageJsonPath)) {
  const pkg = JSON.parse(fs.readFileSync(packageJsonPath, 'utf-8'));
  pkg.version = newVersion;
  fs.writeFileSync(packageJsonPath, JSON.stringify(pkg, null, 2) + '\n');
  console.log(`📄 [Auto-Bump] package.json version updated to: ${newVersion}`);
}

// 6. Update android/app/build.gradle
if (fs.existsSync(gradlePath)) {
  let gradle = fs.readFileSync(gradlePath, 'utf-8');
  gradle = gradle.replace(/versionCode\s+\d+/, `versionCode ${newCode}`);
  gradle = gradle.replace(/versionName\s+"[^"]+"/, `versionName "${newVersion}"`);
  fs.writeFileSync(gradlePath, gradle);
  console.log(`🤖 [Auto-Bump] android/app/build.gradle updated: versionCode ${newCode}, versionName "${newVersion}"`);
}

console.log(`✅ [Auto-Bump] Successfully bumped to v${newVersion} (versionCode: ${newCode})\n`);
