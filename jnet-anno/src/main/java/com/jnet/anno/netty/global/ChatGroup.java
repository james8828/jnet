package com.jnet.anno.netty.global;

import io.netty.channel.Channel;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChatGroup {
    public static final ConcurrentMap<Integer, Channel> CHANNEL_MAP = new ConcurrentHashMap<>(16);
}
