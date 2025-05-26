# 期货交易平台 - 离线部署包

## 📋 包含内容
- 完整的项目源代码
- 所有依赖包 (node_modules)
- Electron二进制文件 (offline-cache)
- 配置文件和安装脚本

## 💾 文件大小
总大小: 约 803MB

## 🚀 安装步骤

### 1. 复制到内网环境
将整个 "futures-trading-platform" 文件夹复制到内网目标机器

### 2. 进入项目目录
cd futures-trading-platform

### 3. 运行离线安装
Windows: 双击 install-offline.bat
或手动执行: npm install --prefer-offline --no-audit

### 4. 启动项目
npm run electron:serve

## 📋 系统要求
- Node.js 14.x 或更高版本
- Windows 10/11
- 至少 1GB 可用磁盘空间

## 🔧 故障排除
如果遇到问题:
1. 确保Node.js版本正确
2. 检查是否有足够磁盘空间
3. 以管理员身份运行安装脚本
4. 查看 OFFLINE-DEPLOYMENT-GUIDE.md 获取详细说明

## 📞 技术支持
详细文档请查看项目内的 OFFLINE-DEPLOYMENT-GUIDE.md 文件
