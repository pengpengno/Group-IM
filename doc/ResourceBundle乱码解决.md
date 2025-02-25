# IntelliJ IDEA 中 `.properties` 文件编码默认为 `ISO-8859-1`解决方法

## 一、问题描述

在 IntelliJ IDEA 中创建 `.properties` 文件时，它会默认使用 `ISO-8859-1`编码。即使你的项目默认是 `UTF-8`，但 `.properties` 文件依然会自动选择 `ISO-8859-1`，导致中文信息显示为“????”。

## 二、原因解释

### 1. 原生 `ResourceBundle` 规范
Java 默认使用 `ResourceBundle` 加载 `.properties` 文件，其中惯例是通过 `ISO-8859-1` 读取文件，如果需要支持中文，必须使用 `native2ascii` 转换成 Unicode 字符串。

### 2. IntelliJ IDEA 为什么默认使用 `ISO-8859-1`？
- 遵循 Java 旧版本标准
- 为了确保跨平台兼容性

## 三、解决方案

### 方法 1: 手动修改 IDEA 的 `.properties` 文件编码

1. 打开 **IntelliJ IDEA 设置**  
   **Windows/Linux:** `File` → `Settings`  
   **macOS:** `IntelliJ IDEA` → `Preferences`

2. 进入 **Editor** → **File Encoding**

3. 找到 **Default encoding for properties files**

4. 选择 **UTF-8**

5. 取消勾选 `Transparent native-to-ascii conversion`

6. **应用并重启 IDEA**

---

### 方法 2: 在 `.editorconfig` 中指定 `.properties` 文件编码
如果想要确保项目中每个人的 IDEA 都使用 `UTF-8`，可以在项目根目录下添加 `.editorconfig` ：

```ini
[*.properties]
charset = utf-8
```

这样所有 `.properties` 文件都会自动使用 UTF-8。

---

### 方法 3: 强制 Spring Boot 读取 UTF-8
如果你的项目使用 Spring Boot，可以在 `pom.xml` 中设置 **系统默认编码**：

```xml
<properties>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>
```

然后在 `application.properties` 或 `application.yml` 文件中，确保 Spring Boot 读取 `UTF-8`：

```properties
spring.messages.encoding=UTF-8
```

---

## 四、结论

如果想要在 IntelliJ IDEA 中正确显示 `.properties` 文件中的中文，可以采用 **方法 1 + 方法 2**，确保固定使用 UTF-8。如果是 Spring Boot 项目，还可以配置 Spring Boot 使其读取 UTF-8 的 properties 文件。

**建议采用 `.editorconfig`，以确保项目全员使用 UTF-8。** 🚀

