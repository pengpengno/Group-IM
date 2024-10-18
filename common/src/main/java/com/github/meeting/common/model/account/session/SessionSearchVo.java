package com.github.meeting.common.model.account.session;

import lombok.Data;

import java.io.Serializable;
@Data
public class SessionSearchVo implements Serializable {

    private Long sessionId;

    private String sessionName;


}
