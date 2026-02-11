package com.github.im.server.tests

import com.github.im.dto.user.UserInfo
import com.github.im.server.model.User

/**
 * 测试工具类
 * 提供便捷的调试和验证功能
 */
class TestUtils {
    
    /**
     * 打印User对象的详细信息
     * @param user 要打印的User对象
     * @param label 标签说明
     */
    static void printUserDetails(User user, String label = "User") {
        println "=== ${label} Details ==="
        if (user == null) {
            println "NULL OBJECT"
            return
        }
        println "UserId: ${user.getUserId() ?: 'NULL'}"
        println "Username: ${user.getUsername() ?: 'NULL'}"
        println "Email: ${user.getEmail() ?: 'NULL'}"
        println "Phone: ${user.getPhoneNumber() ?: 'NULL'}"
        println "PasswordHash: ${user.getPasswordHash() ? 'PRESENT' : 'NULL'}"
        println "ForcePasswordChange: ${user.isForcePasswordChange()}"
        println "PrimaryCompanyId: ${user.getPrimaryCompanyId() ?: 'NULL'}"
        println "====================="
    }
    
    /**
     * 打印UserInfo对象的详细信息
     * @param userInfo 要打印的UserInfo对象
     * @param label 标签说明
     */
    static void printUserInfoDetails(UserInfo userInfo, String label = "UserInfo") {
        println "=== ${label} Details ==="
        if (userInfo == null) {
            println "NULL OBJECT"
            return
        }
        println "UserId: ${userInfo.getUserId() ?: 'NULL'}"
        println "Username: ${userInfo.getUsername() ?: 'NULL'}"
        println "Email: ${userInfo.getEmail() ?: 'NULL'}"
        println "Phone: ${userInfo.getPhoneNumber() ?: 'NULL'}"
        println "Token: ${userInfo.getToken() ? 'PRESENT' : 'NULL'}"
        println "RefreshToken: ${userInfo.getRefreshToken() ? 'PRESENT' : 'NULL'}"
        println "CurrentLoginCompanyId: ${userInfo.getCurrentLoginCompanyId() ?: 'NULL'}"
        println "========================"
    }
    
    /**
     * 比较两个User对象的关键属性
     * @param user1 第一个User对象
     * @param user2 第二个User对象
     * @return 属性差异的详细报告
     */
    static String compareUsers(User user1, User user2) {
        def differences = []
        
        if (user1.getUserId() != user2.getUserId()) {
            differences << "UserId: ${user1.getUserId()} vs ${user2.getUserId()}"
        }
        if (user1.getUsername() != user2.getUsername()) {
            differences << "Username: '${user1.getUsername()}' vs '${user2.getUsername()}'"
        }
        if (user1.getEmail() != user2.getEmail()) {
            differences << "Email: '${user1.getEmail()}' vs '${user2.getEmail()}'"
        }
        if (user1.getPhoneNumber() != user2.getPhoneNumber()) {
            differences << "Phone: '${user1.getPhoneNumber()}' vs '${user2.getPhoneNumber()}'"
        }
        
        return differences.isEmpty() ? 
            "Users are identical" : 
            "Differences found:\n" + differences.join("\n")
    }
    
    /**
     * 比较两个UserInfo对象的关键属性
     * @param info1 第一个UserInfo对象
     * @param info2 第二个UserInfo对象
     * @return 属性差异的详细报告
     */
    static String compareUserInfo(UserInfo info1, UserInfo info2) {
        def differences = []
        
        if (info1.getUserId() != info2.getUserId()) {
            differences << "UserId: ${info1.getUserId()} vs ${info2.getUserId()}"
        }
        if (info1.getUsername() != info2.getUsername()) {
            differences << "Username: '${info1.getUsername()}' vs '${info2.getUsername()}'"
        }
        if (info1.getEmail() != info2.getEmail()) {
            differences << "Email: '${info1.getEmail()}' vs '${info2.getEmail()}'"
        }
        if (info1.getPhoneNumber() != info2.getPhoneNumber()) {
            differences << "Phone: '${info1.getPhoneNumber()}' vs '${info2.getPhoneNumber()}'"
        }
        
        return differences.isEmpty() ? 
            "UserInfo objects are identical" : 
            "Differences found:\n" + differences.join("\n")
    }
    
