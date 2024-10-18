package com.github.im.common.model.account.session;

import lombok.Data;
import lombok.EqualsAndHashCode;
import java.io.Serializable;

/**
 * @author pengpeng
 * @description
 * @date 2023/3/28
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SessionAccountContextVo extends SessionInfoVo implements Serializable {


//    private Map<Long,AccountInfo> sessionAccountContext;  // session 用户容器  k userId value AccountInfo


}
