package com.github.im.server.service.notification;

import com.github.im.dto.notification.PushEndpointDTO;
import com.github.im.dto.notification.PushEndpointUpsertRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PushEndpointService {

    private final PushEndpointRegistry pushEndpointRegistry;

    public PushEndpointDTO register(Long userId, PushEndpointUpsertRequest request) {
        long now = System.currentTimeMillis();
        PushPlatform platform = parsePlatform(request.getPlatform());
        PushProviderType providerType = parseProvider(request.getProvider());
        String endpointId = resolveEndpointId(userId, request, providerType);

        PushEndpoint existing = endpointId == null
                ? null
                : pushEndpointRegistry.findByUserIdAndEndpointId(userId, endpointId).orElse(null);

        PushEndpoint endpoint = PushEndpoint.builder()
                .endpointId(endpointId != null ? endpointId : UUID.randomUUID().toString())
                .userId(userId)
                .platform(platform)
                .providerType(providerType)
                .deviceId(trimToNull(request.getDeviceId()))
                .token(trimToNull(request.getToken()))
                .endpointUrl(trimToNull(request.getEndpointUrl()))
                .p256dh(trimToNull(request.getP256dh()))
                .auth(trimToNull(request.getAuth()))
                .locale(trimToNull(request.getLocale()))
                .appVersion(trimToNull(request.getAppVersion()))
                .sandbox(Boolean.TRUE.equals(request.getSandbox()))
                .enabled(request.getEnabled() == null || request.getEnabled())
                .createdAt(existing != null ? existing.getCreatedAt() : now)
                .updatedAt(now)
                .build();

        return toDto(pushEndpointRegistry.save(endpoint));
    }

    public List<PushEndpointDTO> list(Long userId) {
        return pushEndpointRegistry.findByUserId(userId)
                .stream()
                .map(this::toDto)
                .toList();
    }

    public void delete(Long userId, String endpointId) {
        pushEndpointRegistry.delete(userId, endpointId);
    }

    private String resolveEndpointId(Long userId, PushEndpointUpsertRequest request, PushProviderType providerType) {
        if (trimToNull(request.getEndpointId()) != null) {
            return request.getEndpointId().trim();
        }

        String requestDeviceId = trimToNull(request.getDeviceId());
        String requestToken = trimToNull(request.getToken());
        String requestEndpointUrl = trimToNull(request.getEndpointUrl());

        return pushEndpointRegistry.findByUserId(userId).stream()
                .filter(item -> item.getProviderType() == providerType)
                .filter(item -> sameIdentity(item, requestDeviceId, requestToken, requestEndpointUrl))
                .map(PushEndpoint::getEndpointId)
                .findFirst()
                .orElse(null);
    }

    private boolean sameIdentity(
            PushEndpoint endpoint,
            String requestDeviceId,
            String requestToken,
            String requestEndpointUrl
    ) {
        if (requestDeviceId != null && requestDeviceId.equals(endpoint.getDeviceId())) {
            return true;
        }
        if (requestToken != null && requestToken.equals(endpoint.getToken())) {
            return true;
        }
        return requestEndpointUrl != null && requestEndpointUrl.equals(endpoint.getEndpointUrl());
    }

    private PushPlatform parsePlatform(String value) {
        String normalized = normalizeEnumValue(value, "platform");
        return PushPlatform.valueOf(normalized);
    }

    private PushProviderType parseProvider(String value) {
        String normalized = normalizeEnumValue(value, "provider");
        return PushProviderType.valueOf(normalized);
    }

    private String normalizeEnumValue(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim().toUpperCase(Locale.ROOT).replace('-', '_');
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PushEndpointDTO toDto(PushEndpoint endpoint) {
        return PushEndpointDTO.builder()
                .endpointId(endpoint.getEndpointId())
                .platform(endpoint.getPlatform().name())
                .provider(endpoint.getProviderType().name())
                .deviceId(endpoint.getDeviceId())
                .locale(endpoint.getLocale())
                .appVersion(endpoint.getAppVersion())
                .sandbox(endpoint.isSandbox())
                .enabled(endpoint.isEnabled())
                .createdAt(endpoint.getCreatedAt())
                .updatedAt(endpoint.getUpdatedAt())
                .build();
    }
}
