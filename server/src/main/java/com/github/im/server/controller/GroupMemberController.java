package com.github.im.server.controller;

import com.github.im.dto.GroupMemberDTO;
import com.github.im.server.mapstruct.GroupMemberMapper;
import com.github.im.server.service.GroupMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupMemberController {

    private final GroupMemberService groupMemberService;
    private final GroupMemberMapper groupMemberMapper;



    /**
     * Get all members of a group
     * @param groupId the ID of the group
     * @return a list of group member DTOs
     */
    @GetMapping("/{groupId}/members")
    public ResponseEntity<List<GroupMemberDTO>> getMembersByGroupId(@PathVariable Long groupId) {
        List<GroupMemberDTO> groupMembersDTO = groupMemberService.getMembersByConversationId(groupId).stream()
                .map(groupMemberMapper::groupMemberToGroupMemberDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(groupMembersDTO);
    }

    /**
     * Get all groups of a user
     * @param userId the ID of the user
     * @return a list of group member DTOs
     */
    @GetMapping("/users/{userId}/groups")
    public ResponseEntity<List<GroupMemberDTO>> getGroupsByUserId(@PathVariable Long userId) {
        List<GroupMemberDTO> groupMembersDTO = groupMemberService.getGroupsByUserId(userId).stream()
                .map(groupMemberMapper::groupMemberToGroupMemberDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(groupMembersDTO);
    }

    /**
     * Add a new member to a group
     * @param groupId the ID of the group
     * @param userId the ID of the user
     * @return the added group member
     */
    @PostMapping("/{groupId}/members/{userId}")
    public ResponseEntity<GroupMemberDTO> addMemberToGroup(@PathVariable Long groupId, 
                                                           @PathVariable Long userId) {
        var groupMember = groupMemberService.addMemberToGroup(groupId, userId);
        GroupMemberDTO groupMemberDTO = groupMemberMapper.groupMemberToGroupMemberDTO(groupMember);
        return ResponseEntity.ok(groupMemberDTO);
    }

    /**
     * Add multiple members to a group
     * @param groupId the ID of the group
     * @param userIds a list of user IDs to add
     * @return the number of added members
     */
    @PostMapping("/{groupId}/members/bulk")
    public ResponseEntity<Integer> addMembersToGroup(@PathVariable Long groupId,
                                                     @RequestBody @Valid List<Long> userIds) {
        int addedMembersCount = groupMemberService.addMembersToGroup(groupId, userIds);
        return ResponseEntity.ok(addedMembersCount);
    }

    /**
     * Remove a member from a group
     * @param groupId the ID of the group
     * @param userId the ID of the user
     * @return response indicating success or failure
     */
    @DeleteMapping("/{groupId}/members/{userId}")
    public ResponseEntity<Void> removeMemberFromGroup(@PathVariable Long groupId,
                                                      @PathVariable Long userId) {
        groupMemberService.removeMemberFromGroup(groupId, userId);
        return ResponseEntity.noContent().build();
    }
}
