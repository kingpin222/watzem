import fs from 'fs';
import path from 'path';
import { execSync } from 'child_process';
import { fileURLToPath } from 'url';
import dotenv from 'dotenv';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const projectRoot = path.resolve(__dirname, '..');

// Load .env
dotenv.config({ path: path.join(projectRoot, '.env') });

function run(command, cwd = projectRoot) {
  console.log(`▶️ ${command}`);
  execSync(command, { cwd, stdio: 'inherit' });
}

export async function uploadRelease() {
  console.log('\n🌐 ========================================');
  console.log('🌐 WATZEM AUTOMATED GITHUB RELEASE UPLOADER');
  console.log('🌐 ========================================\n');

  const token = process.env.GITHUB_TOKEN;
  if (!token) {
    console.error('❌ GITHUB_TOKEN is not defined in .env or environment!');
    console.error('👉 Please ensure GITHUB_TOKEN=ghp_... is present in .env');
    return false;
  }

  // 1. Read version metadata
  const versionJsonPath = path.join(projectRoot, 'version.json');
  if (!fs.existsSync(versionJsonPath)) {
    console.error('❌ version.json not found!');
    return false;
  }

  const versionData = JSON.parse(fs.readFileSync(versionJsonPath, 'utf-8'));
  const version = versionData.version;
  const tag = `v${version}`;
  const repoOwner = 'kingpin222';
  const repoName = 'watzem';
  const repoUrl = `https://api.github.com/repos/${repoOwner}/${repoName}`;

  console.log(`📌 Target Release: ${tag} (Version Code: ${versionData.versionCode})`);

  // 2. Locate release APK
  const apkPaths = [
    path.join(projectRoot, 'android/app/build/outputs/apk/release/app-release.apk'),
    path.join(projectRoot, 'public/install.apk')
  ];

  let apkFile = apkPaths.find(p => fs.existsSync(p));
  if (!apkFile) {
    console.error('❌ Could not find compiled APK file!');
    console.error('👉 Looked in:', apkPaths);
    return false;
  }

  const apkStats = fs.statSync(apkFile);
  const sizeMB = (apkStats.size / (1024 * 1024)).toFixed(2);
  console.log(`📦 Found APK: ${apkFile} (${sizeMB} MB)`);

  // 3. Update fileSize in version.json if different
  if (versionData.fileSize !== `${sizeMB} MB`) {
    versionData.fileSize = `${sizeMB} MB`;
    fs.writeFileSync(versionJsonPath, JSON.stringify(versionData, null, 2) + '\n');
    console.log(`📝 Updated fileSize in version.json to: ${sizeMB} MB`);
  }

  // 4. Git sync: Commit on source, sync version.json to release branch and push to origin/main
  console.log('\n🔀 Step 1/3: Synchronizing Git repository...');
  let stashed = false;
  try {
    // Stage version changes on source
    try {
      run('git add version.json package.json android/app/build.gradle');
      run(`git commit -m "chore: bump version to ${tag} (code: ${versionData.versionCode})"`);
    } catch (_) {
      // Nothing new to commit on source
    }

    // Stash any uncommitted files on source so branch checkout is clean
    const dirty = execSync('git status --porcelain', { cwd: projectRoot }).toString().trim();
    if (dirty) {
      console.log('📦 Stashing temporary local changes...');
      run('git stash');
      stashed = true;
    }

    // Switch to release branch to sync version.json strictly without source code
    run('git checkout release');
    run('git checkout source -- version.json');
    run('git add version.json');
    try {
      run(`git commit -m "chore: release manifest ${tag} (code: ${versionData.versionCode})"`);
    } catch (_) {
      // Manifest already up to date
    }

    // Push release branch to origin main
    run('git push origin release:main');

    // Create & push git tag
    run(`git tag -a "${tag}" -m "Release ${tag}" -f`);
    run(`git push origin "${tag}" --force`);

    console.log('✅ Git sync and tag push completed successfully.');
  } catch (err) {
    console.warn('⚠️ Git sync warning:', err.message);
  } finally {
    // Ensure we ALWAYS return to source branch
    try { run('git checkout source'); } catch (_) {}
    if (stashed) {
      console.log('📦 Restoring temporary local changes...');
      try { run('git stash pop'); } catch (_) {}
    }
  }

  // 5. GitHub API: Get or create release
  console.log('\n🐙 Step 2/3: Checking GitHub Release...');
  const headers = {
    'Authorization': `Bearer ${token}`,
    'Accept': 'application/vnd.github.v3+json',
    'User-Agent': 'WATZEM-Release-Uploader'
  };

  let release = null;
  const getReleaseRes = await fetch(`${repoUrl}/releases/tags/${tag}`, { headers });
  if (getReleaseRes.ok) {
    release = await getReleaseRes.json();
    console.log(`ℹ️ Existing release found for ${tag} (ID: ${release.id})`);
  } else if (getReleaseRes.status === 404) {
    console.log(`ℹ️ Release ${tag} does not exist yet. Creating...`);
    const notesText = Array.isArray(versionData.releaseNotes)
      ? versionData.releaseNotes.map(n => `- ${n}`).join('\n')
      : 'General stability and performance improvements.';

    const createRes = await fetch(`${repoUrl}/releases`, {
      method: 'POST',
      headers: { ...headers, 'Content-Type': 'application/json' },
      body: JSON.stringify({
        tag_name: tag,
        target_commitish: 'main',
        name: `v${version}`,
        body: `## What's Changed\n\n${notesText}\n\n**Build**: ${versionData.versionCode}\n**Date**: ${versionData.releaseDate || new Date().toISOString().split('T')[0]}`,
        draft: false,
        prerelease: false
      })
    });

    if (!createRes.ok) {
      const errText = await createRes.text();
      console.error(`❌ Failed to create GitHub release (status ${createRes.status}):`, errText);
      return false;
    }

    release = await createRes.json();
    console.log(`✅ GitHub release created: ${release.html_url}`);
  } else {
    console.error(`❌ Error fetching release (status ${getReleaseRes.status})`);
    return false;
  }

  // 6. Check existing asset named app-release.apk and delete if already present
  if (release.assets && release.assets.length > 0) {
    const existingAsset = release.assets.find(a => a.name === 'app-release.apk');
    if (existingAsset) {
      console.log(`🗑️ Removing previous app-release.apk asset (ID: ${existingAsset.id})...`);
      await fetch(`${repoUrl}/releases/assets/${existingAsset.id}`, {
        method: 'DELETE',
        headers
      });
    }
  }

  // 7. Upload app-release.apk binary
  console.log(`\n🚀 Step 3/3: Uploading ${path.basename(apkFile)} (${sizeMB} MB) to GitHub Releases...`);
  const apkBuffer = fs.readFileSync(apkFile);
  const uploadUrl = `https://uploads.github.com/repos/${repoOwner}/${repoName}/releases/${release.id}/assets?name=app-release.apk`;

  const uploadRes = await fetch(uploadUrl, {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${token}`,
      'Content-Type': 'application/vnd.android.package-archive',
      'Content-Length': String(apkBuffer.length),
      'User-Agent': 'WATZEM-Release-Uploader'
    },
    body: apkBuffer,
    duplex: 'half'
  });

  if (!uploadRes.ok) {
    const errText = await uploadRes.text();
    console.error(`❌ Upload failed with status ${uploadRes.status}:`, errText);
    return false;
  }

  const assetData = await uploadRes.json();
  console.log('\n🎉 ========================================');
  console.log('🎉 DIRECT RELEASE UPLOAD SUCCESSFUL!');
  console.log(`🏷️ Release Tag: ${tag}`);
  console.log(`📦 Asset: ${assetData.name} (${sizeMB} MB)`);
  console.log(`🔗 Release Page: ${release.html_url}`);
  console.log(`⬇️ Latest Download: https://github.com/${repoOwner}/${repoName}/releases/latest/download/app-release.apk`);
  console.log('🎉 ========================================\n');

  return true;
}

// Allow standalone execution: node scripts/upload-release.js
if (process.argv[1] === fileURLToPath(import.meta.url)) {
  uploadRelease().catch(err => {
    console.error('Fatal error during upload:', err);
    process.exit(1);
  });
}
