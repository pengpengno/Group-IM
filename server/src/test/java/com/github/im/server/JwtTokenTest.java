package com.github.im.server;

import com.github.im.server.model.User;
import com.github.im.server.utils.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jwt.JwtEncoder;

/**
 * Description:
 * <p>
 * </p>
 *
 * @author pengpeng
 * @version 1.0
 * @since 2024/11/22
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Slf4j
public class JwtTokenTest {


    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("generate token")
    void  createToken () {

        Long userId = 111L;
        var test = User.builder()
                .userId(111L)
                .username("test")
                .build();

        var token = jwtUtil.createToken(test);
        System.out.println(token);
        Assertions.assertNotNull(token);

        var tokenUserId = jwtUtil.parseToken(token);
        Assertions.assertEquals(userId,tokenUserId);

    }


}