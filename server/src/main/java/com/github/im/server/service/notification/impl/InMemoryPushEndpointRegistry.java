package com.github.im.server.service.notification.impl;

import com.github.im.server.service.notification.PushEndpoint;
import com.github.im.server.service.notification.PushEndpointRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryPushEndpointRegistry implements PushEndpointRegistry {

    private final Map<Long, Map<String, PushEndpoint>> endpointsByUser = new ConcurrentHashMap<>();

    @Override
    public PushEndpoint save(PushEndpoint endpoint) {
        endpointsByUser
                .computeIfAbsent(endpoint.getUserId(), ignored -> new ConcurrentHashMap<>())
                .put(endpoint.getEndpointId(), endpoint);
        return endpoint;
    }

    @Override
    public List<PushEndpoint> findByUserId(Long userId) {
        Map<String, PushEndpoint> endpoints = endpointsByUser.get(userId);
        return endpoints == null ? List.of() : new ArrayList<>(endpoints.values());
    }

    @Override
    public Optional<PushEndpoint> findByUserIdAndEndpointId(Long userId, String endpointId) {
        Map<String, PushEndpoint> endpoints = endpointsByUser.get(userId);
        if (endpoints == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(endpoints.get(endpointId));
    }

    @Override
    public void delete(Long userId, String endpointId) {
        Map<String, PushEndpoint> endpoints = endpointsByUser.get(userId);
        if (endpoints == null) {
            return;
        }
        endpoints.remove(endpointId);
        if (endpoints.isEmpty()) {
            endpointsByUser.remove(userId);
        }
    }
}
