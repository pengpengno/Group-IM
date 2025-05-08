package com.github.im.group.gui.api;

import com.github.im.dto.user.FriendRequestDto;
import com.github.im.dto.user.FriendshipDTO;
import org.springframework.beans.factory.annotation.Lookup;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@HttpExchange("/api")
public interface FriendShipEndpoint {

	// 发送好友请求
	@PostExchange("/friendships/request")
	Mono<ResponseEntity<Void>> sendFriendRequest(@RequestBody FriendRequestDto friendRequestDto);

	// 接受好友请求
	@PostExchange("/friendships/accept")
	Mono<Void> acceptFriendRequest(@RequestBody FriendRequestDto friendRequestDto);

	// 获取好友列表
	@PostExchange("/friendships/list")
	Flux<FriendshipDTO> getFriends(@RequestParam Long userId);

	// 删除好友
	@DeleteExchange("/friendships/{userId}/{friendId}")
	Mono<Void> deleteFriend(@RequestBody Long userId, @RequestBody Long friendId);
}
