#!/usr/bin/env sh
DIR="$(cd "$(dirname "$0")" && pwd)"
JAVA_EXEC="${JAVA_HOME:+$JAVA_HOME/bin/}java"
if [ ! -x "$JAVA_EXEC" ]; then
  JAVA_EXEC="java"
fi
exec "$JAVA_EXEC" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew -classpath "$DIR/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
