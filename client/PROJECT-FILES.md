# 📁 项目文件清单

## 🎯 离线部署相关文件

### 📦 核心部署文件
```
client/
├── 📁 offline-deployment/              # 完整离线部署包 (803MB)
│   └── futures-trading-platform/       # 可直接复制到内网的项目
├── 📁 offline-cache/                   # Electron二进制文件 (237MB)
│   ├── electron-v13.6.9-win32-x64.zip
│   ├── electron-v13.6.9-win32-ia32.zip
│   └── electron-v13.6.9-win32-arm64.zip
└── 📁 node_modules/                    # 已安装的依赖包 (~500MB)
```

### 🛠️ 工具脚本
```
client/
├── 📄 download-dependencies.js         # 依赖下载脚本
├── 📄 install-offline.bat             # 离线安装脚本
├── 📄 setup-mirrors.bat               # 镜像源配置脚本
└── 📄 .npmrc                          # npm镜像源配置
```

### 📚 文档文件
```
client/
├── 📄 OFFLINE-DEPLOYMENT-GUIDE.md     # 详细部署指南
├── 📄 DEPLOYMENT-SUMMARY.md           # 部署总结
├── 📄 PROJECT-FILES.md                # 本文件清单
└── 📄 offline-deployment/README.txt   # 简化使用说明
```

### 🔧 项目核心文件
```
client/
├── 📁 src/                            # Vue源代码
├── 📁 public/                         # 静态资源
├── 📄 package.json                    # 项目配置
├── 📄 package-lock.json               # 依赖锁定
├── 📄 vue.config.js                   # Vue配置
├── 📄 babel.config.js                 # Babel配置
└── 📄 jsconfig.json                   # JS配置
```

## 🚀 内网部署使用

### 方式一：使用完整离线包
```bash
# 1. 复制到内网
cp -r offline-deployment/futures-trading-platform/ /target/path/

# 2. 进入目录并安装
cd /target/path/futures-trading-platform/
install-offline.bat

# 3. 启动项目
npm run electron:serve
```

### 方式二：压缩传输
```bash
# 1. 压缩离线包
tar -czf futures-platform-offline.tar.gz offline-deployment/

# 2. 传输到内网后解压
tar -xzf futures-platform-offline.tar.gz
cd offline-deployment/futures-trading-platform/

# 3. 安装并启动
install-offline.bat
npm run electron:serve
```

## 📊 文件大小统计

| 文件/目录 | 大小 | 用途 |
|-----------|------|------|
| offline-deployment/ | 803MB | 完整离线部署包 |
| offline-cache/ | 237MB | Electron二进制文件 |
| node_modules/ | ~500MB | 项目依赖包 |
| src/ | ~50KB | 源代码 |
| 配置文件 | ~5MB | 各种配置 |

## ✅ 已清理的文件

以下文件已被删除，不再需要：
- ❌ `Dockerfile` - Docker部署文件
- ❌ `docker-compose.yml` - Docker编排文件  
- ❌ `create-offline-package.js` - 复杂打包脚本
- ❌ `prepare-offline-copy.bat` - 文件复制脚本

## 🎯 推荐使用

**最简单的内网部署方式：**
1. 将 `offline-deployment/futures-trading-platform/` 整个文件夹复制到内网
2. 运行 `install-offline.bat`
3. 运行 `npm run electron:serve`

**总传输大小：803MB**
**安装时间：约2-5分钟**
**启动时间：约30秒**
