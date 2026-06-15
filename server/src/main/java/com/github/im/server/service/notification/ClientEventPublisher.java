package com.github.im.server.service.notification;

import com.github.im.server.model.Meeting;
import com.github.im.server.model.Message;
import com.github.im.server.model.User;

import java.util.List;

public interface ClientEventPublisher {

    void publishChatMessageCreated(Message message, User sender, List<User> recipients);

    void publishMeetingInviteCreated(Meeting meeting, User host, List<User> recipients);
}
