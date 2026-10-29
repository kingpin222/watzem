const path = require('path');
const { execSync } = require('child_process');

if (process.platform === 'darwin') {
  console.log('🍏 Detected macOS — Unloading and removing launchd LaunchAgent service...');
  try {
    const startupScript = path.resolve(__dirname, 'scripts', 'mac-startup.js');
    execSync(`node "${startupScript}" --disable-service`, { stdio: 'inherit' });
    process.exit(0);
  } catch (err) {
    console.error('❌ Failed to uninstall macOS service:', err.message);
    process.exit(1);
  }
}

// Windows Support via node-windows
if (process.platform !== 'win32') {
  console.log('----------------------------------------------------');
  console.log('⚠️  Notice: Automatic background service is supported on macOS (launchd) and Windows.');
  console.log('   Current platform:', process.platform);
  console.log('----------------------------------------------------');
  process.exit(0);
}

const { Service } = require('node-windows');

const scriptPath = path.resolve(__dirname, 'dist-server', 'index.js');

const svc = new Service({
  name: 'WATZEM-API',
  script: scriptPath
});

svc.on('uninstall', function () {
  console.log('🗑️ WATZEM API Service uninstalled successfully.');
  console.log('   The service has been removed from Windows Services.');
});

svc.on('alreadyuninstalled', function () {
  console.log('ℹ️ WATZEM API Service is already uninstalled.');
});

svc.on('error', function (err) {
  console.error('❌ Service uninstall error:', err);
});

console.log('🗑️ Uninstalling WATZEM API Service...');
svc.uninstall();
