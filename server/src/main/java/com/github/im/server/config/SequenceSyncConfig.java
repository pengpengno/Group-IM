package com.github.im.server.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "app.sequence-sync")
public class SequenceSyncConfig {
    
    /**
     * 是否启用序列同步功能
     */
    private boolean enabled = true;
    
    /**
     * 是否自动扫描所有包含自增主键的表
     * 如果设置为true，则会自动查找所有包含自增主键的表并进行序列同步
     * 如果设置为false，则只处理tables中配置的表
     */
    private boolean autoScan = false;
    
    /**
     * 需要同步序列的表配置列表（仅在autoScan为false时使用）
     */
    private List<TableSequence> tables = List.of(
        new TableSequence("users", "user_id")
    );
    
    @Data
    public static class TableSequence {
        /**
         * 表名
         */
        private String tableName;
        
        /**
         * 主键列名
         */
        private String primaryKeyColumn;
        
        public TableSequence() {
        }
        
        public TableSequence(String tableName, String primaryKeyColumn) {
            this.tableName = tableName;
            this.primaryKeyColumn = primaryKeyColumn;
        }
    }
}