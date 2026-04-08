package com.github.im.server.controller;

import com.github.im.dto.meeting.MeetingCreateRequest;
import com.github.im.dto.meeting.MeetingDTO;
import com.github.im.dto.meeting.MeetingEndRequest;
import com.github.im.dto.meeting.MeetingJoinRequest;
import com.github.im.dto.meeting.MeetingLeaveRequest;
import com.github.im.server.model.User;
import com.github.im.server.service.MeetingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meetings")
@RequiredArgsConstructor
public class MeetingController {

    private final MeetingService meetingService;

    @PostMapping("/create")
    public ResponseEntity<MeetingDTO> create(@RequestBody MeetingCreateRequest request,
                                             @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(meetingService.createMeeting(request, user));
    }

    @PostMapping("/join")
    public ResponseEntity<MeetingDTO> join(@RequestBody MeetingJoinRequest request,
                                           @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(meetingService.joinMeeting(request, user));
    }

    @PostMapping("/leave")
    public ResponseEntity<Void> leave(@RequestBody MeetingLeaveRequest request,
                                      @AuthenticationPrincipal User user) {
        meetingService.leaveMeeting(request, user);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/end")
    public ResponseEntity<Void> end(@RequestBody MeetingEndRequest request,
                                    @AuthenticationPrincipal User user) {
        meetingService.endMeeting(request, user);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/room/{roomId}")
    public ResponseEntity<MeetingDTO> getByRoom(@PathVariable String roomId) {
        return ResponseEntity.ok(meetingService.getByRoomId(roomId));
    }
}
