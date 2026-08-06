package com.nova.bioconnect.rtm.dml;

import com.nova.bioconnect.rtm.config.BioConnectProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the outbound DML TCP client for pushing patient/operator data to devices.
 *
 * <p>This is the RTMOPL/PAT exit point:
 * <pre>
 *   PatientService / OperatorService -> DmlClientManager -> DML TCP -> Device
 * </pre>
 *
 * <p>Auto-reconnects on disconnection. Message sending is fire-and-forget with an
 * optional ACK future. Messages are identified by their MessageId for ACK correlation.
 */
@Component
public class DmlClientManager {

    private static final Logger log = LoggerFactory.getLogger(DmlClientManager.class);

    private final BioConnectProperties properties;
    private final DmlMessageParser parser;

    private EventLoopGroup group;
    private Channel channel;
    private final Map<String, CompletableFuture<String>> pendingResponses = new ConcurrentHashMap<>();
    private volatile boolean reconnectScheduled = false;

    public DmlClientManager(BioConnectProperties properties, DmlMessageParser parser) {
        this.properties = properties;
        this.parser = parser;
    }

    @PostConstruct
    public void start() {
        BioConnectProperties.DmlClientEndpoint device = properties.getDevice();
        if (!device.isEnabled()) {
            log.info("DML device client disabled");
            return;
        }
        group = new NioEventLoopGroup(1, new DefaultThreadFactory("dml-client"));
        connectAsync();
    }

    private void connectAsync() {
        BioConnectProperties.DmlClientEndpoint device = properties.getDevice();
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.AUTO_READ, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new DmlFrameDecoder());
                        ch.pipeline().addLast(new DmlResponseHandler(parser, pendingResponses));
                        ch.pipeline().addLast(new ChannelInboundHandlerAdapter() {
                            @Override
                            public void channelInactive(ChannelHandlerContext ctx) {
                                log.warn("DML device connection closed, scheduling reconnect...");
                                scheduleReconnect();
                            }

                            @Override
                            public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
                                log.warn("DML device connection error: {}", cause.getMessage());
                                ctx.close();
                            }
                        });
                    }
                });

        bootstrap.connect(new InetSocketAddress(device.getHost(), device.getPort()))
                .addListener((ChannelFutureListener) cf -> {
                    if (cf.isSuccess()) {
                        channel = cf.channel();
                        log.info("DML device connected to {}:{}", device.getHost(), device.getPort());
                    } else {
                        log.warn("DML device connect failed to {}:{}: {}",
                                device.getHost(), device.getPort(), cf.cause().getMessage());
                        scheduleReconnect();
                    }
                });
    }

    private void scheduleReconnect() {
        if (reconnectScheduled) return;
        reconnectScheduled = true;
        BioConnectProperties.DmlClientEndpoint device = properties.getDevice();
        group.schedule(() -> {
            reconnectScheduled = false;
            log.info("Attempting DML device reconnect...");
            connectAsync();
        }, device.getReconnectDelayMs(), java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    /**
     * Send a DML message to the device and return a future that completes when the ACK is received.
     *
     * @param xmlMessage the DML XML message string
     * @param messageId  the MessageId from the DML XML header (for ACK correlation)
     * @return a future completing with the raw ACK response string, or failing if not connected
     */
    public CompletableFuture<String> send(String xmlMessage, String messageId) {
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingResponses.put(messageId, future);

        if (channel != null && channel.isActive()) {
            ByteBuf buffer = Unpooled.copiedBuffer(xmlMessage.getBytes(StandardCharsets.UTF_8));
            channel.writeAndFlush(buffer).addListener(f -> {
                if (!f.isSuccess()) {
                    log.error("Failed to send DML message {}: {}", messageId, f.cause().getMessage());
                    pendingResponses.remove(messageId);
                    future.completeExceptionally(f.cause());
                }
            });
            log.debug("DML message {} sent to device ({} bytes)", messageId, xmlMessage.length());
        } else {
            pendingResponses.remove(messageId);
            future.completeExceptionally(new IllegalStateException("DML device not connected"));
            log.warn("Cannot send DML message {}: device not connected", messageId);
        }
        return future;
    }

    /**
     * Send a DML message without waiting for ACK (fire-and-forget).
     */
    public void sendOneway(String xmlMessage) {
        if (channel != null && channel.isActive()) {
            ByteBuf buffer = Unpooled.copiedBuffer(xmlMessage.getBytes(StandardCharsets.UTF_8));
            channel.writeAndFlush(buffer).addListener(f -> {
                if (!f.isSuccess()) {
                    log.warn("DML one-way send failed: {}", f.cause().getMessage());
                }
            });
        } else {
            log.warn("Cannot send DML message: device not connected");
        }
    }

    public boolean isConnected() {
        return channel != null && channel.isActive();
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping DML device client...");
        if (channel != null) {
            channel.close();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        pendingResponses.values().forEach(f -> f.completeExceptionally(new RuntimeException("Client stopped")));
        pendingResponses.clear();
    }
}