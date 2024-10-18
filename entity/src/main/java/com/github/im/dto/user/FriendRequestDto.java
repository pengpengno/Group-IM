package com.github.im.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FriendRequestDto implements Serializable {
    private Long userId;
    private Long friendId;

    // Getters and Setters
}
