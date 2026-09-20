import JSZip from 'jszip';
import { ANDROID_PROJECT_FILES } from '../data/projectFiles';

export async function exportProjectZip(): Promise<void> {
  const zip = new JSZip();

  // Root files
  zip.file('README.md', `# SecureVault Android Project\nOffline-first secure password manager.`);
  zip.file('build.gradle.kts', ANDROID_PROJECT_FILES.find(f => f.path.includes('android/build.gradle.kts'))?.content || '');
  zip.file('settings.gradle.kts', ANDROID_PROJECT_FILES.find(f => f.path.includes('android/settings.gradle.kts'))?.content || '');
  zip.file('gradle.properties', `org.gradle.jvmargs=-Xmx2048m\nandroid.useAndroidX=true\nandroid.nonTransitiveRClass=true`);
  zip.file('gradlew', `#!/bin/sh\nexec gradle "$@"\n`);

  // GitHub Actions Workflow for automated APK compilation in cloud
  const ghFolder = zip.folder('.github')?.folder('workflows');
  if (ghFolder) {
    ghFolder.file('build-apk.yml', `name: Build SecureVault APK
on: [push, workflow_dispatch]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v4
    - uses: actions/setup-java@v4
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: gradle
    - run: ./gradlew assembleDebug --no-daemon
    - uses: actions/upload-artifact@v4
      with:
        name: SecureVault-Debug-APK
        path: app/build/outputs/apk/debug/*.apk
`);
  }

  // App module files
  const appFolder = zip.folder('app');
  if (appFolder) {
    appFolder.file('build.gradle.kts', ANDROID_PROJECT_FILES.find(f => f.path.includes('app/build.gradle.kts'))?.content || '');
    appFolder.file('proguard-rules.pro', `-keep class net.zetetic.database.sqlcipher.** { *; }`);
    
    const srcMain = appFolder.folder('src')?.folder('main');
    if (srcMain) {
      srcMain.file('AndroidManifest.xml', ANDROID_PROJECT_FILES.find(f => f.path.includes('AndroidManifest.xml'))?.content || '');
      
      const javaFolder = srcMain.folder('java')?.folder('com')?.folder('securevault')?.folder('passwordmanager');
      if (javaFolder) {
        for (const file of ANDROID_PROJECT_FILES) {
          if (file.path.startsWith('com/')) {
            const relative = file.path.replace('com/securevault/passwordmanager/', '');
            javaFolder.file(relative, file.content);
          }
        }
      }
    }
  }

  const blob = await zip.generateAsync({ type: 'blob' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'SecureVault-Android-Project.zip';
  document.body.appendChild(a);
  a.click();
  document.body.removeChild(a);
  URL.revokeObjectURL(url);
}
