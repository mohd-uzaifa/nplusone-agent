#!/bin/sh
MAVEN_PROJECTBASEDIR="$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)"
WRAPPER_JAR="$MAVEN_PROJECTBASEDIR/.mvn/wrapper/maven-wrapper.jar"
if [ ! -f "$WRAPPER_JAR" ]; then
  echo "Maven Wrapper JAR is missing: $WRAPPER_JAR" >&2
  exit 1
fi
JAVA_EXE="${JAVA_HOME:+$JAVA_HOME/bin/}java"
exec "$JAVA_EXE" -Dmaven.multiModuleProjectDirectory="$MAVEN_PROJECTBASEDIR" -classpath "$WRAPPER_JAR" org.apache.maven.wrapper.MavenWrapperMain "$@"