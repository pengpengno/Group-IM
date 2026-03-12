# 最简单方便的 Server 部署与 CI/CD 配置指南 (支持宝塔/原生)

这份指南教你如何将你的 **Group-IM Backend (Server)** 通过 GitHub Actions 自动化部署到你的服务器。
无论你是否使用“宝塔面板 (BT Panel)”，最终的部署逻辑都是使用 **Docker**。Docker 能够避免复杂的 Java、PostgreSQL、Redis 环境配置。

---

## 步骤 1：在你的 GitHub 仓库配置 Secrets (必需)

为了让 GitHub 知道如何登录你的服务器并推送代码，你需要在 GitHub 上配置秘钥。

1. 进入你项目的 **GitHub 页面**。
2. 点击上方的 `Settings` -> 左侧栏的 `Secrets and variables` -> 选择 `Actions`。
3. 点击 `New repository secret`，依次添加以下内容：

| 变量名 (Name) | 值示例 (Value) | 说明 |
| --- | --- | --- |
| `SERVER_HOST` | `123.45.67.89` | 你的服务器外网 IP 地址 |
| `SERVER_PORT` | `22` | 服务器的 SSH 端口 (一般是 22) |
| `SERVER_USERNAME`| `root` | 服务器用户名 |
| `SERVER_SSH_KEY` | `-----BEGIN OPENSSH PRIVATE KEY-----...` | 你的服务器 SSH 私钥。建议用私钥免密登录（请看底部常见问题）。如果只能用密码，请看底部说明或自己修改 yaml 文件使用密码插件。 |
| `TARGET_DIR` | `/www/wwwroot/Group-IM` | *(宝塔用户常用路径)* 你的项目在服务器上的绝对路径。 |

---

## 步骤 2：在服务器准备环境

### 👉 情况 A：使用宝塔面板 (推荐新手)
1. 登录宝塔面板后台。
2. 左侧点击 **“软件商店”** -> 搜索 **“Docker 管理器”**，点击安装。
3. 把本项目的代码上传到服务器。
   - 方式1：在宝塔 **“文件”** 里，进入 `/www/wwwroot/` 目录。
   - 方式2：点击上方“终端”，执行：
     ```bash
     cd /www/wwwroot/
     git clone https://github.com/你的名字/Group-IM.git
     ```
   *(此时你的 `TARGET_DIR` 就是 `/www/wwwroot/Group-IM`)*
4. 如果宝塔预装了 PostgreSQL/Redis，请**确保它们不会跟你 Docker 中的 5432 / 6379 端口冲突**！如果有冲突，可以在宝塔面板中关掉宝塔版本的服务，或者在项目中修改映射端口。

### 👉 情况 B：不使用宝塔面板 (原生 Linux 系统)
1. 通过 SSH 登录你的服务器：`ssh root@123.45.67.89`
2. 使用官方懒人命令安装 Docker 和 Docker-Compose：
   ```bash
   curl -fsSL https://get.docker.com | bash
   apt install docker-compose -y  # Ubuntu/Debian系统
   # 或者
   yum install docker-compose -y  # CentOS系统
   ```
3. 拉取项目代码到 `/opt`（或者其他目录，请与上面的 `TARGET_DIR` 对应）：
   ```bash
   cd /opt
   git clone https://github.com/你的名字/Group-IM.git
   ```

---

## 步骤 3：验证与第一次手动拉取 (重点)

此时，每次你把代码推送到 `main` 分支时，GitHub 就会自动运行编译构建流程，并且在最后一步登录你的服务器，启动项目。

> **⚠️ 特别注意：** 咱们项目的 `docker-compose.cicd.yml` 中用到了 GitHub 的 `ghcr.io` 镜像仓库。首次在服务器上拉取私有/公开镜像时，最好做一次测试。

1. 在你的代码编辑器中（你刚刚本地的修改），将我为你添加的 `.github/workflows/deploy-server.yml` 和 `deploy/docker/docker-compose.cicd.yml` **提交并 Push 到 GitHub**。
2. 观察你的 GitHub 仓库的 **Actions** 面板，等待 `Build and push` 这个任务变绿（完成）。此时表示你的 Docker 镜像已经成功打包。
3. （如果是公开库就不需要登录）如果是**私有库**，请在你的服务器终端运行登录命令：
    ```bash
    # 在服务器终端输入以下命令：
    docker login ghcr.io -u 你的github用户名
    # 密码输入你的 GitHub Personal Access Token (PAT)。
    ```
4. 如果你在 Actions 里看到 `Deploy` 这个任务也成功了，说明已经部署成功！

---

## 步骤 4：如何访问你的服务？

因为宝塔天然拥有 Nginx，所以最简单的做法就是利用宝塔的网站服务：

1. 在宝塔中新建一个 **“纯静态网站”**，域名填你绑定的域名（比如 `api.你的域名.com`）。
2. 在该网站设置中，点击 **“反向代理”**。
3. 添加一条代理规则，代理目标设置为：`http://127.0.0.1:8080` (这是因为我们在 docker 里把 8080 端口暴露出来了)。
4. 如果你需要配置 TCP 端口 (`8088`)，由于宝塔直接管理 Nginx，你需要在宝塔左侧的 **“项目” / “安全”** 中放行 `8088`，并在需要的情况下直接在宝塔里为 Nginx 追加 WebSocket 支持。

如果是原生环境，你可以直接用服务器 IP 加上端口就可以访问：
- HTTP API：`http://服务器IP:8080`
- TCP 端口：`服务器IP:8088`

---

## 常见问题 (FAQ)

### 1. SSH 私钥 `SERVER_SSH_KEY` 是什么？怎么获取？
如果你是用密码登录服务器的，强烈建议你改成密钥登录（更安全配合CI）。
可以在你自己的电脑上生成密钥：
```bash
ssh-keygen -t rsa -b 4096 -C "deploy"
```
这会产生两个文件，比如 `id_rsa` 和 `id_rsa.pub`。
- 将 `id_rsa` （私钥）的全部内容复制，粘贴到 GitHub 的 `SERVER_SSH_KEY` 中。
- 将 `id_rsa.pub` （公钥）的内容，添加到服务器里的 `~/.ssh/authorized_keys` 文件中。

### 2. Actions 报错连接不到服务器 `ssh: handshake failed`
请检查：
1. `SERVER_HOST` 是否正确？
2. `SERVER_PORT` 是否被宝塔安全机制、云服务商(如阿里云安全组)拦截了？默认 SSH 是 22，确保是通的。
3. 私钥格式是否完整（包含 `BEGIN` 和 `END` 行）。

### 3. 如何查看日志？
如果网页打不开，你想看看后端 Java 到底有没有跑起来，在服务器运行：
```bash
# 假设你在 /www/wwwroot/Group-IM 目录下
docker-compose -f deploy/docker/docker-compose.cicd.yml logs -f server
```

### 4. 为什么要修改 `docker-compose.cicd.yml` 而不是用原本的 `prod` 文件？
因为 `prod` 里面包含了让你服务器本地执行 `build` 打包（尤其是 Native 镜像打包）的步骤，这会占用极大的 CPU 和内存。在 CI/CD 下，我们将打包任务交给了 Github 免费云端处理，服务器只需负责**运行**（拉取已经打包好的镜像运行）即可。这就是最“简单方便”的方案！
