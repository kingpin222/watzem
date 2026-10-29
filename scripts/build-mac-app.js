#!/usr/bin/env node

import fs from 'fs';
import path from 'path';
import os from 'os';
import { execSync } from 'child_process';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '..');

const APP_NAME = 'WATZEM';
const BUNDLE_ID = 'com.watzem.desktop';
const VERSION = '1.1.257';

const args = process.argv.slice(2);
const shouldInstallApps = args.includes('--install') || args.includes('--install-applications') || args.includes('-i');
const shouldEnableStartup = args.includes('--startup') || args.includes('--enable-startup') || args.includes('-s');

console.log('═══════════════════════════════════════════════════════════════');
console.log('🍿  BUILDING WATZEM macOS APPLICATION BUNDLE');
console.log('═══════════════════════════════════════════════════════════════');

if (process.platform !== 'darwin') {
  console.error('❌ Error: macOS application bundle can only be built on macOS.');
  process.exit(1);
}

// 1. Ensure build artifacts exist
const clientDist = path.join(projectRoot, 'dist');
const serverDist = path.join(projectRoot, 'dist-server', 'index.js');

if (!fs.existsSync(serverDist) || !fs.existsSync(path.join(clientDist, 'index.html'))) {
  console.log('🔨 Compiling frontend and backend bundles...');
  execSync('npm run build', { cwd: projectRoot, stdio: 'inherit' });
} else {
  console.log('✅ Existing build artifacts found in dist/ and dist-server/.');
}

// 2. Generate macOS AppIcon.icns
const iconSrc = path.join(projectRoot, 'public', 'icon-512.png');
const appBundleDir = path.join(projectRoot, `${APP_NAME}.app`);
const contentsDir = path.join(appBundleDir, 'Contents');
const macosDir = path.join(contentsDir, 'MacOS');
const resourcesDir = path.join(contentsDir, 'Resources');

fs.mkdirSync(macosDir, { recursive: true });
fs.mkdirSync(resourcesDir, { recursive: true });

const targetIcns = path.join(resourcesDir, 'AppIcon.icns');

console.log('🎨 Generating high-resolution AppIcon.icns...');
const tmpIconset = path.join(os.tmpdir(), `watzem_${Date.now()}.iconset`);
fs.mkdirSync(tmpIconset, { recursive: true });

const iconSizes = [
  { name: 'icon_16x16.png', size: 16 },
  { name: 'icon_16x16@2x.png', size: 32 },
  { name: 'icon_32x32.png', size: 32 },
  { name: 'icon_32x32@2x.png', size: 64 },
  { name: 'icon_128x128.png', size: 128 },
  { name: 'icon_128x128@2x.png', size: 256 },
  { name: 'icon_256x256.png', size: 256 },
  { name: 'icon_256x256@2x.png', size: 512 },
  { name: 'icon_512x512.png', size: 512 }
];

try {
  for (const s of iconSizes) {
    const dest = path.join(tmpIconset, s.name);
    execSync(`sips -z ${s.size} ${s.size} "${iconSrc}" --out "${dest}"`, { stdio: 'ignore' });
  }
  execSync(`iconutil -c icns "${tmpIconset}" -o "${targetIcns}"`, { stdio: 'ignore' });
  console.log('✅ AppIcon.icns created successfully.');
} catch (err) {
  console.warn('⚠️ Warning: Could not generate .icns with iconutil, falling back to direct copy:', err.message);
  fs.copyFileSync(iconSrc, path.join(resourcesDir, 'icon.png'));
} finally {
  try {
    fs.rmSync(tmpIconset, { recursive: true, force: true });
  } catch {}
}

// 3. Write Info.plist
console.log('📄 Writing Info.plist & PkgInfo...');
const infoPlistContent = `<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>${APP_NAME}</string>
    <key>CFBundleIconFile</key>
    <string>AppIcon</string>
    <key>CFBundleIdentifier</key>
    <string>${BUNDLE_ID}</string>
    <key>CFBundleName</key>
    <string>${APP_NAME}</string>
    <key>CFBundleDisplayName</key>
    <string>${APP_NAME}</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleShortVersionString</key>
    <string>${VERSION}</string>
    <key>CFBundleVersion</key>
    <string>${VERSION}</string>
    <key>LSMinimumSystemVersion</key>
    <string>10.13</string>
    <key>NSHighResolutionCapable</key>
    <true/>
    <key>NSSupportsAutomaticGraphicsSwitching</key>
    <true/>
</dict>
</plist>`;

fs.writeFileSync(path.join(contentsDir, 'Info.plist'), infoPlistContent, 'utf8');
fs.writeFileSync(path.join(contentsDir, 'PkgInfo'), 'APPL????', 'utf8');

// 4. Write Executable Launcher Script
console.log('🚀 Writing native executable launcher...');
const nodePath = process.execPath; // current node executable
const launcherScriptPath = path.join(macosDir, APP_NAME);

