package com.github.im.group.gui.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 时序性保证
 */
public class MessageFactory {

    private AtomicLong lastClientTimestamp = new AtomicLong(System.currentTimeMillis());

//    public Message buildMessage(String content) {
//        long ts = System.currentTimeMillis();
//        long clientTimestamp = Math.max(ts, lastClientTimestamp.incrementAndGet());
//
//        return new Message(content, clientTimestamp);
//    }
}
