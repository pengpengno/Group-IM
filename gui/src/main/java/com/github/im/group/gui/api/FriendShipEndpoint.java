package com.github.im.group.gui.api;

import com.github.im.dto.user.FriendRequestDto;
import com.github.im.dto.user.FriendshipDTO;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@HttpExchange("/api")
public interface FriendShipEndpoint {



	// 发送好友请求
	@PostExchange("/friendships/request")
	Mono<Void> sendFriendRequest(@RequestBody FriendRequestDto friendRequestDto);

	// 接受好友请求
	@PostExchange("/friendships/accept")
	Mono<Void> acceptFriendRequest(@RequestBody FriendRequestDto friendRequestDto);

	// 获取好友列表
	@GetExchange("/friendships/list")
	Mono<List<FriendshipDTO>> getFriends(@RequestBody Long userId);

	// 删除好友
	@DeleteExchange("/friendships/{userId}/{friendId}")
	Mono<Void> deleteFriend(@RequestBody Long userId, @RequestBody Long friendId);
}
