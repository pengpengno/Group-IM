# 💻 Group Electron IM 命令使用说明

本文档介绍了 **Group Electron IM** 项目中可用的 NPM 脚本及其用途。

## 🚀 桌面端应用命令 (Electron)

| 命令 | 描述 | 环境 |
| :--- | :--- | :--- |
| `npm run app:dev` | **推荐开发时使用**。启动带有监听模式（代码更改后自动重构）和 Chrome 开发工具的应用。 | `development` |
| `npm run app:start` | 构建项目并以接近生产的状态启动 Electron 应用。 | `production` |
| `npm run app:build` | 同时编译主进程和渲染进程，并生成 Protobuf 捆绑包。 | `N/A` |
| `npm run app:dist` | 将应用程序打包成可执行安装程序（使用 electron-builder）。 | `production` |

## 🌐 Web 端应用命令 (浏览器)

| 命令 | 描述 | 环境 |
| :--- | :--- | :--- |
| `npm run web:dev` | 启动具有热模块替换 (HMR) 功能的本地开发服务器。 | `development` |
| `npm run web:build` | 构建 Web 版本以进行生产部署。 | `production` |
| `npm run web:serve` | 使用简单的 Node.js 服务器提供之前构建的 Web 版本。 | `production` |

## 🛠 工具类命令

| 命令 | 描述 |
| :--- | :--- |
| `npm run sync-protos` | 拉取最新的 Protobuf 定义并生成 JavaScript/TypeScript 捆绑包。 |
| `npm run setup` | 安装系统依赖项并执行初步环境检查。 |
| `npm run kill-ports` | 强制关闭在常见开发端口（如 8080, 3000）运行的进程。 |

---

## ⚙️ 环境配置与覆盖 (Environment Overrides)

项目支持通过环境变量动态配置后端服务地址。在执行启动或构建命令前，可以使用 `cross-env` 进行覆盖。

**支持的变量：**
- `API_BASE`: REST API 基准地址 (默认: `http://localhost:8080`)
- `TCP_HOST`: Socket 服务主机地址 (默认: `localhost`)
- `TCP_PORT`: Socket 服务端口 (默认: `8088`)

**示例用法：**

```powershell
# 使用特定的生产后端地址启动桌面端
npx cross-env API_BASE=https://api.yourdomain.com TCP_HOST=socket.yourdomain.com npm run app:dev

# 构建生产环境 Web 版本
npx cross-env NODE_ENV=production API_BASE=https://api.yourdomain.com npm run web:build
```

---

> [!TIP]
> 每次启动开发环境时，终端都会显示一个友好的 **环境信息横幅**，包含系统元数据、项目状态以及任何生效的 **配置覆盖(Config Overrides)**。
