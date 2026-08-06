package com.nova.bioconnect.rtm.dml;

import com.nova.bioconnect.rtm.config.BioConnectProperties;
import com.nova.bioconnect.rtm.service.DmlObservationService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

/**
 * Netty TCP server for DML protocol connections from devices (RTMLIS entry point).
 *
 * <p>Receives DML XML messages (OBS.R01, SVC.R01, etc.) from instruments,
 * delegates to {@link DmlObservationService} for processing and LIS forwarding,
 * and sends ACK back to the device.
 */
@Slf4j
@Component
public class DmlTcpServer {

    private final DmlObservationService observationService;
    private final DmlMessageParser dmlParser;
    private final DmlMessageBuilder dmlBuilder;
    private final BioConnectProperties properties;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    public DmlTcpServer(DmlObservationService observationService,
                        DmlMessageParser dmlParser,
                        DmlMessageBuilder dmlBuilder,
                        BioConnectProperties properties) {
        this.observationService = observationService;
        this.dmlParser = dmlParser;
        this.dmlBuilder = dmlBuilder;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        BioConnectProperties.DmlEndpoint dml = properties.getDml();
        if (!dml.isEnabled()) {
            log.info("DML TCP server disabled");
            return;
        }
        int port = dml.getPort();
        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("dml-server-boss"));
        workerGroup = new NioEventLoopGroup(4, new DefaultThreadFactory("dml-server-worker"));

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new DmlFrameDecoder());
                        ch.pipeline().addLast(new DmlServerHandler(observationService, dmlBuilder));
                    }
                });

        try {
            serverChannel = bootstrap.bind(port).sync().channel();
            log.info("DML TCP server listening on port {}", port);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to bind DML TCP server on port {}", port, e);
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping DML TCP server...");
        if (serverChannel != null) serverChannel.close();
        if (workerGroup != null) workerGroup.shutdownGracefully();
        if (bossGroup != null) bossGroup.shutdownGracefully();
    }

    /**
     * Handler for incoming DML messages from devices.
     */
    private static class DmlServerHandler extends SimpleChannelInboundHandler<String> {
        private static final Logger log = LoggerFactory.getLogger(DmlServerHandler.class);

        private final DmlObservationService observationService;
        private final DmlMessageBuilder dmlBuilder;

        DmlServerHandler(DmlObservationService observationService, DmlMessageBuilder dmlBuilder) {
            this.observationService = observationService;
            this.dmlBuilder = dmlBuilder;
        }

        @Override
        protected void channelRead0(ChannelHandlerContext ctx, String message) {
            log.debug("DML message received: length={}", message.length());

            try {
                DmlMessage parsed = observationService.processDmlMessage(message);
                String ack = dmlBuilder.buildObservationAck(parsed.sessionId(), parsed.messageId());
                ByteBuf buffer = Unpooled.copiedBuffer(ack.getBytes(StandardCharsets.UTF_8));
                ctx.writeAndFlush(buffer).addListener(f -> {
                    if (!f.isSuccess()) {
                        log.warn("Failed to send DML ACK: {}", f.cause().getMessage());
                    }
                });
            } catch (Exception e) {
                log.error("Error processing DML message", e);
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
            log.error("DML server error on channel {}", ctx.channel().remoteAddress(), cause);
            ctx.close();
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) {
            log.info("DML device connected: {}", ctx.channel().remoteAddress());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) {
            log.info("DML device disconnected: {}", ctx.channel().remoteAddress());
        }
    }
}