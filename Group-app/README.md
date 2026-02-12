# Group 项目开发指南

## 项目结构
```
Group-app/                  # 根目录
├── Group/                 # KMP 主项目
│   ├── composeApp/       # 主应用模块
│   ├── build.gradle.kts  # 构建配置
│   └── ...               # 其他文件
├── Group-Electronjs/     # Electron 桌面端
└── doc/                  # 文档目录
```

## 快捷操作方式

### 方法1: 使用包装脚本 (推荐)
在根目录 `Group-app` 下直接运行：

```bash
# Windows
group.bat assembleDebug
group.bat clean
group.bat test

# Linux/Mac
./group.sh assembleDebug
./group.sh clean
./group.sh test
```

### 方法2: 直接使用 gradlew
在根目录下：
```bash
gradlew.bat -p Group assembleDebug
```

### 方法3: 进入子目录操作
```bash
cd Group
gradlew.bat assembleDebug
```

## 常用命令

### 构建相关
```bash
# 构建 Debug 版本
group.bat assembleDebug

# 构建 Release 版本  
group.bat assembleRelease

# 清理构建
group.bat clean

# 构建并安装到设备
group.bat installDebug
```

### 测试相关
```bash
# 运行单元测试
group.bat test

# 运行 Android instrumentation 测试
group.bat connectedAndroidTest

# 运行特定测试
group.bat testDebugUnitTest --tests "*ViewModelTest"
```

### 开发相关
```bash
# 依赖检查
group.bat dependencies

# 任务列表
group.bat tasks

# 项目信息
group.bat projects
```

## IDE 配置

### Android Studio
1. 打开 `Group-app/Group` 目录作为项目根目录
2. 或者打开根目录，但需要在运行配置中指定工作目录为 `Group`

### VS Code
推荐使用工作区配置，在 `.vscode/settings.json` 中设置：
```json
{
    "java.project.referencedLibraries": [
        "Group/**/*"
    ]
}
```

## 注意事项

1. 所有 Gradle 命令都应该在 `Group` 目录或通过包装脚本执行
2. 根目录的配置文件是代理文件，实际配置在 `Group` 子目录中
3. 编译输出在 `Group/build` 目录下
4. 如遇到路径问题，请确保使用绝对路径或相对路径正确引用

## 故障排除

如果遇到 "Directory does not contain a Gradle build" 错误：
1. 确保在正确的目录执行命令
2. 使用包装脚本 `group.bat` 或 `group.sh`
3. 检查 `Group/settings.gradle.kts` 文件是否存在