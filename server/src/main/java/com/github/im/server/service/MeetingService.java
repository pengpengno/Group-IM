package com.github.im.server.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.im.common.connect.model.proto.Chat;
import com.github.im.dto.meeting.MeetingCreateRequest;
import com.github.im.dto.meeting.MeetingDTO;
import com.github.im.dto.meeting.MeetingEndRequest;
import com.github.im.dto.meeting.MeetingJoinRequest;
import com.github.im.dto.meeting.MeetingLeaveRequest;
import com.github.im.dto.meeting.MeetingParticipantDTO;
import com.github.im.dto.message.MeetingMessagePayLoad;
import com.github.im.server.model.Conversation;
import com.github.im.server.model.Meeting;
import com.github.im.server.model.MeetingParticipant;
import com.github.im.server.model.User;
import com.github.im.server.model.enums.MeetingParticipantRole;
import com.github.im.server.model.enums.MeetingParticipantStatus;
import com.github.im.server.model.enums.MeetingStatus;
import com.github.im.server.handler.SignalWebSocketHandler;
import com.github.im.server.repository.MeetingParticipantRepository;
import com.github.im.server.repository.MeetingRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MeetingService {

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository meetingParticipantRepository;
    private final MessageService messageService;
    private final ObjectMapper objectMapper;
    private final SignalWebSocketHandler signalHandler;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public MeetingDTO createMeeting(MeetingCreateRequest request, User creator) {
        if (request.getConversationId() == null) {
            throw new IllegalArgumentException("conversationId is required");
        }

        String roomId = request.getRoomId();
        if (roomId == null || roomId.isBlank()) {
            roomId = "meeting-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 8);
        }

        Conversation conversation = entityManager.getReference(Conversation.class, request.getConversationId());

        MeetingStatus status = MeetingStatus.ACTIVE;
        LocalDateTime scheduledAt = null;
        if (request.getScheduledAt() != null && !request.getScheduledAt().isBlank()) {
            try {
                scheduledAt = LocalDateTime.parse(request.getScheduledAt());
                status = MeetingStatus.SCHEDULED;
            } catch (Exception e) {
                // fallback or log error
            }
        }

        Meeting meeting = Meeting.builder()
                .conversation(conversation)
                .roomId(roomId)
                .title(request.getTitle() == null ? "Meeting" : request.getTitle())
                .createdBy(creator)
                .status(status)
                .scheduledAt(scheduledAt)
                .startedAt(status == MeetingStatus.ACTIVE ? LocalDateTime.now() : null)
                .build();

        meeting = meetingRepository.save(meeting);

        Set<Long> participantIds = new LinkedHashSet<>();
        if (request.getParticipantIds() != null) {
            participantIds.addAll(request.getParticipantIds());
        }
        participantIds.add(creator.getUserId());

        List<MeetingParticipant> participants = new ArrayList<>();
        for (Long userId : participantIds) {
            User user = entityManager.getReference(User.class, userId);
            MeetingParticipantRole role = userId.equals(creator.getUserId())
                    ? MeetingParticipantRole.HOST
                    : MeetingParticipantRole.PARTICIPANT;
            MeetingParticipantStatus participantStatus = userId.equals(creator.getUserId())
                    ? MeetingParticipantStatus.JOINED
                    : MeetingParticipantStatus.INVITED;
            MeetingParticipant participant = MeetingParticipant.builder()
                    .meeting(meeting)
                    .user(user)
                    .role(role)
                    .status(participantStatus)
                    .joinedAt(participantStatus == MeetingParticipantStatus.JOINED ? LocalDateTime.now() : null)
                    .build();
            participants.add(participant);
        }

        meetingParticipantRepository.saveAll(participants);

        boolean recordMessage = request.getRecordMessage() == null || request.getRecordMessage();
        if (recordMessage) {
            String action = meeting.getStatus() == MeetingStatus.SCHEDULED ? "SCHEDULE" : "START";
            publishMeetingMessage(meeting, creator, action);
        }

        // 发送实时呼叫信令 (如果是立即开始的会议)
        if (meeting.getStatus() == MeetingStatus.ACTIVE) {
            sendMeetingInvites(meeting, creator, participantIds);
        }

        return mapMeeting(meeting);
    }

    @Transactional
    public MeetingDTO joinMeeting(MeetingJoinRequest request, User user) {
        Meeting meeting = meetingRepository.findByRoomId(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("meeting not found"));

        MeetingParticipant participant = meetingParticipantRepository
                .findByMeeting_MeetingIdAndUser_UserId(meeting.getMeetingId(), user.getUserId())
                .orElseGet(() -> MeetingParticipant.builder()
                        .meeting(meeting)
                        .user(user)
                        .role(MeetingParticipantRole.PARTICIPANT)
                        .build());

        participant.setStatus(MeetingParticipantStatus.JOINED);
        if (participant.getJoinedAt() == null) {
            participant.setJoinedAt(LocalDateTime.now());
        }
        meetingParticipantRepository.save(participant);

        if (meeting.getStatus() != MeetingStatus.ACTIVE) {
            meeting.setStatus(MeetingStatus.ACTIVE);
        }

        return mapMeeting(meeting);
    }

    @Transactional
    public void leaveMeeting(MeetingLeaveRequest request, User user) {
        Meeting meeting = meetingRepository.findByRoomId(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("meeting not found"));

        meetingParticipantRepository.findByMeeting_MeetingIdAndUser_UserId(meeting.getMeetingId(), user.getUserId())
                .ifPresent(participant -> {
                    participant.setStatus(MeetingParticipantStatus.LEFT);
                    participant.setLeftAt(LocalDateTime.now());
                    meetingParticipantRepository.save(participant);
                });
    }

    @Transactional
    public void endMeeting(MeetingEndRequest request, User user) {
        Meeting meeting = meetingRepository.findByRoomId(request.getRoomId())
                .orElseThrow(() -> new IllegalArgumentException("meeting not found"));

        meeting.setStatus(MeetingStatus.ENDED);
        meeting.setEndedAt(LocalDateTime.now());
        meeting.setEndedBy(user);
        meetingRepository.save(meeting);

        meetingParticipantRepository.findByMeeting_MeetingId(meeting.getMeetingId())
                .forEach(participant -> {
                    if (participant.getStatus() == MeetingParticipantStatus.JOINED) {
                        participant.setStatus(MeetingParticipantStatus.LEFT);
                        participant.setLeftAt(LocalDateTime.now());
                        meetingParticipantRepository.save(participant);
                    }
                });

        boolean recordMessage = request.getRecordMessage() == null || request.getRecordMessage();
        if (recordMessage) {
            publishMeetingMessage(meeting, user, "END");
        }

        // 通知所有在线参与者会议已结束
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("fromUser", user.getUserId().toString());
        signalHandler.broadcastToMeeting(meeting.getRoomId(), "meeting/end", data);
    }

    @Transactional(readOnly = true)
    public MeetingDTO getByRoomId(String roomId) {
        Meeting meeting = meetingRepository.findByRoomId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("meeting not found"));
        return mapMeeting(meeting);
    }

    @Transactional(readOnly = true)
    public List<MeetingDTO> getMeetingsByConversation(Long conversationId) {
        return meetingRepository.findByConversation_ConversationIdOrderByCreatedAtDesc(conversationId)
                .stream()
                .map(this::mapMeeting)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MeetingDTO> getMyMeetings(Long userId) {
        return meetingRepository.findByParticipantUserId(userId)
                .stream()
                .map(this::mapMeeting)
                .collect(Collectors.toList());
    }

    private void publishMeetingMessage(Meeting meeting, User actor, String action) {
        MeetingMessagePayLoad payload = new MeetingMessagePayLoad();
        payload.setMeetingId(meeting.getMeetingId());
        payload.setRoomId(meeting.getRoomId());
        payload.setTitle(meeting.getTitle());
        payload.setAction(action);
        payload.setHostId(meeting.getCreatedBy().getUserId());
        payload.setScheduledAt(meeting.getScheduledAt());

        List<MeetingParticipant> participants = meetingParticipantRepository.findByMeeting_MeetingId(meeting.getMeetingId());
        List<Long> participantIds = participants.stream()
                .map(p -> p.getUser().getUserId())
                .collect(Collectors.toList());
        payload.setParticipantIds(participantIds);
        payload.setParticipantCount(participantIds.size());

        try {
            String content = objectMapper.writeValueAsString(payload);
            Chat.ChatMessage chatMessage = Chat.ChatMessage.newBuilder()
                    .setConversationId(meeting.getConversation().getConversationId())
                    .setContent(content)
                    .setType(Chat.MessageType.MEETING)
                    .setClientMsgId(UUID.randomUUID().toString())
                    .setFromUser(com.github.im.common.connect.model.proto.User.UserInfo.newBuilder()
                            .setUserId(actor.getUserId())
                            .setUsername(actor.getUsername())
                            .build())
                    .setClientTimeStamp(System.currentTimeMillis())
                    .build();
            messageService.handleMessage(chatMessage);
        } catch (Exception ignored) {
        }
    }

    private void sendMeetingInvites(Meeting meeting, User host, Set<Long> participantIds) {
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("fromUser", host.getUserId().toString());
        data.put("fromUserName", host.getUsername());
        data.put("roomId", meeting.getRoomId());
        data.put("title", meeting.getTitle());
        
        List<java.util.Map<String, Object>> others = participantIds.stream()
                .filter(id -> !id.equals(host.getUserId()))
                .map(id -> {
                    java.util.Map<String, Object> p = new java.util.HashMap<>();
                    p.put("userId", id.toString());
                    // 暂时只传ID，客户端通常会从缓存中找名字，或者由SignalWebSocketHandler在转发时补全profile(如果以后实现了)
                    return p;
                })
                .collect(Collectors.toList());
        data.put("participants", others);

        for (Long userId : participantIds) {
            if (userId.equals(host.getUserId())) continue;
            signalHandler.sendToUser(userId.toString(), "meeting/request", data);
        }
    }

    private MeetingDTO mapMeeting(Meeting meeting) {
        MeetingDTO dto = new MeetingDTO();
        dto.setMeetingId(meeting.getMeetingId());
        dto.setConversationId(meeting.getConversation().getConversationId());
        dto.setRoomId(meeting.getRoomId());
        dto.setTitle(meeting.getTitle());
        dto.setHostId(meeting.getCreatedBy().getUserId());
        dto.setStatus(meeting.getStatus().name());
        dto.setScheduledAt(meeting.getScheduledAt());
        dto.setStartedAt(meeting.getStartedAt());
        dto.setEndedAt(meeting.getEndedAt());

        List<MeetingParticipantDTO> participants = meetingParticipantRepository
                .findByMeeting_MeetingId(meeting.getMeetingId())
                .stream()
                .map(item -> {
                    MeetingParticipantDTO p = new MeetingParticipantDTO();
                    p.setUserId(item.getUser().getUserId());
                    p.setUsername(item.getUser().getUsername());
                    p.setRole(item.getRole().name());
                    p.setStatus(item.getStatus().name());
                    p.setJoinedAt(item.getJoinedAt());
                    p.setLeftAt(item.getLeftAt());
                    return p;
                })
                .collect(Collectors.toList());
        dto.setParticipants(participants);
        return dto;
    }
}
