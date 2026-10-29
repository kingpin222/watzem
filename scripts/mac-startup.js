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
const PLIST_LABEL = 'com.watzem.api';
const PLIST_PATH = path.join(os.homedir(), 'Library', 'LaunchAgents', `${PLIST_LABEL}.plist`);
const LOG_DIR = path.join(os.homedir(), 'Library', 'Logs');
const STDOUT_LOG = path.join(LOG_DIR, 'watzem-api.log');
const STDERR_LOG = path.join(LOG_DIR, 'watzem-api-error.log');

const args = process.argv.slice(2);
const command = args[0] || '--status';

function getAppPath() {
  const standardApp = '/Applications/WATZEM.app';
  const userApp = path.join(os.homedir(), 'Applications', 'WATZEM.app');
  const localApp = path.join(projectRoot, 'WATZEM.app');

  if (fs.existsSync(standardApp)) return standardApp;
  if (fs.existsSync(userApp)) return userApp;
  if (fs.existsSync(localApp)) return localApp;
  return localApp;
}

function isAppInLoginItems() {
  try {
    const output = execSync(`osascript -e 'tell application "System Events" to get name of every login item'`, { encoding: 'utf8' });
    return output.split(',').map(s => s.trim().toLowerCase()).includes(APP_NAME.toLowerCase());
  } catch {
    return false;
  }
}

