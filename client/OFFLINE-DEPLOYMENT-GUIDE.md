# 📦 离线部署指南

## 🎯 当前状态
✅ **依赖下载完成！** 已成功下载以下文件：

### 📁 下载的文件清单
```
offline-cache/
├── electron-v13.6.9-win32-x64.zip    (79MB) - Windows 64位
├── electron-v13.6.9-win32-ia32.zip   (75MB) - Windows 32位
└── electron-v13.6.9-win32-arm64.zip  (83MB) - Windows ARM64
```

**总大小**: 约 237MB

## 📋 需要复制到内网的文件

### 🔥 必须复制的文件和目录

```
futures-trading-platform/client/
├── 📁 src/                    # 源代码目录
├── 📁 public/                 # 静态资源
├── 📁 offline-cache/          # ⭐ 下载的Electron二进制文件
├── 📁 node_modules/           # ⭐ 已安装的依赖包
├── 📄 package.json            # 项目配置
├── 📄 package-lock.json       # 依赖锁定文件
├── 📄 .npmrc                  # npm配置文件
├── 📄 vue.config.js           # Vue配置
├── 📄 babel.config.js         # Babel配置
├── 📄 jsconfig.json           # JS配置
├── 📄 install-offline.bat     # ⭐ 离线安装脚本
└── 📄 setup-mirrors.bat       # 镜像源配置脚本
```

### ❌ 可以忽略的文件
```
❌ *.md                        # 文档文件
❌ api-test.html              # 测试文件
❌ dist_electron.zip          # 旧的构建文件
❌ download-dependencies.js   # 下载脚本(已完成任务)
❌ create-offline-package.js  # 打包脚本
❌ Dockerfile                 # Docker文件
❌ docker-compose.yml         # Docker配置
```

## 🚀 内网部署步骤

### 第一步：复制文件到内网
1. **压缩整个client目录**
   ```bash
   # 在有网络的环境
   tar -czf futures-trading-platform-offline.tar.gz client/
   # 或使用WinRAR/7zip压缩
   ```

2. **传输到内网环境**
   - 使用U盘、移动硬盘
   - 或通过内网文件传输

3. **在内网环境解压**
   ```bash
   tar -xzf futures-trading-platform-offline.tar.gz
   cd client/
   ```

### 第二步：内网安装
```bash
# Windows环境
install-offline.bat

# 或手动执行
set ELECTRON_CACHE=%cd%\offline-cache
set ELECTRON_MIRROR=file:///%cd%/offline-cache/
npm install --prefer-offline --no-audit
```

### 第三步：启动项目
```bash
# 开发模式
npm run electron:serve

# 或Web模式
npm run serve
```

## 🔍 验证安装

### 检查关键文件
```bash
# 检查Electron缓存
ls -la offline-cache/

# 检查node_modules
ls -la node_modules/electron/

# 检查package.json
cat package.json
```

### 测试启动
```bash
# 测试命令是否可用
npm run --help

# 检查electron版本
npx electron --version
```

## 🛠️ 故障排除

### 问题1：Electron下载失败
```bash
# 解决方案：手动设置缓存路径
set ELECTRON_CACHE=%cd%\offline-cache
set ELECTRON_CUSTOM_DIR=%cd%\offline-cache
npm install electron --force
```

### 问题2：权限问题
```bash
# Windows: 以管理员身份运行
# Linux: 修改权限
chmod -R 755 offline-cache/
```

### 问题3：路径问题
```bash
# 确保在正确目录
pwd
ls -la package.json
```

## 📊 文件大小统计

| 目录/文件 | 大小 | 说明 |
|-----------|------|------|
| node_modules/ | ~500MB | 已安装的依赖 |
| offline-cache/ | ~237MB | Electron二进制 |
| src/ | ~50KB | 源代码 |
| public/ | ~10KB | 静态资源 |
| **总计** | **~740MB** | **完整项目** |

## ✅ 成功标志

安装成功后，你应该看到：
1. ✅ `npm run electron:serve` 能正常启动
2. ✅ Electron窗口正常打开
3. ✅ 应用界面正常显示
4. ✅ 无网络错误提示

## 📞 技术支持

如果遇到问题，请检查：
1. **Node.js版本**: 需要14.x或更高
2. **文件完整性**: 确保所有文件都已复制
3. **权限设置**: 确保有执行权限
4. **路径正确**: 在正确的目录执行命令

---

🎉 **恭喜！** 你已经成功准备好了离线部署包！
