package com.github.im.server.controller;

import com.github.im.dto.user.FriendRequestDto;
import com.github.im.dto.user.FriendshipDTO;
import com.github.im.server.model.Friendship;
import com.github.im.server.model.enums.Status;
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
    public ResponseEntity<FriendshipDTO> sendFriendRequest(@RequestBody FriendRequestDto request) {

        return ResponseEntity.ok(friendshipService.sendFriendRequest(request));
    }


    /**
     * 同步好友请求
     * 传入 客户端 目前最大的关系id ，并且获取之后获取新的关系数据
     * @param userId 用户ID
     *               maxId 最大关系id
     * @return
     */
    @PostMapping("/sync")
    public ResponseEntity<List<FriendshipDTO>> syncFriendSync(@RequestParam Long userId,
                                                              @RequestParam(required = false, defaultValue = "0") Long maxId) {
        List<FriendshipDTO> friendshipDTOS = friendshipService.syncFriendRequests(userId, maxId);
        return ResponseEntity.ok(friendshipDTOS);
    }

    /**
     *
     * 接受好友请求
     * @param request 好友请求
     * @return 响应
     */
    @PostMapping("/accept")
    public ResponseEntity<Void> acceptFriendRequest(@RequestBody FriendRequestDto request) {
        friendshipService.acceptFriendRequest(request);
        return ResponseEntity.ok().build();
    }



    @PostMapping("/delete")
    public ResponseEntity<Void> deleteFriendRequest(@RequestParam Long   userId  , Long friendId) {
        friendshipService.updateFriendRequestStatus(userId,friendId, Status.DELETED);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reject")
    public ResponseEntity<Void> rejectFriendRequest(@RequestParam Long   userId  , Long friendId) {
        friendshipService.updateFriendRequestStatus(userId,friendId, Status.REJECT);
        return ResponseEntity.ok().build();
    }


    /**
     * 查询所有联系人
     * @param userId 用户ID
     * @return 联系人列表
     */
    @PostMapping("/list")
    public  ResponseEntity<List<FriendshipDTO>> getFriends(@RequestParam Long userId   ,
                                                        @RequestHeader(value = "Authior",required = false)  String token
    )   {
        //TODO  只能获取 当前用户的 信息数据

        return ResponseEntity.ok( friendshipService.getFriends(userId));
    }

    /**
     * 查询用户的好友请求
     * @param userId 用户ID
     * @return 好友请求列表
     */
    @PostMapping("/queryRequest")
    public ResponseEntity<List<FriendshipDTO>> getSentFriendRequests(@RequestParam Long userId) {
        return ResponseEntity.ok(friendshipService.getFriendRequests(userId));
    }

    @DeleteMapping("/{userId}/{friendId}")
    public ResponseEntity<Void> deleteFriend(@PathVariable Long friendId,@PathVariable Long userId) {
        friendshipService.deleteFriend(userId,friendId);
        return ResponseEntity.noContent().build();
    }


}
