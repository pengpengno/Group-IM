package com.github.im.server.repository;

import com.github.im.server.model.MeetingParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MeetingParticipantRepository extends JpaRepository<MeetingParticipant, Long> {
    Optional<MeetingParticipant> findByMeeting_MeetingIdAndUser_UserId(Long meetingId, Long userId);
    List<MeetingParticipant> findByMeeting_MeetingId(Long meetingId);
}
