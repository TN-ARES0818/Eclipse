#!/bin/sh
##############################################################################
#  Eclipse Browser - Gradle Start Script
#
#  CLOUD VS CODE FIX:
#  Cloud environments set JAVA_TOOL_OPTIONS=-Xmx64m which conflicts with
#  Gradle's own JVM args. We unset it before running.
##############################################################################

# ── UNSET CONFLICTING ENV VARS (fixes -Xmx64m error) ──────────────────────
unset JAVA_TOOL_OPTIONS
unset _JAVA_OPTIONS
unset JDK_JAVA_OPTIONS
export JAVA_OPTS=""
# ──────────────────────────────────────────────────────────────────────────

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)

CLASSPATH="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# Determine Java
if [ -n "$JAVA_HOME" ]; then
    JAVACMD="$JAVA_HOME/bin/java"
    if [ ! -x "$JAVACMD" ]; then
        echo "ERROR: JAVA_HOME is set but java not found at $JAVACMD" >&2
        exit 1
    fi
else
    JAVACMD="java"
    command -v java >/dev/null 2>&1 || { echo "ERROR: java not found in PATH" >&2; exit 1; }
fi

exec "$JAVACMD" \
    -Xmx2048m \
    -Dfile.encoding=UTF-8 \
    -Duser.country=US \
    -Duser.language=en \
    -classpath "$CLASSPATH" \
    org.gradle.wrapper.GradleWrapperMain \
    "$@"