    /**
     * 创建测试用的User对象
     * @param overrides 覆盖默认值的属性
     * @return User对象
     */
    static User createTestUser(Map overrides = [:]) {
        def defaults = [
            userId: 1L,
            username: "testuser",
            email: "test@example.com",
            phoneNumber: "13800138000",
            passwordHash: "hashed_password",
            forcePasswordChange: false,
            primaryCompanyId: 1L
        ]
        
        def props = defaults + overrides
        return User.builder()
            .userId(props.userId)
            .username(props.username)
            .email(props.email)
            .phoneNumber(props.phoneNumber)
            .passwordHash(props.passwordHash)
            .forcePasswordChange(props.forcePasswordChange)
            .primaryCompanyId(props.primaryCompanyId)
            .build()
    }
    
    /**
     * 创建测试用的UserInfo对象
     * @param overrides 覆盖默认值的属性
     * @return UserInfo对象
     */
    static UserInfo createTestUserInfo(Map overrides = [:]) {
        def defaults = [
            userId: 1L,
            username: "testuser",
            email: "test@example.com",
            phoneNumber: "13800138000",
            token: "test_token",
            refreshToken: "test_refresh_token",
            currentLoginCompanyId: 1L
        ]
        
        def props = defaults + overrides
        def userInfo = new UserInfo()
        userInfo.setUserId(props.userId)
        userInfo.setUsername(props.username)
        userInfo.setEmail(props.email)
        userInfo.setPhoneNumber(props.phoneNumber)
        userInfo.setToken(props.token)
        userInfo.setRefreshToken(props.refreshToken)
        userInfo.setCurrentLoginCompanyId(props.currentLoginCompanyId)
        
        return userInfo
    }
    
    /**
     * 深度比较两个User对象的所有属性
     * @param user1 第一个User对象
     * @param user2 第二个User对象
     * @param ignoreFields 要忽略比较的字段列表
     * @return 详细的比较报告
     */
    static String deepCompareUsers(User user1, User user2, List<String> ignoreFields = []) {
        def differences = []
        def similarities = []
        
        if (user1 == null && user2 == null) {
            return "Both objects are NULL"
        }
        
        if (user1 == null || user2 == null) {
            return "One object is NULL, another is not"
        }
        
        // 检查内存地址
        if (user1.is(user2)) {
            similarities << "SAME MEMORY ADDRESS"
        } else {
            differences << "DIFFERENT MEMORY ADDRESSES"
        }
        
        // 逐个属性比较
        compareAndRecord("UserId", user1.getUserId(), user2.getUserId(), similarities, differences, ignoreFields)
        compareAndRecord("Username", user1.getUsername(), user2.getUsername(), similarities, differences, ignoreFields)
        compareAndRecord("Email", user1.getEmail(), user2.getEmail(), similarities, differences, ignoreFields)
        compareAndRecord("PhoneNumber", user1.getPhoneNumber(), user2.getPhoneNumber(), similarities, differences, ignoreFields)
        compareAndRecord("PasswordHash", 
            user1.getPasswordHash() ? "[PRESENT]" : "[NULL]", 
            user2.getPasswordHash() ? "[PRESENT]" : "[NULL]", 
            similarities, differences, ignoreFields)
        compareAndRecord("ForcePasswordChange", user1.isForcePasswordChange(), user2.isForcePasswordChange(), similarities, differences, ignoreFields)
        compareAndRecord("PrimaryCompanyId", user1.getPrimaryCompanyId(), user2.getPrimaryCompanyId(), similarities, differences, ignoreFields)
        
        def report = "=== 深度User对象比较报告 ===\n"
        if (similarities) {
            report += "相似属性 (${similarities.size()}):\n"
            similarities.each { report += "  ✓ ${it}\n" }
        }
        if (differences) {
            report += "差异属性 (${differences.size()}):\n"
            differences.each { report += "  ✗ ${it}\n" }
        }
        report += "============================"
        
        return report
    }
    
