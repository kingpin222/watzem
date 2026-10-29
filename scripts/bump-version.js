import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '..');

const versionJsonPath = path.join(projectRoot, 'version.json');
const packageJsonPath = path.join(projectRoot, 'package.json');
const nativeGradlePath = path.join(projectRoot, 'watzem-native-android/app/build.gradle.kts');

// 1. Read version.json
if (!fs.existsSync(versionJsonPath)) {
  console.error('❌ version.json not found at:', versionJsonPath);
  process.exit(1);
}

const versionData = JSON.parse(fs.readFileSync(versionJsonPath, 'utf-8'));

// 2. Increment or set versionCode and version
const targetVersion = process.argv[2];
const targetCode = process.argv[3] ? Number(process.argv[3]) : null;

const oldCode = Number(versionData.versionCode) || 1;
const newCode = targetCode || (oldCode + 1);

let newVersion;
if (targetVersion) {
  newVersion = targetVersion.replace(/^v/, '');
} else {
  const currentVersion = String(versionData.version || '1.0.0');
  const parts = currentVersion.split('.').map(Number);
  if (parts.length >= 3) {
    parts[2] = (parts[2] || 0) + 1;
  } else if (parts.length === 2) {
    parts.push(1);
  } else {
    parts.push(0, 1);
  }
  newVersion = parts.join('.');
}

const today = new Date().toISOString().split('T')[0];

versionData.version = newVersion;
versionData.latestVersion = newVersion;
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

// 6. Update Native Android build file
if (fs.existsSync(nativeGradlePath)) {
  let nativeGradle = fs.readFileSync(nativeGradlePath, 'utf-8');
  nativeGradle = nativeGradle.replace(/versionCode\s*=\s*\d+/, `versionCode = ${newCode}`);
  nativeGradle = nativeGradle.replace(/versionName\s*=\s*"[^"]+"/, `versionName = "${newVersion}"`);
  fs.writeFileSync(nativeGradlePath, nativeGradle);
  console.log(`🤖 [Auto-Bump] ${path.relative(projectRoot, nativeGradlePath)} updated: versionCode = ${newCode}, versionName = "${newVersion}"`);
}

// 7. Update iOS build file
const iosProjectPath = path.join(projectRoot, 'ios/App/App.xcodeproj/project.pbxproj');
if (fs.existsSync(iosProjectPath)) {
  let iosProject = fs.readFileSync(iosProjectPath, 'utf-8');
  iosProject = iosProject.replace(/CURRENT_PROJECT_VERSION = \d+;/g, `CURRENT_PROJECT_VERSION = ${newCode};`);
  iosProject = iosProject.replace(/MARKETING_VERSION = [^;]+;/g, `MARKETING_VERSION = ${newVersion};`);
  fs.writeFileSync(iosProjectPath, iosProject);
  console.log(`🍎 [Auto-Bump] ${path.relative(projectRoot, iosProjectPath)} updated: CURRENT_PROJECT_VERSION = ${newCode}, MARKETING_VERSION = ${newVersion}`);
}

console.log(`✅ [Auto-Bump] Successfully bumped to v${newVersion} (versionCode: ${newCode})\n`);
