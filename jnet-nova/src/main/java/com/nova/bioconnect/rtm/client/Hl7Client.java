package com.nova.bioconnect.rtm.client;

import com.nova.bioconnect.rtm.hl7.Hl7Message;
import com.nova.bioconnect.rtm.mllp.MllpEncoder;
import com.nova.bioconnect.rtm.mllp.MllpFrameDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

/**
 * A resilient HL7 MLLP client. Connects to a remote TCP endpoint, sends HL7 messages and
 * correlates the returned ACK with each outbound request by message control id (MSH-10 / MSA-2).
 *
 * <p>Automatically reconnects when the connection drops. {@link #send(Hl7Message)} fails fast
 * while the client is not connected; the returned future completes exceptionally on ACK timeout.
 */
public class Hl7Client {

    private static final Logger log = LoggerFactory.getLogger(Hl7Client.class);

    private final String name;
    private final String host;
    private final int port;
    private final long reconnectDelayMs;
    private final Charset charset;
    private final long ackTimeoutSeconds;

    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private final ConcurrentMap<String, CompletableFuture<Hl7Message>> pending = new ConcurrentHashMap<>();

    private volatile Channel channel;
    private volatile boolean running;

    public Hl7Client(String name, String host, int port, long reconnectDelayMs, Charset charset) {
        this(name, host, port, reconnectDelayMs, charset, 30);
    }

    public Hl7Client(String name, String host, int port, long reconnectDelayMs, Charset charset, long ackTimeoutSeconds) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.reconnectDelayMs = reconnectDelayMs;
        this.charset = charset;
        this.ackTimeoutSeconds = ackTimeoutSeconds;
        this.group = new NioEventLoopGroup(1, new DefaultThreadFactory("hl7-client-" + name));
        this.bootstrap = new Bootstrap();
        this.bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new MllpFrameDecoder(charset));
                        ch.pipeline().addLast(new MllpEncoder());
                        ch.pipeline().addLast(new Hl7ClientHandler(Hl7Client.this));
                    }
                });
    }

    public String getName() { return name; }
    public String getHost() { return host; }
    public int getPort() { return port; }
    public boolean isConnected() { return channel != null && channel.isActive(); }

    /** Start the client and attempt the initial connection. */
    public void start() {
        running = true;
        connect();
    }

    /** Stop the client and release resources. */
    public void stop() {
        running = false;
        if (channel != null) {
            channel.close();
        }
        group.shutdownGracefully();
        // fail any pending requests
        pending.values().forEach(f -> f.completeExceptionally(new IllegalStateException("client stopped")));
        pending.clear();
    }

    private void connect() {
        if (!running) {
            return;
        }
        log.info("[{}] Connecting to {}:{} ...", name, host, port);
        ChannelFuture future = bootstrap.connect(host, port);
        future.addListener((ChannelFuture f) -> {
            if (f.isSuccess()) {
                channel = f.channel();
                log.info("[{}] Connected to {}:{}", name, host, port);
            } else {
                log.warn("[{}] Connect failed: {}", name, f.cause().getMessage());
                scheduleReconnect();
            }
        });
    }

    /** Schedule a reconnect after the configured delay. Called by the handler on channel inactive. */
    public void scheduleReconnect() {
        if (!running) {
            return;
        }
        group.schedule(this::connect, reconnectDelayMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Send an HL7 message and return a future that completes with the ACK returned by the peer.
     * The future completes exceptionally if the client is not connected or the ACK times out.
     */
    public CompletableFuture<Hl7Message> send(Hl7Message message) {
        if (!isConnected()) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "[" + name + "] not connected to " + host + ":" + port));
        }
        String controlId = message.getMessageControlId();
        CompletableFuture<Hl7Message> future = new CompletableFuture<>();
        pending.put(controlId, future);
        future.orTimeout(ackTimeoutSeconds, TimeUnit.SECONDS)
                .whenComplete((r, e) -> pending.remove(controlId));

        channel.writeAndFlush(message).addListener((ChannelFuture f) -> {
            if (!f.isSuccess()) {
                pending.remove(controlId);
                future.completeExceptionally(f.cause());
            } else {
                log.debug("[{}] Sent {} (control id {})", name, message.getMessageType(), controlId);
            }
        });
        return future;
    }

    /** Complete a pending request with the received ACK. */
    public void completePending(String controlId, Hl7Message ack) {
        CompletableFuture<Hl7Message> f = pending.remove(controlId);
        if (f != null) {
            f.complete(ack);
        }
    }
}
