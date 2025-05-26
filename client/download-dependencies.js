const fs = require('fs');
const path = require('path');
const https = require('https');
const { execSync } = require('child_process');

// Electron版本配置
const ELECTRON_VERSION = '13.6.9';
const PLATFORMS = ['win32-x64', 'win32-ia32', 'win32-arm64'];

// 下载目录
const DOWNLOAD_DIR = path.join(__dirname, 'offline-cache');

// 创建下载目录
if (!fs.existsSync(DOWNLOAD_DIR)) {
    fs.mkdirSync(DOWNLOAD_DIR, { recursive: true });
}

// 下载文件函数
function downloadFile(url, dest) {
    return new Promise((resolve, reject) => {
        console.log(`下载: ${url}`);
        const file = fs.createWriteStream(dest);
        
        https.get(url, (response) => {
            if (response.statusCode === 302 || response.statusCode === 301) {
                // 处理重定向
                return downloadFile(response.headers.location, dest)
                    .then(resolve)
                    .catch(reject);
            }
            
            if (response.statusCode !== 200) {
                reject(new Error(`下载失败: ${response.statusCode}`));
                return;
            }
            
            response.pipe(file);
            
            file.on('finish', () => {
                file.close();
                console.log(`下载完成: ${path.basename(dest)}`);
                resolve();
            });
            
            file.on('error', (err) => {
                fs.unlink(dest, () => {});
                reject(err);
            });
        }).on('error', reject);
    });
}

// 主下载函数
async function downloadElectronBinaries() {
    console.log('开始下载Electron二进制文件...');
    
    for (const platform of PLATFORMS) {
        const filename = `electron-v${ELECTRON_VERSION}-${platform}.zip`;
        const url = `https://npmmirror.com/mirrors/electron/${ELECTRON_VERSION}/${filename}`;
        const dest = path.join(DOWNLOAD_DIR, filename);
        
        if (fs.existsSync(dest)) {
            console.log(`文件已存在，跳过: ${filename}`);
            continue;
        }
        
        try {
            await downloadFile(url, dest);
        } catch (error) {
            console.error(`下载失败 ${filename}:`, error.message);
        }
    }
    
    console.log('Electron二进制文件下载完成！');
}

// 创建离线安装脚本
function createOfflineInstallScript() {
    const scriptContent = `@echo off
echo 开始离线安装...

REM 设置离线缓存目录
set ELECTRON_CACHE=%cd%\\offline-cache
set ELECTRON_MIRROR=file:///%cd%/offline-cache/

REM 安装依赖
npm install --prefer-offline --no-audit

echo 离线安装完成！
pause`;

    fs.writeFileSync(path.join(__dirname, 'install-offline.bat'), scriptContent);
    console.log('离线安装脚本已创建: install-offline.bat');
}

// 执行下载
async function main() {
    try {
        await downloadElectronBinaries();
        createOfflineInstallScript();
        
        console.log('\n=== 下载完成 ===');
        console.log('1. 将整个项目文件夹复制到内网环境');
        console.log('2. 运行 install-offline.bat 进行离线安装');
        console.log('3. 运行 npm run electron:serve 启动项目');
        
    } catch (error) {
        console.error('下载过程中出现错误:', error);
    }
}

// 如果直接运行此脚本
if (require.main === module) {
    main();
}

module.exports = { downloadElectronBinaries, createOfflineInstallScript };