    /**
     * 深度比较两个UserInfo对象的所有属性
     * @param info1 第一个UserInfo对象
     * @param info2 第二个UserInfo对象
     * @param ignoreFields 要忽略比较的字段列表
     * @return 详细的比较报告
     */
    static String deepCompareUserInfo(UserInfo info1, UserInfo info2, List<String> ignoreFields = []) {
        def differences = []
        def similarities = []
        
        if (info1 == null && info2 == null) {
            return "Both objects are NULL"
        }
        
        if (info1 == null || info2 == null) {
            return "One object is NULL, another is not"
        }
        
        // 检查内存地址
        if (info1.is(info2)) {
            similarities << "SAME MEMORY ADDRESS"
        } else {
            differences << "DIFFERENT MEMORY ADDRESSES"
        }
        
        // 逐个属性比较
        compareAndRecord("UserId", info1.getUserId(), info2.getUserId(), similarities, differences, ignoreFields)
        compareAndRecord("Username", info1.getUsername(), info2.getUsername(), similarities, differences, ignoreFields)
        compareAndRecord("Email", info1.getEmail(), info2.getEmail(), similarities, differences, ignoreFields)
        compareAndRecord("PhoneNumber", info1.getPhoneNumber(), info2.getPhoneNumber(), similarities, differences, ignoreFields)
        compareAndRecord("Token", 
            info1.getToken() ? "[PRESENT]" : "[NULL]", 
            info2.getToken() ? "[PRESENT]" : "[NULL]", 
            similarities, differences, ignoreFields)
        compareAndRecord("RefreshToken", 
            info1.getRefreshToken() ? "[PRESENT]" : "[NULL]", 
            info2.getRefreshToken() ? "[PRESENT]" : "[NULL]", 
            similarities, differences, ignoreFields)
        compareAndRecord("CurrentLoginCompanyId", info1.getCurrentLoginCompanyId(), info2.getCurrentLoginCompanyId(), similarities, differences, ignoreFields)
        
        def report = "=== 深度UserInfo对象比较报告 ===\n"
        if (similarities) {
            report += "相似属性 (${similarities.size()}):\n"
            similarities.each { report += "  ✓ ${it}\n" }
        }
        if (differences) {
            report += "差异属性 (${differences.size()}):\n"
            differences.each { report += "  ✗ ${it}\n" }
        }
        report += "================================"
        
        return report
    }
    
    /**
     * 辅助方法：比较两个值并记录结果
     */
    private static void compareAndRecord(String fieldName, def value1, def value2, 
                                       List<String> similarities, List<String> differences, 
                                       List<String> ignoreFields) {
        if (ignoreFields.contains(fieldName)) {
            return
        }
        
        if (value1 == value2) {
            similarities << "${fieldName}: ${formatValue(value1)}"
        } else {
            differences << "${fieldName}: ${formatValue(value1)} vs ${formatValue(value2)}"
        }
    }
    
    /**
     * 格式化值用于显示
     */
    private static String formatValue(def value) {
        if (value == null) return "[NULL]"
        if (value instanceof String && value.isEmpty()) return "[EMPTY_STRING]"
        return value.toString()
    }
    
    /**
     * 验证对象属性是否符合预期值
     * @param object 要验证的对象
     * @param expectedProperties 期望的属性值映射
     * @return 验证结果报告
     */
    static String validateObjectProperties(Object object, Map<String, Object> expectedProperties) {
        def mismatches = []
        def matches = []
        
        expectedProperties.each { propertyName, expectedValue ->
            try {
                // 使用反射获取属性值
                def actualValue = object."get${propertyName.capitalize()}"()
                
                if (actualValue == expectedValue) {
                    matches << "${propertyName}: ${formatValue(actualValue)}"
                } else {
                    mismatches << "${propertyName}: 期望[${formatValue(expectedValue)}] 实际[${formatValue(actualValue)}]"
                }
            } catch (Exception e) {
                mismatches << "${propertyName}: 无法访问属性 (${e.message})"
            }
        }
        
        def report = "=== 对象属性验证报告 ===\n"
        report += "对象类型: ${object.getClass().simpleName}\n"
        if (matches) {
            report += "匹配的属性 (${matches.size()}):\n"
            matches.each { report += "  ✓ ${it}\n" }
        }
        if (mismatches) {
            report += "不匹配的属性 (${mismatches.size()}):\n"
            mismatches.each { report += "  ✗ ${it}\n" }
        }
        report += "========================"
        
        return report
    }
    
    /**
     * 创建具有特定属性的测试User对象用于比较测试
     */
    static User createComparisonTestUser(Map properties = [:]) {
        def baseProps = [
            userId: 999L,  // 特殊ID用于识别
            username: "comparison_test_user",
            email: "comparison@test.com",
            phoneNumber: "13800138888",
            passwordHash: "comparison_hash",
            forcePasswordChange: true,
            primaryCompanyId: 999L
        ]
        
        def finalProps = baseProps + properties
        return createTestUser(finalProps)
    }
    
    /**
     * 创建具有特定属性的测试UserInfo对象用于比较测试
     */
    static UserInfo createComparisonTestUserInfo(Map properties = [:]) {
        def baseProps = [
            userId: 999L,  // 特殊ID用于识别
            username: "comparison_test_user",
            email: "comparison@test.com",
            phoneNumber: "13800138888",
            token: "comparison_token",
            refreshToken: "comparison_refresh_token",
            currentLoginCompanyId: 999L
        ]
        
        def finalProps = baseProps + properties
        return createTestUserInfo(finalProps)
    }
}