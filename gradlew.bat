@echo off
setlocal
set "APP_HOME=%~dp0"
set "JAR=%APP_HOME%gradle\wrapper\gradle-wrapper.jar"
set "URL=https://raw.githubusercontent.com/gradle/gradle/v9.6.0/gradle/wrapper/gradle-wrapper.jar"
set "EXPECTED=497c8c2a7e5031f6aa847f88104aa80a93532ec32ee17bdb8d1d2f67a194a9c7"

if not exist "%JAR%" (
  powershell -NoProfile -ExecutionPolicy Bypass -Command "$ProgressPreference='SilentlyContinue'; Invoke-WebRequest -Uri '%URL%' -OutFile '%JAR%.tmp'; $h=(Get-FileHash -Algorithm SHA256 '%JAR%.tmp').Hash.ToLower(); if ($h -ne '%EXPECTED%') { Remove-Item '%JAR%.tmp' -Force; exit 42 }; Move-Item '%JAR%.tmp' '%JAR%' -Force"
  if errorlevel 1 (
    echo Failed to bootstrap the Gradle wrapper or checksum validation failed.
    exit /b 1
  )
)

java -classpath "%JAR%" org.gradle.wrapper.GradleWrapperMain %*
endlocal
