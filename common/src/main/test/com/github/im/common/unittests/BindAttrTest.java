package com.github.im.common.unittests;

import com.github.im.common.connect.connection.ReactiveConnectionManager;
import com.github.im.common.connect.connection.server.BindAttr;
import com.github.im.common.connect.enums.PlatformType;
import com.github.im.common.connect.model.proto.Account;
import com.github.im.common.connect.model.proto.BaseMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Sinks;

import static org.junit.jupiter.api.Assertions.*;

public class BindAttrTest {

    private Account.AccountInfo mockAccountInfo;

    @BeforeEach
    public void setUp() {
        // 创建一个模拟的 Account.AccountInfo 对象
        mockAccountInfo = Mockito.mock(Account.AccountInfo.class);
        Mockito.when(mockAccountInfo.getAccount()).thenReturn("testAccount");
        Mockito.when(mockAccountInfo.getPlatformType()).thenReturn(Account.PlatformType.ANDROID);
    }

    @Test
    public void testGetBindAttr() {
        // 调用 getBindAttr 方法
        BindAttr<String> bindAttr = BindAttr.getBindAttr(mockAccountInfo);

        // 验证生成的 key 是否正确
        String expectedKey = "testAccount_MOBILE";
        assertEquals(expectedKey, bindAttr.getKey());
    }

    @Test
    public void testEqualsAndHashCode() {
        // 创建两个相同的 BindAttr 实例
        BindAttr<String> bindAttr1 = BindAttr.getBindAttr(mockAccountInfo);
        BindAttr<String> bindAttr2 = BindAttr.getBindAttr(mockAccountInfo);

        // 验证 equals 方法
        assertTrue(bindAttr1.equals(bindAttr2));
        assertTrue(bindAttr2.equals(bindAttr1));

        // 验证 hashCode 方法
        assertEquals(bindAttr1.hashCode(), bindAttr2.hashCode());

        // 创建一个不同的 BindAttr 实例
        Mockito.when(mockAccountInfo.getAccount()).thenReturn("anotherAccount");
        BindAttr<String> bindAttr3 = BindAttr.getBindAttr(mockAccountInfo);

        // 验证 equals 方法
        assertFalse(bindAttr1.equals(bindAttr3));
        assertFalse(bindAttr3.equals(bindAttr1));

        // 验证 hashCode 方法
        assertNotEquals(bindAttr1.hashCode(), bindAttr3.hashCode());
    }

    @Test
    public void testRegisterAndGetSinkFlow() {
        // 创建一个 BindAttr 实例
        BindAttr<String> bindAttr = BindAttr.getBindAttr(mockAccountInfo);

        // 注册 Sink 流
        Sinks.Many<BaseMessage.BaseMessagePkg> registeredSink = ReactiveConnectionManager.registerSinkFlow(bindAttr);

        // 获取已注册的 Sink 流
        Sinks.Many<BaseMessage.BaseMessagePkg> retrievedSink = ReactiveConnectionManager.getSinkFlow(bindAttr);

        // 验证获取的 Sink 流是否与注册的 Sink 流相同
        assertNotNull(retrievedSink);
        assertSame(registeredSink, retrievedSink);

        // 尝试再次注册相同的 BindAttr
        Sinks.Many<BaseMessage.BaseMessagePkg> secondRegisteredSink = ReactiveConnectionManager.registerSinkFlow(bindAttr);

        // 验证第二次注册的 Sink 流是否与第一次注册的 Sink 流相同
        assertSame(registeredSink, secondRegisteredSink);
    }
}
