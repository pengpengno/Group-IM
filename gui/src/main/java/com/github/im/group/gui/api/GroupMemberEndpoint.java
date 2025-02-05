package com.github.im.group.gui.api;

import com.github.im.dto.GroupMemberDTO;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.service.annotation.DeleteExchange;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import reactor.core.publisher.Mono;
import java.util.List;

@HttpExchange
public interface GroupMemberEndpoint {

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
     * Add a new member to a group
     * @param groupId the ID of the group
     * @param userId the ID of the user
     * @return the added group member
     */
    @PostExchange("/api/groups/{groupId}/members/{userId}")
    Mono<GroupMemberDTO> addMemberToGroup(@PathVariable("groupId") Long groupId, 
                                           @PathVariable("userId") Long userId);

    /**
     * Add multiple members to a group
     * @param groupId the ID of the group
     * @param userIds a list of user IDs to add
     * @return the number of added members
     */
    @PostExchange("/api/groups/{groupId}/members/bulk")
    Mono<Integer> addMembersToGroup(@PathVariable("groupId") Long groupId, 
                                    @RequestBody List<Long> userIds);

    /**
     * Remove a member from a group
     * @param groupId the ID of the group
     * @param userId the ID of the user
     * @return response indicating success or failure
     */
    @DeleteExchange("/api/groups/{groupId}/members/{userId}")
    Mono<Void> removeMemberFromGroup(@PathVariable("groupId") Long groupId, 
                                     @PathVariable("userId") Long userId);
}
