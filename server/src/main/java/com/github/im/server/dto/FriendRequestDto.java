package com.github.im.server.dto;

import lombok.Data;

@Data
public class FriendRequestDto {
    private Long userId;
    private Long friendId;

    // Getters and Setters
}