function addAppToLoginItems() {
  const appPath = getAppPath();
  if (!fs.existsSync(appPath)) {
    console.log(`🔨 ${APP_NAME}.app not found. Building it first...`);
    execSync(`node "${path.join(projectRoot, 'scripts', 'build-mac-app.js')}" --install`, { stdio: 'inherit' });
  }

  const resolvedAppPath = getAppPath();
  console.log(`🔄 Adding "${resolvedAppPath}" to macOS Login Items...`);
  const escapedPath = resolvedAppPath.replace(/"/g, '\\"');
  const osaScript = `tell application "System Events"
    delete (every login item whose name is "${APP_NAME}")
    make login item at end with properties {path:"${escapedPath}", hidden:false, name:"${APP_NAME}"}
  end tell`;
  execSync(`osascript -e '${osaScript}'`);
  console.log('✅ WATZEM added to macOS Login Items (will open when you log into Mac).');
}

function removeAppFromLoginItems() {
  console.log(`🗑️ Removing "${APP_NAME}" from macOS Login Items...`);
  try {
    const osaScript = `tell application "System Events"
      delete (every login item whose name is "${APP_NAME}")
    end tell`;
    execSync(`osascript -e '${osaScript}'`);
    console.log('✅ Removed WATZEM from macOS Login Items.');
  } catch (err) {
    console.log('ℹ️ WATZEM was not in macOS Login Items.');
  }
}

function isLaunchAgentRunning() {
  try {
    const output = execSync(`launchctl list | grep "${PLIST_LABEL}" || true`, { encoding: 'utf8' });
    return output.trim().length > 0;
  } catch {
    return false;
  }
}

function isServerHealthy() {
  try {
    const output = execSync(`curl -s -m 2 http://127.0.0.1:2727/api/health`, { encoding: 'utf8' });
    return output.includes('"status"');
  } catch {
    return false;
  }
}

function installLaunchAgent() {
  console.log(`📦 Configuring macOS LaunchAgent background service (${PLIST_LABEL})...`);

  // Ensure server bundle is built
  const serverDist = path.join(projectRoot, 'dist-server', 'index.js');
  if (!fs.existsSync(serverDist)) {
    console.log('🔨 Compiling backend server with TypeScript...');
    execSync('npm run build:server', { cwd: projectRoot, stdio: 'inherit' });
  }

  fs.mkdirSync(path.join(os.homedir(), 'Library', 'LaunchAgents'), { recursive: true });
  fs.mkdirSync(LOG_DIR, { recursive: true });

  const nodeBin = process.execPath;
  const plistContent = `<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>Label</key>
    <string>${PLIST_LABEL}</string>
    <key>ProgramArguments</key>
    <array>
        <string>${nodeBin}</string>
        <string>${serverDist}</string>
    </array>
    <key>WorkingDirectory</key>
    <string>${projectRoot}</string>
    <key>EnvironmentVariables</key>
    <dict>
        <key>PORT</key>
        <string>2727</string>
        <key>NODE_ENV</key>
        <string>production</string>
        <key>PATH</key>
        <string>/usr/local/bin:/usr/bin:/bin:/usr/sbin:/sbin:/opt/homebrew/bin</string>
    </dict>
    <key>RunAtLoad</key>
    <true/>
    <key>KeepAlive</key>
    <true/>
    <key>StandardOutPath</key>
    <string>${STDOUT_LOG}</string>
    <key>StandardErrorPath</key>
    <string>${STDERR_LOG}</string>
</dict>
</plist>`;

  // Unload previous instance if present
  try {
    execSync(`launchctl unload "${PLIST_PATH}" 2>/dev/null || true`);
  } catch {}

  fs.writeFileSync(PLIST_PATH, plistContent, 'utf8');

  // Load new agent
  execSync(`launchctl load -w "${PLIST_PATH}"`);
  console.log(`✅ LaunchAgent installed to ${PLIST_PATH}`);
  console.log(`🚀 Service loaded with launchctl. Checking health...`);

  // Wait a couple seconds and check health
  let healthy = false;
  for (let i = 0; i < 15; i++) {
    if (isServerHealthy()) {
      healthy = true;
      break;
    }
    execSync('sleep 0.3');
  }

  if (healthy) {
    console.log(`🍿 WATZEM API Service is running at http://localhost:2727 (Logs: ${STDOUT_LOG})`);
  } else {
    console.log(`⚠️ Service registered. View logs at: ${STDERR_LOG}`);
  }
}

function uninstallLaunchAgent() {
  console.log(`🗑️ Unloading LaunchAgent (${PLIST_LABEL})...`);
  try {
    execSync(`launchctl unload -w "${PLIST_PATH}" 2>/dev/null || true`);
  } catch {}

  if (fs.existsSync(PLIST_PATH)) {
    fs.unlinkSync(PLIST_PATH);
    console.log(`✅ Removed ${PLIST_PATH}`);
  } else {
    console.log(`ℹ️ LaunchAgent plist file was not present.`);
  }
}

function printStatus() {
  console.log('═══════════════════════════════════════════════════════════════');
  console.log('📊 WATZEM macOS Startup & Service Status');
  console.log('═══════════════════════════════════════════════════════════════');

  const inLogin = isAppInLoginItems();
  const agentRunning = isLaunchAgentRunning();
  const serverUp = isServerHealthy();
  const appPath = getAppPath();

  console.log(`• WATZEM.app bundle:        ${fs.existsSync(appPath) ? `Installed (${appPath})` : 'Not built yet'}`);
  console.log(`• Startup Login Item:       ${inLogin ? '🟢 Enabled (Launches GUI on login)' : '⚪ Disabled'}`);
  console.log(`• LaunchAgent Background:   ${agentRunning ? '🟢 Running (Auto-restarts at boot)' : '⚪ Inactive'}`);
  console.log(`• API Server (port 2727):   ${serverUp ? '🟢 Online (http://localhost:2727)' : '🔴 Offline'}`);
  console.log('───────────────────────────────────────────────────────────────');
  console.log('Commands:');
  console.log('  npm run mac:startup           - Enable both App & Background Service');
  console.log('  npm run mac:startup:disable   - Disable both');
  console.log('  npm run service:install       - Start background daemon only');
  console.log('  npm run service:uninstall     - Stop background daemon');
  console.log('═══════════════════════════════════════════════════════════════');
}

// Action dispatcher
switch (command) {
  case '--status':
  case 'status':
    printStatus();
    break;
  case '--enable-app':
    addAppToLoginItems();
    printStatus();
    break;
  case '--disable-app':
    removeAppFromLoginItems();
    printStatus();
    break;
  case '--enable-service':
    installLaunchAgent();
    printStatus();
    break;
  case '--disable-service':
    uninstallLaunchAgent();
    printStatus();
    break;
  case '--enable-all':
  case '--enable':
  case 'enable':
    installLaunchAgent();
    addAppToLoginItems();
    printStatus();
    break;
  case '--disable-all':
  case '--disable':
  case 'disable':
    uninstallLaunchAgent();
    removeAppFromLoginItems();
    printStatus();
    break;
  default:
    console.log(`Unknown command: ${command}`);
    printStatus();
    break;
}
