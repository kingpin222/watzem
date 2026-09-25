const path = require('path');

// node-windows only runs natively on Windows
if (process.platform !== 'win32') {
  console.log('----------------------------------------------------');
  console.log('⚠️  Notice: node-windows requires a Windows operating system.');
  console.log('   Current platform:', process.platform);
  console.log('   To install this Windows Service, deploy/copy this project to your Windows host/server');
  console.log('   and run:');
  console.log('      npm run service:install');
  console.log('----------------------------------------------------');
  process.exit(0);
}

const { Service } = require('node-windows');

// Script to be run as service (compiled server entry)
const scriptPath = path.resolve(__dirname, 'dist-server', 'index.js');

const svc = new Service({
  name: 'WATZEM-API',
  displayName: 'WATZEM API Service',
  description: 'WATZEM Movie App Backend API Server running on port 2727',
  script: scriptPath,
  nodeOptions: [
    '--max_old_space_size=4096'
  ],
  env: [
    {
      name: 'PORT',
      value: '2727'
    },
    {
      name: 'NODE_ENV',
      value: 'production'
    }
  ],
  wait: 2,
  grow: 0.5,
  maxRetries: 10
});

svc.on('install', function () {
  console.log('✅ WATZEM API Service installed successfully!');
  console.log('🚀 Starting service on port 2727...');
  svc.start();
});

svc.on('alreadyinstalled', function () {
  console.log('ℹ️ WATZEM API Service is already installed.');
  console.log('   If you want to reinstall, run "npm run service:uninstall" first.');
});

svc.on('start', function () {
  console.log('🍿 WATZEM API Service is now running at http://localhost:2727');
});

svc.on('invalidinstallation', function () {
  console.error('❌ Service installation failed or is invalid.');
});

svc.on('error', function (err) {
  console.error('❌ Service error:', err);
});

console.log('📦 Installing WATZEM API Service (Port 2727)...');
svc.install();
