@echo off
echo 配置内网镜像源...

REM 设置npm镜像源
npm config set registry https://registry.npmmirror.com/

REM 设置electron镜像源
npm config set electron_mirror https://npmmirror.com/mirrors/electron/
npm config set electron_builder_binaries_mirror https://npmmirror.com/mirrors/electron-builder-binaries/

REM 设置其他镜像源
npm config set chromedriver_cdnurl https://npmmirror.com/mirrors/chromedriver/
npm config set operadriver_cdnurl https://npmmirror.com/mirrors/operadriver/
npm config set phantomjs_cdnurl https://npmmirror.com/mirrors/phantomjs/
npm config set selenium_cdnurl https://npmmirror.com/mirrors/selenium/

REM 设置环境变量
set ELECTRON_MIRROR=https://npmmirror.com/mirrors/electron/
set ELECTRON_BUILDER_BINARIES_MIRROR=https://npmmirror.com/mirrors/electron-builder-binaries/

echo 镜像源配置完成！
echo 现在可以运行: npm install

pause
