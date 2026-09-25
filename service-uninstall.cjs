const path = require('path');

// node-windows only runs natively on Windows
if (process.platform !== 'win32') {
  console.log('----------------------------------------------------');
  console.log('⚠️  Notice: node-windows requires a Windows operating system.');
  console.log('   Current platform:', process.platform);
  console.log('   To uninstall this Windows Service, run on your Windows host/server:');
  console.log('      npm run service:uninstall');
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
