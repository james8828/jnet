package com.nova.bioconnect.rtm.dml;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class DmlTcpClient {

    private final String host;
    private final int port;
    private EventLoopGroup group;
    private Channel channel;
    private final Map<String, CompletableFuture<String>> pendingResponses = new ConcurrentHashMap<>();
    private final DmlMessageParser parser;

    public DmlTcpClient(String host, int port, DmlMessageParser parser) {
        this.host = host;
        this.port = port;
        this.parser = parser;
    }

    public void connect() {
        group = new NioEventLoopGroup(1);
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
            .channel(NioSocketChannel.class)
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .option(ChannelOption.TCP_NODELAY, true)
            .handler(new ChannelInitializer<SocketChannel>() {
                @Override
                protected void initChannel(SocketChannel ch) {
                    ch.pipeline().addLast(new DmlFrameDecoder());
                    ch.pipeline().addLast(new DmlResponseHandler(parser, pendingResponses));
                }
            });

        try {
            channel = bootstrap.connect(new InetSocketAddress(host, port)).sync().channel();
            log.info("DML client connected to {}:{}", host, port);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to connect DML client to {}:{}", host, port, e);
        }
    }

    public CompletableFuture<String> send(String message, String messageId) {
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingResponses.put(messageId, future);
        if (channel != null && channel.isActive()) {
            ByteBuf buffer = Unpooled.copiedBuffer(message.getBytes(StandardCharsets.UTF_8));
            channel.writeAndFlush(buffer).addListener(f -> {
                if (!f.isSuccess()) {
                    pendingResponses.remove(messageId);
                    future.completeExceptionally(f.cause());
                }
            });
        } else {
            pendingResponses.remove(messageId);
            future.completeExceptionally(new IllegalStateException("Not connected"));
        }
        return future;
    }

    public void disconnect() {
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        pendingResponses.values().forEach(f -> f.completeExceptionally(new RuntimeException("Disconnected")));
        pendingResponses.clear();
    }

    public boolean isConnected() {
        return channel != null && channel.isActive();
    }
}