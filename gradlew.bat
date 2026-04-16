@ECHO OFF
SETLOCAL
SET DIR=%~dp0
SET JAVA_EXEC=%JAVA_HOME%\bin\java.exe
IF NOT EXIST "%JAVA_EXEC%" SET JAVA_EXEC=java
"%JAVA_EXEC%" -Xmx64m -Xms64m -Dorg.gradle.appname=gradlew -classpath "%DIR%gradle\wrapper\gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain %*
ENDLOCAL
