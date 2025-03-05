package com.github.im.group.gui.api;

import com.github.im.dto.GroupMemberDTO;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
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


}
