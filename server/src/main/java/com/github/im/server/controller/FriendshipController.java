package com.github.im.server.controller;

import com.github.im.dto.user.FriendRequestDto;
import com.github.im.dto.user.FriendshipDTO;
import com.github.im.server.model.Friendship;
import com.github.im.server.service.FriendshipService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/friendships")
@Valid
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


    @PostMapping("/acceptTs")
    public ResponseEntity<Void> acceptFriendRequestTs(@RequestBody FriendRequestDto request) {
        friendshipService.acceptFriendRequest(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/acceptGet")
    public ResponseEntity<String> acceptFriendRequestGet() {
        return ResponseEntity.ok().build();
    }




    @PostMapping("/list")
    public  ResponseEntity<List<FriendshipDTO>> getFriends(@RequestParam Long userId) {

        return ResponseEntity.ok( friendshipService.getFriends(userId));
    }



    @DeleteMapping("/{userId}/{friendId}")
    public ResponseEntity<Void> deleteFriend(@PathVariable Long friendId,@PathVariable Long userId) {
        friendshipService.deleteFriend(userId,friendId);
        return ResponseEntity.noContent().build();
    }


}
