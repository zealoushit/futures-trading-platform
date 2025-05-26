@echo off
echo 开始离线安装...

REM 设置离线缓存目录
set ELECTRON_CACHE=%cd%\offline-cache
set ELECTRON_MIRROR=file:///%cd%/offline-cache/

REM 安装依赖
npm install --prefer-offline --no-audit

echo 离线安装完成！
pause