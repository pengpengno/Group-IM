package com.github.im.server.controller;

import com.github.im.dto.user.FriendRequestDto;
import com.github.im.server.service.FriendshipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friendships")
public class FriendshipController {

    @Autowired
    private FriendshipService friendshipService;

    @PostMapping("/request")
    public ResponseEntity<Void> sendFriendRequest(@RequestBody FriendRequestDto request) {
        friendshipService.sendFriendRequest(request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/accept")
    public ResponseEntity<Void> acceptFriendRequest(@RequestBody FriendRequestDto request) {
        friendshipService.acceptFriendRequest(request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<Void> deleteFriend(@PathVariable Long friendId) {
        friendshipService.deleteFriend(friendId);
        return ResponseEntity.noContent().build();
    }
}
