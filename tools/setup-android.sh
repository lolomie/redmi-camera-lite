#!/usr/bin/env bash
set -euo pipefail
# Keep SDK/caches outside the checkout. No credentials or verification bypasses.
task_repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
source "$task_repo_dir/tools/cloud-env.sh"
export ANDROID_HOME="${ANDROID_HOME:-/workspace/android-sdk}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/workspace/gradle-cache}"
export ANDROID_USER_HOME="${ANDROID_USER_HOME:-/workspace/android-user}"
task_gradle_dir=/workspace/toolchains/gradle-8.11.1
mkdir -p /workspace/toolchains "$ANDROID_HOME/cmdline-tools" "$GRADLE_USER_HOME" "$ANDROID_USER_HOME"
task_download_dir="$(mktemp -d /tmp/redmi-android-setup.XXXXXX)"
if [[ ! -x /workspace/toolchains/jdk-21/bin/javac ]]; then
    curl --fail --location --retry 2 https://download.oracle.com/java/21/archive/jdk-21.0.12.1_linux-x64_bin.tar.gz -o "$task_download_dir/jdk.tar.gz"
    printf '%s  %s\n' 12f870b21301b42292558a3f872ce543affa2b86cb6458591c78388c41ddb111 "$task_download_dir/jdk.tar.gz" | sha256sum --check
    mkdir -p /workspace/toolchains/jdk-21
    tar -xzf "$task_download_dir/jdk.tar.gz" -C /workspace/toolchains/jdk-21 --strip-components=1
fi
export JAVA_HOME=/workspace/toolchains/jdk-21
export PATH="$JAVA_HOME/bin:$PATH"
if [[ ! -x "$task_gradle_dir/bin/gradle" ]]; then
    curl --fail --location --retry 2 https://services.gradle.org/distributions/gradle-8.11.1-bin.zip -o "$task_download_dir/gradle.zip"
    printf '%s  %s\n' f397b287023acdba1e9f6fc5ea72d22dd63669d59ed4a289a29b1a76eee151c6 "$task_download_dir/gradle.zip" | sha256sum --check
    unzip -q "$task_download_dir/gradle.zip" -d /workspace/toolchains
fi
if [[ ! -x "$ANDROID_HOME/cmdline-tools/19.0/bin/sdkmanager" ]]; then
    curl --fail --location --retry 2 https://dl.google.com/android/repository/commandlinetools-linux-13114758_latest.zip -o "$task_download_dir/cmdline.zip"
    # Official repository2-1.xml checksum, fetched over verified TLS.
    printf '%s  %s\n' 5fdcc763663eefb86a5b8879697aa6088b041e70 "$task_download_dir/cmdline.zip" | sha1sum --check
    unzip -q "$task_download_dir/cmdline.zip" -d "$task_download_dir/sdk"
    mv "$task_download_dir/sdk/cmdline-tools" "$ANDROID_HOME/cmdline-tools/19.0"
fi
export PATH="$ANDROID_HOME/cmdline-tools/19.0/bin:$ANDROID_HOME/platform-tools:$task_gradle_dir/bin:$PATH"
# sdkmanager can close stdin after its last prompt: accept yes SIGPIPE only.
set +o pipefail
yes 2>/dev/null | sdkmanager --sdk_root="$ANDROID_HOME" --licenses
task_sdk_status=${PIPESTATUS[1]}
set -o pipefail
[[ "$task_sdk_status" == 0 ]]
sdkmanager --sdk_root="$ANDROID_HOME" 'platforms;android-35' 'build-tools;35.0.0' 'platform-tools'
cd "$task_repo_dir"
./gradlew --no-daemon --version
printf 'SDK and Gradle installed. Build with ANDROID_HOME=%s GRADLE_USER_HOME=%s ./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug\n' "$ANDROID_HOME" "$GRADLE_USER_HOME"
