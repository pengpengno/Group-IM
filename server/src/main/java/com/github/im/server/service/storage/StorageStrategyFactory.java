package com.github.im.server.service.storage;

import java.util.HashMap;
import java.util.Map;

/**
 * 存储策略工厂
 */
public class StorageStrategyFactory {
    
    private final Map<String, StorageStrategy> strategies = new HashMap<>();
    
    public void registerStrategy(StorageStrategy strategy) {
        strategies.put(strategy.getStorageType(), strategy);
    }
    
    public StorageStrategy getStrategy(String storageType) {
        StorageStrategy strategy = strategies.get(storageType.toUpperCase());
        if (strategy == null) {
            throw new IllegalArgumentException("Unsupported storage type: " + storageType);
        }
        return strategy;
    }
    
    public StorageStrategy getDefaultStrategy() {
        return getStrategy("LOCAL");
    }
}