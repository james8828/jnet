package com.nova.bioconnect.icpmgr.protocol;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * DML TCP Server based on Netty
 * Listens for DML protocol connections from devices
 */
@Slf4j
@Component
public class DmlTcpServer {

    /**
     * -- GETTER --
     *  Get the server port
     */
    @Getter
    @Value("${dml.server.port:57380}")
    private int port;

    @Value("${dml.server.boss-threads:1}")
    private int bossThreads;

    @Value("${dml.server.worker-threads:0}")
    private int workerThreads;

    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    private Channel serverChannel;

    private final DmlServerHandler serverHandler;

    public DmlTcpServer(DmlServerHandler serverHandler) {
        this.serverHandler = serverHandler;
    }

    /**
     * Start the DML TCP server when application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        try {
            bossGroup = new NioEventLoopGroup(bossThreads);
            workerGroup = new NioEventLoopGroup(workerThreads);

            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_RCVBUF, 65536)
                    .childOption(ChannelOption.SO_SNDBUF, 65536)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ch.pipeline()
                                    .addLast("frameDecoder", new DmlXmlFrameDecoder())
                                    .addLast("stringEncoder", new DmlStringEncoder())
                                    .addLast("idleStateHandler",
                                            new IdleStateHandler(60, 60, 0, TimeUnit.SECONDS))
                                    .addLast("serverHandler", serverHandler);
                        }
                    });

            serverChannel = bootstrap.bind(port).sync().channel();
            log.info("DML TCP Server started on port {}", port);

        } catch (InterruptedException e) {
            log.error("Failed to start DML TCP server", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Stop the DML TCP server
     */
    public void stop() {
        try {
            if (serverChannel != null) {
                serverChannel.close().sync();
            }
            if (bossGroup != null) {
                bossGroup.shutdownGracefully();
            }
            if (workerGroup != null) {
                workerGroup.shutdownGracefully();
            }
            log.info("DML TCP Server stopped");
        } catch (InterruptedException e) {
            log.error("Error stopping DML TCP server", e);
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Check if server is running
     */
    public boolean isRunning() {
        return serverChannel != null && serverChannel.isActive();
    }
}