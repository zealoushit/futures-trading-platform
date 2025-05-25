@echo off
echo Generating JNI Headers for Femas Trading API...

REM 设置环境变量
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_XXX
set PATH=%JAVA_HOME%\bin;%PATH%

REM 创建输出目录
if not exist "jni" mkdir jni
if not exist "jni\headers" mkdir jni\headers
if not exist "jni\cpp" mkdir jni\cpp
if not exist "lib" mkdir lib

echo.
echo Step 1: Compiling Java classes...
javac -cp "." -d "target/classes" src/main/java/com/trading/jni/FemasTraderApi.java
if %ERRORLEVEL% neq 0 (
    echo Error: Java compilation failed
    pause
    exit /b 1
)

echo.
echo Step 2: Generating JNI headers...
javah -cp "target/classes" -d "jni/headers" -jni com.trading.jni.FemasTraderApi
if %ERRORLEVEL% neq 0 (
    echo Error: JNI header generation failed
    pause
    exit /b 1
)

echo.
echo Step 3: Generating additional headers for Market Data API...
javah -cp "target/classes" -d "jni/headers" -jni com.trading.jni.FemasMarketApi
if %ERRORLEVEL% neq 0 (
    echo Warning: Market Data API header generation failed (may not exist yet)
)

echo.
echo JNI Headers generated successfully!
echo Headers location: jni/headers/
echo.
echo Next steps:
echo 1. Implement C++ wrapper in jni/cpp/
echo 2. Compile C++ library
echo 3. Place DLL files in lib/ directory
echo.

dir jni\headers

pause
