package com.github.im.group.gui.api;

import com.github.im.conversation.ConversationRes;
import com.github.im.conversation.GroupInfo;
import com.github.im.dto.GroupMemberDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@HttpExchange
public interface ConversationEndpoint {

    /**
     * Get all members of a group
     * @param groupId the ID of the group
     * @return a list of group member DTOs
     */
    @GetExchange("/api/groups/{groupId}/members")
    Mono<List<GroupMemberDTO>> getMembersByGroupId(@PathVariable("groupId") Long groupId);

    /**
     * Get all groups of a user
     * @param userId the ID of the user
     * @return a list of group member DTOs
     */
    @GetExchange("/api/groups/users/{userId}/groups")
    Mono<List<GroupMemberDTO>> getGroupsByUserId(@PathVariable("userId") Long userId);

    /**
     * Create a new group
     * @param groupInfo the group information including group name, description, and members
     * @return the created group
     */
    @PostExchange("/api/conversations")
    Mono<ConversationRes> createGroup(@RequestBody GroupInfo groupInfo);

    /**
     * Create or get a private chat conversation between two users
     * @param userId1 the ID of the first user  group creator
     * @param userId2 the ID of the second user
     * @return the private chat conversation DTO
     */
    @PostExchange("/api/conversations/private-chat")
    Mono<ConversationRes> createOrGetPrivateChat(@RequestParam("userId1") Long userId1, @RequestParam("userId2") Long userId2);

    /**
     * Get active conversations for a user
     * @param userId the ID of the user
     * @return a list of active conversations
     */
    @GetExchange("/api/conversations/{userId}/active")
    Mono<List<ConversationRes>> getActiveConversationsByUserId(@PathVariable("userId") Long userId);
}