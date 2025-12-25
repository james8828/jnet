package com.jnet.anno.netty.global;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * @author gjt
 * @date 2024/08/29
 */
public class ChannelSupervise {

    public static final ConcurrentMap<Channel, Long> CHANNEL_MAP = new ConcurrentHashMap<>(16);
    private static final ChannelGroup CHANNEL_GROUP = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private static final ConcurrentMap<String, ChannelId> CHANNEL_ID_MAP = new ConcurrentHashMap();

    public static void addChannel(Channel channel) {
        CHANNEL_GROUP.add(channel);
        CHANNEL_ID_MAP.put(channel.id().asShortText(), channel.id());
    }

    public static void removeChannel(Channel channel) {
        CHANNEL_GROUP.remove(channel);
        CHANNEL_ID_MAP.remove(channel.id().asShortText());
    }

    public static void send2All(TextWebSocketFrame tws) {
        CHANNEL_GROUP.writeAndFlush(tws);
    }

    public static void addChannelTest(Channel channel, Long slideId) {
        CHANNEL_MAP.put(channel, slideId);
    }

    public static void removeChannelTest(Channel channel) {
        CHANNEL_MAP.remove(channel);
    }
}