const launcherScript = `#!/bin/bash
# -------------------------------------------------------------
# WATZEM macOS Native App Launcher
# -------------------------------------------------------------
set -e

DIR="$( cd "$( dirname "\${BASH_SOURCE[0]}" )" && pwd )"
PROJECT_DIR="${projectRoot}"

if [ ! -f "\${PROJECT_DIR}/dist-server/index.js" ]; then
  # Fallback relative to .app bundle
  PROJECT_DIR="$( cd "\${DIR}/../../.." && pwd )"
fi

export PORT=2727
export NODE_ENV=production
LOG_DIR="$HOME/Library/Logs"
LOG_FILE="$LOG_DIR/watzem-app.log"
mkdir -p "$LOG_DIR"

# Resolve Node binary
NODE_BIN=""
for candidate in \\
  "${nodePath}" \\
  "/usr/local/bin/node" \\
  "/opt/homebrew/bin/node" \\
  "\$(which node 2>/dev/null)" \\
  "\$HOME/.nvm/versions/node/\$(ls -t "\$HOME/.nvm/versions/node" 2>/dev/null | head -n 1)/bin/node"
do
  if [ -n "$candidate" ] && [ -x "$candidate" ]; then
    NODE_BIN="$candidate"
    break
  fi
done

if [ -z "$NODE_BIN" ]; then
  osascript -e 'display alert "WATZEM Error" message "Node.js was not found. Please ensure Node.js is installed." as critical'
  exit 1
fi

SERVER_URL="http://127.0.0.1:\${PORT}"
HEALTH_URL="\${SERVER_URL}/api/health"

# Check if server is already running
IS_RUNNING=0
if curl -s -m 2 "$HEALTH_URL" | grep -q "status"; then
  IS_RUNNING=1
fi

if [ $IS_RUNNING -eq 0 ]; then
  cd "$PROJECT_DIR"
  nohup "$NODE_BIN" dist-server/index.js >> "$LOG_FILE" 2>&1 &
  
  # Wait up to 10 seconds for API to be ready
  READY=0
  for i in {1..30}; do
    if curl -s -m 1 "$HEALTH_URL" | grep -q "status"; then
      READY=1
      break
    fi
    sleep 0.3
  done
  
  if [ $READY -eq 0 ]; then
    osascript -e 'display alert "WATZEM Error" message "Failed to start local streaming server. Check ~/Library/Logs/watzem-app.log for details." as critical'
    exit 1
  fi
fi

# Launch in dedicated Chromium standalone window if available (Brave, Chrome, Edge, Arc)
# or open in default system browser
APP_LAUNCHED=0

if [ -d "/Applications/Brave Browser.app" ]; then
  open -n -a "Brave Browser" --args --app="$SERVER_URL"
  APP_LAUNCHED=1
elif [ -d "/Applications/Google Chrome.app" ]; then
  open -n -a "Google Chrome" --args --app="$SERVER_URL"
  APP_LAUNCHED=1
elif [ -d "/Applications/Microsoft Edge.app" ]; then
  open -n -a "Microsoft Edge" --args --app="$SERVER_URL"
  APP_LAUNCHED=1
elif [ -d "/Applications/Arc.app" ]; then
  open -n -a "Arc" "$SERVER_URL"
  APP_LAUNCHED=1
fi

if [ $APP_LAUNCHED -eq 0 ]; then
  open "$SERVER_URL"
fi
`;

fs.writeFileSync(launcherScriptPath, launcherScript, 'utf8');
fs.chmodSync(launcherScriptPath, 0o755);

console.log(`✅ App bundle built successfully at:\n   ${appBundleDir}`);

// 5. Optional Installation into /Applications
let installedAppPath = appBundleDir;
if (shouldInstallApps) {
  const targetDir = fs.existsSync('/Applications') ? '/Applications' : path.join(os.homedir(), 'Applications');
  const targetApp = path.join(targetDir, `${APP_NAME}.app`);
  console.log(`📦 Installing ${APP_NAME}.app into ${targetApp}...`);
  try {
    execSync(`rm -rf "${targetApp}" && cp -R "${appBundleDir}" "${targetApp}"`);
    installedAppPath = targetApp;
    console.log(`✅ Installed to ${targetApp}! You can now launch WATZEM from Spotlight or Dock.`);
  } catch (err) {
    console.warn(`⚠️ Could not write to /Applications directly (${err.message}). Trying ~/Applications...`);
    const userApps = path.join(os.homedir(), 'Applications');
    fs.mkdirSync(userApps, { recursive: true });
    const userTargetApp = path.join(userApps, `${APP_NAME}.app`);
    execSync(`rm -rf "${userTargetApp}" && cp -R "${appBundleDir}" "${userTargetApp}"`);
    installedAppPath = userTargetApp;
    console.log(`✅ Installed to ${userTargetApp}!`);
  }
}

// 6. Optional Startup Configuration (Login Item)
if (shouldEnableStartup) {
  console.log('🔄 Registering WATZEM into macOS Login Items (runs automatically on startup)...');
  try {
    const escapedPath = installedAppPath.replace(/"/g, '\\"');
    const osaScript = `tell application "System Events"
      delete (every login item whose name is "${APP_NAME}")
      make login item at end with properties {path:"${escapedPath}", hidden:false, name:"${APP_NAME}"}
    end tell`;
    execSync(`osascript -e '${osaScript}'`);
    console.log(`✅ WATZEM registered as macOS Login Item!`);
    console.log(`   It will now launch automatically when your Mac starts up or you log in.`);
  } catch (err) {
    console.error('❌ Failed to add login item via osascript:', err.message);
  }
}

console.log('───────────────────────────────────────────────────────────────');
console.log('🎉 Done! Quick commands:');
console.log(`   • Open App now:        open "${installedAppPath}"`);
console.log(`   • Install to /Apps:    node scripts/build-mac-app.js --install --startup`);
console.log(`   • Manage Startup:      npm run mac:startup / npm run mac:startup:disable`);
console.log(`   • LaunchAgent Service: npm run service:install`);
console.log('═══════════════════════════════════════════════════════════════');
