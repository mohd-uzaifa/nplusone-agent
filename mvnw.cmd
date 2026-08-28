@ECHO OFF
SETLOCAL
SET "MAVEN_PROJECTBASEDIR=%~dp0."
SET "WRAPPER_JAR=%MAVEN_PROJECTBASEDIR%\.mvn\wrapper\maven-wrapper.jar"
IF NOT EXIST "%WRAPPER_JAR%" (
  ECHO Maven Wrapper JAR is missing: %WRAPPER_JAR%
  EXIT /B 1
)
SET "JAVA_EXE=java.exe"
IF NOT "%JAVA_HOME%"=="" SET "JAVA_EXE=%JAVA_HOME%\bin\java.exe"
"%JAVA_EXE%" "-Dmaven.multiModuleProjectDirectory=%MAVEN_PROJECTBASEDIR%" -classpath "%WRAPPER_JAR%" org.apache.maven.wrapper.MavenWrapperMain %*
ENDLOCAL