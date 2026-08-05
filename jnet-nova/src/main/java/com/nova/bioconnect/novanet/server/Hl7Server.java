package com.nova.bioconnect.novanet.server;

import com.nova.bioconnect.novanet.config.BioConnectProperties;
import com.nova.bioconnect.novanet.mllp.MllpEncoder;
import com.nova.bioconnect.novanet.mllp.MllpFrameDecoder;
import com.nova.bioconnect.novanet.service.AdtService;
import com.nova.bioconnect.novanet.service.ResultService;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

/**
 * Netty TCP server listening for inbound HL7 MLLP messages (ADT from HIS, results from device).
 *
 * <p>The server starts once the application is ready (all beans, including the outbound clients,
 * have been initialised) and binds to {@code bioconnect.inbound.port}.
 */
@Component
public class Hl7Server {

    private static final Logger log = LoggerFactory.getLogger(Hl7Server.class);

    private final BioConnectProperties properties;
    private final AdtService adtService;
    private final ResultService resultService;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;
    private final Hl7ServerHandler handler;

    public Hl7Server(BioConnectProperties properties, AdtService adtService, ResultService resultService) {
        this.properties = properties;
        this.adtService = adtService;
        this.resultService = resultService;
        this.handler = new Hl7ServerHandler(adtService, resultService, properties);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        BioConnectProperties.ServerEndpoint inbound = properties.getInbound();
        if (!inbound.isEnabled()) {
            log.info("Inbound HL7 server disabled");
            return;
        }
        Charset charset = Charset.forName(properties.getCharset());
        bossGroup = new NioEventLoopGroup(1, new DefaultThreadFactory("hl7-server-boss"));
        workerGroup = new NioEventLoopGroup(inbound.getWorkerThreads(), new DefaultThreadFactory("hl7-server-worker"));

        ServerBootstrap bootstrap = new ServerBootstrap();
        bootstrap.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .childOption(ChannelOption.SO_KEEPALIVE, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new MllpFrameDecoder(charset));
                        ch.pipeline().addLast(new MllpEncoder());
                        ch.pipeline().addLast(handler);
                    }
                });

        try {
            serverChannel = bootstrap.bind(inbound.getPort()).sync().channel();
            log.info("Inbound HL7 server listening on port {} (MLLP, HL7 v{})",
                    inbound.getPort(), properties.getVersion());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Failed to bind HL7 server on port " + inbound.getPort(), e);
        }
    }

    @PreDestroy
    public void stop() {
        log.info("Stopping HL7 server...");
        if (serverChannel != null) {
            serverChannel.close();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
    }
}
