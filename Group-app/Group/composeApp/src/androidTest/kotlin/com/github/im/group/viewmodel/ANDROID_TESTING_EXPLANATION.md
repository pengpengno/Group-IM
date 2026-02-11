# Android测试 vs Common测试 - 区别与价值说明

## 核心区别

### CommonTest (src/commonTest/)
- **运行环境**: JVM本地环境
- **测试目标**: 跨平台业务逻辑
- **依赖**: 纯Kotlin代码，无Android特定API
- **优势**: 快速执行，易于CI集成
- **局限**: 无法测试Android特定功能

### AndroidTest (src/androidTest/)  
- **运行环境**: Android设备/模拟器
- **测试目标**: Android平台特有功能
- **依赖**: Android Framework API
- **优势**: 真实环境测试，完整功能验证
- **局限**: 执行较慢，需要设备支持

## 当前测试文件说明

### 1. UserViewModelAndroidTest.kt
**定位**: Android平台基础能力测试
**测试内容**:
- Android Context的正确获取和使用
- Android系统服务访问能力
- 设备硬件信息获取
- Android存储路径访问
- 配置变更处理能力

**价值**: 验证应用在Android环境中的基础运行能力

### 2. UserViewModelAndroidIntegrationTest.kt  
**定位**: Android集成测试
**测试内容**:
- SharedPreferences真实存储测试
- 网络状态检测和处理
- 电池优化和省电模式适配
- 运行时权限动态检查
- 应用生命周期状态管理

**价值**: 验证Android特有功能的完整集成和交互

## 为什么需要两种测试？

### CommonTest的价值
```kotlin
// 适合测试纯业务逻辑
@Test
fun testBusinessLogic() {
    val result = calculateSomething(input)
    assertEquals(expected, result)
}
```

### AndroidTest的价值  
```kotlin
// 适合测试Android特定功能
@Test
fun testAndroidFeature() {
    val sharedPrefs = context.getSharedPreferences("test", Context.MODE_PRIVATE)
    sharedPrefs.edit().putString("key", "value").apply()
    assertEquals("value", sharedPrefs.getString("key", ""))
}
```

## 最佳实践建议

1. **ViewModel业务逻辑**: 优先使用CommonTest测试核心逻辑
2. **Android平台集成**: 使用AndroidTest验证平台特有功能  
3. **端到端流程**: 结合两种测试确保完整覆盖
4. **性能敏感功能**: AndroidTest验证真实环境表现

## 测试金字塔模型

```
      ∧
     / \   
    /___\   UI Tests (Espresso/Compose)
   /     \  
  /_______\ Integration Tests (AndroidTest)
 /         \
/___________\ Unit Tests (CommonTest)
```

这样的分层确保了：
- 底层快速稳定的单元测试
- 中层Android平台集成验证  
- 顶层用户体验端到端测试