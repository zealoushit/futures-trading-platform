@echo off
echo Building Femas Trading JNI Library...

REM 设置环境变量
set JAVA_HOME=C:\Program Files\Java\jdk1.8.0_XXX
set FEMAS_API_PATH=C:\Femas\1.32飞马交易api\FEMF_V1.32.r2_03a_Api_Win64_20231025_09c84740d\femasapi
set MSVC_PATH=C:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Tools\MSVC\14.29.30133

REM 检查路径
if not exist "%JAVA_HOME%" (
    echo Error: JAVA_HOME not found: %JAVA_HOME%
    echo Please update JAVA_HOME in this script
    pause
    exit /b 1
)

if not exist "%FEMAS_API_PATH%" (
    echo Error: Femas API path not found: %FEMAS_API_PATH%
    echo Please update FEMAS_API_PATH in this script
    pause
    exit /b 1
)

REM 创建目录
if not exist "jni\headers" mkdir jni\headers
if not exist "jni\cpp" mkdir jni\cpp
if not exist "lib" mkdir lib
if not exist "target\classes" mkdir target\classes

echo.
echo Step 1: Compiling Java classes...
"%JAVA_HOME%\bin\javac" -cp "." -d "target/classes" src/main/java/com/trading/jni/FemasTraderApi.java
if %ERRORLEVEL% neq 0 (
    echo Error: Java compilation failed
    pause
    exit /b 1
)

"%JAVA_HOME%\bin\javac" -cp "." -d "target/classes" src/main/java/com/trading/jni/FemasMarketApi.java
if %ERRORLEVEL% neq 0 (
    echo Error: Java compilation failed
    pause
    exit /b 1
)

echo.
echo Step 2: Generating JNI headers...
"%JAVA_HOME%\bin\javah" -cp "target/classes" -d "jni/headers" -jni com.trading.jni.FemasTraderApi
if %ERRORLEVEL% neq 0 (
    echo Error: JNI header generation failed
    pause
    exit /b 1
)

"%JAVA_HOME%\bin\javah" -cp "target/classes" -d "jni/headers" -jni com.trading.jni.FemasMarketApi
if %ERRORLEVEL% neq 0 (
    echo Error: JNI header generation failed
    pause
    exit /b 1
)

echo.
echo Step 3: Setting up Visual Studio environment...
call "%MSVC_PATH%\..\..\..\..\Tools\VsDevCmd.bat"

echo.
echo Step 4: Compiling C++ JNI library...

REM 设置编译参数
set INCLUDE_PATHS=-I"%JAVA_HOME%\include" -I"%JAVA_HOME%\include\win32" -I"%FEMAS_API_PATH%\include" -I"jni\headers" -I"jni\cpp"
set LIB_PATHS=/LIBPATH:"%FEMAS_API_PATH%\lib"
set LIBS=USTPtraderapi.lib USTPmdapi.lib

REM 编译交易API JNI库
cl /LD /MD %INCLUDE_PATHS% jni\cpp\FemasTraderJNI.cpp /Fe:lib\FemasTraderJNI.dll /link %LIB_PATHS% %LIBS%
if %ERRORLEVEL% neq 0 (
    echo Error: C++ compilation failed for FemasTraderJNI
    pause
    exit /b 1
)

echo.
echo Step 5: Copying required DLL files...
copy "%FEMAS_API_PATH%\lib\USTPtraderapi.dll" lib\
copy "%FEMAS_API_PATH%\lib\USTPmdapi.dll" lib\

echo.
echo Build completed successfully!
echo.
echo Generated files:
echo - lib\FemasTraderJNI.dll
echo - lib\USTPtraderapi.dll  
echo - lib\USTPmdapi.dll
echo - jni\headers\com_trading_jni_FemasTraderApi.h
echo - jni\headers\com_trading_jni_FemasMarketApi.h
echo.
echo Next steps:
echo 1. Add lib\ directory to java.library.path
echo 2. Update application.yml with correct connection parameters
echo 3. Run the Java application
echo.

pause
