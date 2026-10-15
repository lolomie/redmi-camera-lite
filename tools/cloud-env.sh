#!/usr/bin/env bash
# Source in Codex cloud shells. Normal Android Studio/CI builds do not need this.
export ANDROID_HOME="${ANDROID_HOME:-/workspace/android-sdk}"
export ANDROID_USER_HOME="${ANDROID_USER_HOME:-/workspace/android-user}"
export GRADLE_USER_HOME="${GRADLE_USER_HOME:-/workspace/gradle-cache}"
if [[ -x /workspace/toolchains/jdk-21/bin/javac ]]; then
    export JAVA_HOME=/workspace/toolchains/jdk-21
    export PATH="$JAVA_HOME/bin:$PATH"
fi
if [[ -z "${REDMI_JAVA_HOME_CONFIGURED:-}" ]]; then
    export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Duser.home=$ANDROID_USER_HOME"
    export REDMI_JAVA_HOME_CONFIGURED=1
fi
export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/cmdline-tools/19.0/bin:/workspace/toolchains/gradle-8.11.1/bin:$PATH"
# Java does not consume HTTPS_PROXY itself. Only hostname/port are propagated;
# credentials remain with the platform proxy and are never copied into files.
if [[ -n "${HTTPS_PROXY:-}" && -z "${REDMI_JAVA_PROXY_CONFIGURED:-}" ]]; then
    read -r task_proxy_host task_proxy_port < <(node -e 'const u=new URL(process.env.HTTPS_PROXY); console.log(u.hostname+" "+(u.port||80))')
    export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:-} -Dhttps.proxyHost=$task_proxy_host -Dhttps.proxyPort=$task_proxy_port -Dhttp.proxyHost=$task_proxy_host -Dhttp.proxyPort=$task_proxy_port"
    if [[ -f "${CODEX_PROXY_CERT:-}" ]]; then
        task_truststore=/workspace/toolchains/redmi-java-cacerts
        if [[ ! -f "$task_truststore" ]]; then
            mkdir -p /workspace/toolchains
            cp /etc/ssl/certs/java/cacerts "$task_truststore"
            keytool -importcert -noprompt -alias codex-cloud-proxy -file "$CODEX_PROXY_CERT" -keystore "$task_truststore" -storepass changeit >/dev/null
        fi
        export JAVA_TOOL_OPTIONS="$JAVA_TOOL_OPTIONS -Djavax.net.ssl.trustStore=$task_truststore -Djavax.net.ssl.trustStorePassword=changeit"
    fi
    export REDMI_JAVA_PROXY_CONFIGURED=1
fi
