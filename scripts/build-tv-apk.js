import { execSync } from 'child_process';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '..');

console.log('📺 Android and Android TV have been consolidated into a Single Universal APK!');
console.log('🚀 Running universal build:apk...\n');
execSync('node scripts/build-apk.js', { cwd: projectRoot, stdio: 'inherit' });
process.exit(0);



