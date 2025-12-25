package com.github.im.server.constants;

/**
 * 缓存键常量类
 * 定义项目中所有缓存键的命名规范，避免混乱并提供更好的可维护性
 */
public class CacheKeyConstants {

    /**
     * 用户会话相关缓存键
     */
    public static class UserSession {
        public static final String PREFIX = "user:session";
        public static final String USER_SESSION_FORMAT = PREFIX + ":%d:%d"; // userId, companyId
        public static final String USER_SESSION_SPEL = "'user:session:' + #userId + ':' + #companyId"; // userId, companyId
    }

    /**
     * 公司相关缓存键
     */
    public static class Company {
        public static final String COMPANY_CACHE = "companies";

        public static final String PREFIX = "company";
        public static final String COMPANY_BY_ID_FORMAT = PREFIX + ":id:%d";
        public static final String COMPANY_BY_ID_SPEL = "'company:id:' + #companyId";
        public static final String COMPANY_BY_SCHEMA_FORMAT = PREFIX + ":schema:%s";
        public static final String COMPANY_BY_SCHEMA_SPEL = "'company:schema:' + #schemaName";
        public static final String COMPANY_WITH_USERS_FORMAT = PREFIX + ":withUsers:%d";
        public static final String COMPANY_WITH_USERS_SPEL = "'company:withUsers:' + #companyId";
        public static final String USER_COMPANY_IDS = PREFIX + ":user:%d";
        public static final String USER_COMPANY_IDS_SPEL = "'company:user:' + #userId";
    }

    /**
     * 用户相关缓存键
     */
    public static class User {
        public static final String PREFIX = "user";
        public static final String USER_BY_ID = PREFIX + ":id:%d";
        public static final String USER_BY_USERNAME = PREFIX + ":username:%s";
        public static final String USER_BY_EMAIL = PREFIX + ":email:%s";
        public static final String USER_ACCESS_TOKEN_FORMAT = PREFIX + ":access_token:%d";
        public static final String USER_ACCESS_TOKEN_SPEL = "'user:access_token:' + #userId"; // userId
        public static final String USER_REFRESH_TOKEN_FORMAT = PREFIX + ":refreshToken:%d";
        public static final String USER_REFRESH_TOKEN_SPEL = "'user:refreshToken:' + #userId"; // userId
    }

    /**
     * 部门相关缓存键
     */
    public static class Department {
        public static final String PREFIX = "department";
        public static final String DEPARTMENT_BY_ID = PREFIX + ":id:%d";
        public static final String DEPARTMENT_BY_ID_SPEL = "'department:id:' + #departmentId";
        public static final String DEPARTMENT_WITH_USERS = PREFIX + ":withUsers:%d";
        public static final String DEPARTMENT_WITH_USERS_SPEL = "'department:withUsers:' + #departmentId";
        public static final String DEPARTMENT_CHILDREN = PREFIX + ":children:%d";
        public static final String DEPARTMENT_CHILDREN_SPEL = "'department:children:' + #departmentId";
    }

    /**
     * 系统配置相关缓存键
     */
    public static class SystemConfig {
        public static final String PREFIX = "system:config";
        public static final String CONFIG_BY_KEY = PREFIX + ":key:%s";
        public static final String CONFIG_BY_KEY_SPEL = "'system:config:key:' + #configKey";
    }
}