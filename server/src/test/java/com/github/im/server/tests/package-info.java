/**
 * Group IM Server 测试包
 * 
 * <h2>测试框架和工具</h2>
 * <ul>
 *   <li><b>测试框架</b>: Spock Framework 2.4-M6</li>
 *   <li><b>编程语言</b>: Groovy</li>
 *   <li><b>Mock工具</b>: Spock内置Mock机制</li>
 *   <li><b>断言机制</b>: Spock内置断言</li>
 *   <li><b>覆盖率工具</b>: JaCoCo</li>
 * </ul>
 * 
 * <h2>测试分类和范围</h2>
 * 
 * <h3>1. 单元测试 (Unit Tests)</h3>
 * <p><b>测试范围</b>: 服务层和控制器层的核心业务逻辑</p>
 * <p><b>测试目标</b>:</p>
 * <ul>
 *   <li>UserService - 用户注册、登录、查询、密码管理等核心功能</li>
 *   <li>UserController - REST API接口的正确性和异常处理</li>
 *   <li>AuthenticationService - 认证授权逻辑</li>
 *   <li>其他业务服务类的核心方法</li>
 * </ul>
 * 
 * <h3>2. 集成测试 (Integration Tests)</h3>
 * <p><b>测试范围</b>: 多个组件协同工作的场景</p>
 * <p><b>测试目标</b>:</p>
 * <ul>
 *   <li>服务间调用的正确性</li>
 *   <li>数据流转的完整性</li>
 *   <li>事务处理的正确性</li>
 * </ul>
 * 
 * <h3>3. 安全测试 (Security Tests)</h3>
 * <p><b>测试范围</b>: 权限控制和安全机制</p>
 * <p><b>测试目标</b>:</p>
 * <ul>
 *   <li>@PreAuthorize注解的权限验证</li>
 *   <li>JWT Token的生成和验证</li>
 *   <li>敏感数据的访问控制</li>
 * </ul>
 * 
 * <h2>测试条件和环境要求</h2>
 * 
 * <h3>基础环境</h3>
 * <ul>
 *   <li><b>JDK版本</b>: Java 21 (含预览特性支持)</li>
 *   <li><b>构建工具</b>: Maven 3.8+</li>
 *   <li><b>测试框架</b>: Spock 2.4-M6 + Groovy 3.0</li>
 *   <li><b>内存要求</b>: 至少2GB可用内存</li>
 * </ul>
 * 
 * <h3>依赖服务</h3>
 * <ul>
 *   <li><b>数据库</b>: PostgreSQL (可通过内存数据库H2替代)</li>
 *   <li><b>Redis</b>: 缓存服务 (可选，用于集成测试)</li>
 *   <li><b>LDAP</b>: 认证服务 (可选，用于LDAP相关测试)</li>
 * </ul>
 * 
 * <h3>测试数据要求</h3>
 * <ul>
 *   <li>使用Mock对象隔离外部依赖</li>
 *   <li>测试数据应具有代表性但不依赖真实环境</li>
 *   <li>每个测试用例应独立运行，不互相影响</li>
 * </ul>
 * 
 * <h2>测试编写规范</h2>
 * 
 * <h3>命名规范</h3>
 * <pre>
 * // 测试方法命名格式
 * def "测试[功能模块]_[具体场景]_[预期结果]"() {
 *     // 测试实现
 * }
 * 
 * // 示例
 * def "测试用户注册_用户名已存在_抛出异常"()
 * def "测试用户登录_凭据正确_返回用户信息"()
 * </pre>
 * 
 * <h3>结构规范</h3>
 * <pre>
 * def "测试描述"() {
 *     given: "测试前置条件和数据准备"
 *         // 准备测试数据
 *         // 设置Mock行为
 *         
 *     when: "执行被测试的操作"
 *         // 调用被测试方法
 *         
 *     then: "验证结果和副作用"
 *         // 验证返回值
 *         // 验证Mock交互
 *         // 验证状态变化
 * }
 * </pre>
 * 
 * <h3>Mock使用规范</h3>
 * <ul>
 *   <li>只Mock外部依赖和服务</li>
 *   <li>明确验证方法调用次数: 1 * service.method(_) >> result</li>
 *   <li>使用具体参数而非通配符，除非确实需要</li>
 *   <li>在then块中验证Mock交互</li>
 * </ul>
 * 
 * <h2>测试执行和报告</h2>
 * 
 * <h3>执行命令</h3>
 * <pre>
 * # 运行所有测试
 * mvn test
 * 
 * # 运行特定测试类
 * mvn test -Dtest=UserServiceSpec
 * 
 * # 生成覆盖率报告
 * mvn jacoco:report
 * 
 * # 清理并运行测试
 * mvn clean test
 * </pre>
 * 
 * <h3>报告位置</h3>
 * <ul>
 *   <li><b>测试结果</b>: target/surefire-reports/</li>
 *   <li><b>覆盖率报告</b>: target/site/jacoco/index.html</li>
 *   <li><b>测试日志</b>: 控制台输出或target/test.log</li>
 * </ul>
 * 
 * <h2>质量标准</h2>
 * 
 * <h3>覆盖率指标</h3>
 * <ul>
 *   <li><b>行覆盖率</b>: ≥ 80%</li>
 *   <li><b>分支覆盖率</b>: ≥ 75%</li>
 *   <li><b>方法覆盖率</b>: ≥ 85%</li>
 * </ul>
 * 
 * <h3>测试特性要求</h3>
 * <ul>
 *   <li><b>独立性</b>: 测试之间无依赖关系</li>
 *   <li><b>可重复性</b>: 多次运行结果一致</li>
 *   <li><b>快速性</b>: 单个测试执行时间 < 1秒</li>
 *   <li><b>清晰性</b>: 测试名称和结构易于理解</li>
 * </ul>
 * 
 * <h2>常见测试场景模板</h2>
 * 
 * <h3>成功场景测试模板</h3>
 * <pre>
 * def "测试[功能]_成功场景"() {
 *     given: "准备有效的测试数据"
 *         def testData = // 准备测试数据
 *         
 *     when: "执行正常操作"
 *         def result = service.method(testData)
 *         
 *     then: "验证期望结果"
 *         result != null
 *         result.property == expectedValue
 * }
 * </pre>
 * 
 * <h3>异常场景测试模板</h3>
 * <pre>
 * def "测试[功能]_异常场景"() {
 *     given: "准备导致异常的数据"
 *         def invalidData = // 准备无效数据
 *         
 *     when: "执行操作"
 *         service.method(invalidData)
 *         
 *     then: "验证异常被正确抛出"
 *         thrown(ExpectedException)
 * }
 * </pre>
 * 
 * @since 1.0
 * @author Group IM Team
 */
package com.github.im.server.tests;