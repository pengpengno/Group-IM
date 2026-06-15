package com.github.im.server.service.notification;

import java.util.List;
import java.util.Optional;

public interface PushEndpointRegistry {

    PushEndpoint save(PushEndpoint endpoint);

    List<PushEndpoint> findByUserId(Long userId);

    Optional<PushEndpoint> findByUserIdAndEndpointId(Long userId, String endpointId);

    void delete(Long userId, String endpointId);
}
