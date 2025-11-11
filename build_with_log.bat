@echo off
setlocal
set JAVA_HOME=C:\Program Files\Android\Android Studio\jbr
set PATH=%JAVA_HOME%\bin;%PATH%
echo Starting build at %DATE% %TIME% > "C:\Users\zanee\AppDevAttendance\build_output.txt"
"C:\Users\zanee\AppDevAttendance\gradlew.bat" clean assembleDebug >> "C:\Users\zanee\AppDevAttendance\build_output.txt" 2>&1
echo Build completed at %DATE% %TIME% >> "C:\Users\zanee\AppDevAttendance\build_output.txt"
echo Exit code: %ERRORLEVEL% >> "C:\Users\zanee\AppDevAttendance\build_output.txt"
type "C:\Users\zanee\AppDevAttendance\build_output.txt"
