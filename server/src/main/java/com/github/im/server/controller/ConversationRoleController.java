package com.github.im.server.controller;

import com.github.im.server.model.ConversationMember;
import com.github.im.server.model.User;
import com.github.im.server.model.enums.ConversationMemberRole;
import com.github.im.server.repository.GroupMemberRepository;
import com.github.im.server.service.ConversationRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/groups") @RequiredArgsConstructor
public class ConversationRoleController {
    private final ConversationRoleService roleService; private final GroupMemberRepository memberRepository;
    @GetMapping("/{conversationId}/members/roles") public ResponseEntity<List<MemberRoleResponse>> list(@PathVariable Long conversationId, @AuthenticationPrincipal User user) {
        roleService.requireMember(conversationId, user.getUserId()); return ResponseEntity.ok(memberRepository.findByConversationId(conversationId).stream().map(this::toResponse).toList());
    }
    @PutMapping("/{conversationId}/members/{userId}/role") public ResponseEntity<MemberRoleResponse> update(@PathVariable Long conversationId, @PathVariable Long userId, @RequestBody UpdateRoleRequest request, @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(toResponse(roleService.updateRole(conversationId, user.getUserId(), userId, request.role())));
    }
    @PutMapping("/{conversationId}/owner") public ResponseEntity<Void> transfer(@PathVariable Long conversationId, @RequestBody TransferOwnerRequest request, @AuthenticationPrincipal User user) {
        roleService.transferOwnership(conversationId, user.getUserId(), request.userId()); return ResponseEntity.noContent().build();
    }
    private MemberRoleResponse toResponse(ConversationMember member) { return new MemberRoleResponse(String.valueOf(member.getUser().getUserId()), member.getRole()); }
    public record UpdateRoleRequest(ConversationMemberRole role) { } public record TransferOwnerRequest(Long userId) { } public record MemberRoleResponse(String userId, ConversationMemberRole role) { }
}
