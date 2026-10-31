# Build the APK from GitHub

The workflow at `.github/workflows/android-build.yml` builds a debug APK automatically.

## 1. Push to GitHub
```bash
git init
git add .
git commit -m "Initial Mandu AI"
git branch -M main
git remote add origin https://github.com/<you>/ManduAI.git
git push -u origin main
```

## 2. Wait for the build

Open your repo → **Actions** → **Build Android APK**. It runs `gradle assembleDebug`.

## 3. Download the APK

- Open the completed run → **Artifacts** → `mandu-debug-apk` → unzip → `app-debug.apk`.
- Install on any Android 8.0+ (minSdk 26) device. Enable "install from unknown sources" first.

## 4. Configure the backend URL (important!)

The debug build defaults to `http://10.0.2.2:4000` (emulator). For a real phone:

1. Deploy the backend somewhere public (Railway, Render, Fly.io, your VPS).
2. On GitHub → **Settings → Variables → Actions → New repository variable**

- Name: `MANDU_BACKEND_URL`
- Value: `https://your-backend.example.com`
3. Re-run the workflow. The APK will now point at your hosted backend.

## 5. Release (signed) APK — optional

1. Create a keystore locally:

```
keytool -genkey -v -keystore mandu-release.jks -keyalg RSA -keysize 2048 -validity 10000 -alias mandu
```
2. `base64 -w0 mandu-release.jks > keystore.b64` (Linux/macOS) or `certutil -encode` on Windows.
3. Add GitHub Secrets:

- `RELEASE_KEYSTORE_BASE64` — the file contents of `keystore.b64`
- `RELEASE_KEYSTORE_PASSWORD`
- `RELEASE_KEY_ALIAS`
- `RELEASE_KEY_PASSWORD`
4. Push a tag: `git tag v1.0.0 && git push origin v1.0.0`
5. The workflow will produce a signed `app-release.apk` artifact.

</BDS:create_file>

<!-- ============================ CI ============================ -->

<BDS:create_file fileName="ManduAI/.github/workflows/android-build.yml">

```yaml
name: Build Android APK

on:
  push:
    branches: [ main ]
    paths:
      - 'android/**'
      - '.github/workflows/android-build.yml'
  pull_request:
    branches: [ main ]
    paths:
      - 'android/**'
  workflow_dispatch:
  release:
    types: [ published ]

permissions:
  contents: read

jobs:
  build:
    name: Assemble Debug / Release
    runs-on: ubuntu-latest
    defaults:
      run:
        working-directory: android
    steps:
      - uses: actions/checkout@v4

      - name: Set up JDK 17
        uses: actions/setup-java@v4
        with:
          distribution: temurin
          java-version: '17'

      - name: Set up Android SDK
        uses: android-actions/setup-android@v3
        with:
          packages: 'platform-tools platforms;android-34 build-tools;34.0.0'

      - name: Setup Gradle
        uses: gradle/actions/setup-gradle@v3
        with:
          gradle-version: '8.7'

      - name: Decode release keystore
        if: ${{ github.event_name == 'release' || startsWith(github.ref, 'refs/tags/') }}
        env:
          KEYSTORE_B64: ${{ secrets.RELEASE_KEYSTORE_BASE64 }}
        run: |
          if [ -n "$KEYSTORE_B64" ]; then
            echo "$KEYSTORE_B64" | base64 -d > release.jks
            echo "RELEASE_KEYSTORE_PATH=$PWD/release.jks" >> $GITHUB_ENV
          fi

      - name: Build debug APK
        run: |
          gradle assembleDebug \
            -PMANDU_BACKEND_URL="${{ vars.MANDU_BACKEND_URL || 'http://10.0.2.2:4000' }}" \
            --no-daemon

      - name: Run unit tests
        run: gradle testDebugUnitTest --no-daemon || true

      - name: Build release APK (signed)
        if: ${{ env.RELEASE_KEYSTORE_PATH != '' }}
        env:
          KEYSTORE_PASSWORD: ${{ secrets.RELEASE_KEYSTORE_PASSWORD }}
          KEY_ALIAS: ${{ secrets.RELEASE_KEY_ALIAS }}
          KEY_PASSWORD: ${{ secrets.RELEASE_KEY_PASSWORD }}
        run: |
          gradle assembleRelease \
            -PMANDU_BACKEND_URL="${{ vars.MANDU_BACKEND_URL || 'https://mandu.example.com' }}" \
            -Pandroid.injected.signing.store.file="$RELEASE_KEYSTORE_PATH" \
            -Pandroid.injected.signing.store.password="$KEYSTORE_PASSWORD" \
            -Pandroid.injected.signing.key.alias="$KEY_ALIAS" \
            -Pandroid.injected.signing.key.password="$KEY_PASSWORD" \
            --no-daemon

      - name: Upload debug APK
        uses: actions/upload-artifact@v4
        with:
          name: mandu-debug-apk
          path: android/app/build/outputs/apk/debug/*.apk
          if-no-files-found: error

      - name: Upload release APK
        if: ${{ env.RELEASE_KEYSTORE_PATH != '' }}
        uses: actions/upload-artifact@v4
        with:
          name: mandu-release-apk
          path: android/app/build/outputs/apk/release/*.apk
          if-no-files-found: warn

