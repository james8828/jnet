package com.nova.bioconnect.rtm.dml;

import com.nova.bioconnect.rtm.config.BioConnectProperties;
import com.nova.bioconnect.rtm.entity.SampleEntity;
import com.nova.bioconnect.rtm.repository.SampleRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * DML Client mode for RTMLIS - pushes observation/QC data to devices.
 *
 * <p>NOTE: This is a secondary mode. The primary mode is Server mode ({@link DmlTcpServer})
 * where RTMLIS actively pushes data to Java. This Client mode is only needed when
 * devices do not support Server mode and need to be polled.
 *
 * <p>Implements the Client-mode DML communication as seen in the C# RTMLIS source:
 * <pre>
 *   1. Periodically query DB for untransmitted samples (transmitted_flag = 'F')
 *   2. Connect to device TCP endpoint
 *   3. Send HEL.R01 → wait ACK
 *   4. Send DST.R01 → wait ACK
 *   5. Send OBS.R01/OBS.R02 → wait ACK
 *   6. Send EOT.R01 → wait ACK
 *   7. Mark sample as transmitted
 * </pre>
 *
 * <p>Connection is ephemeral: connect → send sequence → disconnect.
 * Auto-retry on failure with configurable interval.
 *
 * <p>Enable with: {@code bioconnect.dml-lis-client.enabled=true}
 */
@ConditionalOnProperty(name = "bioconnect.dml-lis-client.enabled", havingValue = "true")
@Component
public class DmlLisClientManager {

    private static final Logger log = LoggerFactory.getLogger(DmlLisClientManager.class);

    private final BioConnectProperties properties;
    private final DmlMessageParser parser;
    private final DmlMessageBuilder builder;
    private final SampleRepository sampleRepository;

    private EventLoopGroup group;
    private ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    // Tracks messageIds for ACK correlation during a single session
    private final Map<String, CompletableFuture<String>> sessionPending = new ConcurrentHashMap<>();

    public DmlLisClientManager(BioConnectProperties properties,
                                DmlMessageParser parser,
                                DmlMessageBuilder builder,
                                SampleRepository sampleRepository) {
        this.properties = properties;
        this.parser = parser;
        this.builder = builder;
        this.sampleRepository = sampleRepository;
    }

    @PostConstruct
    public void start() {
        BioConnectProperties.DmlLisClientEndpoint dml = properties.getDmlLisClient();
        if (!dml.isEnabled()) {
            log.info("DML LIS client disabled");
            return;
        }
        group = new NioEventLoopGroup(1, new DefaultThreadFactory("dml-lis-client"));
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "dml-lis-scheduler");
            t.setDaemon(true);
            return t;
        });
        running = true;
        long interval = Math.max(200, dml.getReconnectDelayMs());
        scheduler.scheduleWithFixedDelay(this::pollAndPush, 1000, interval, TimeUnit.MILLISECONDS);
        log.info("DML LIS client started, polling every {}ms", interval);
    }

    private void pollAndPush() {
        if (!running) return;
        try {
            List<SampleEntity> pending = sampleRepository.findByTransmittedFlag("F");
            if (pending.isEmpty()) {
                log.debug("No pending samples to push");
                return;
            }
            log.info("Found {} pending samples to push", pending.size());
            for (SampleEntity sample : pending) {
                if (!running) break;
                try {
                    pushSample(sample);
                } catch (Exception e) {
                    log.error("Failed to push sample {}: {}", sample.getSampleKeyNum(), e.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Error polling for pending samples", e);
        }
    }

    /**
     * Push a single sample through the full DML handshake sequence.
     */
    private void pushSample(SampleEntity sample) throws Exception {
        BioConnectProperties.DmlLisClientEndpoint dml = properties.getDmlLisClient();
        log.info("Pushing sample {} (device={}, controlType={})",
                sample.getSampleKeyNum(), sample.getDeviceSerial(), sample.getControlType());

        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(group)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .option(ChannelOption.TCP_NODELAY, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ch.pipeline().addLast(new DmlFrameDecoder());
                        ch.pipeline().addLast(new DmlResponseHandler(parser, sessionPending));
                    }
                });

        Channel channel = bootstrap.connect(new InetSocketAddress(dml.getHost(), dml.getPort()))
                .sync()
                .channel();

        try {
            String controlType = sample.getControlType();
            boolean isQc = !"OBS".equals(controlType);
            String obsType = isQc ? "OBS.R02" : "OBS.R01";

            // Step 1: Send HEL.R01
            sendAndWaitAck(channel, buildHelMessage(sample));
            log.debug("HEL.R01 sent and ACKed");

            // Step 2: Send DST.R01
            sendAndWaitAck(channel, buildDstMessage(sample));
            log.debug("DST.R01 sent and ACKed");

            // Step 3: Send OBS.R01/OBS.R02
            String obsMsg = buildObsMessage(sample, obsType);
            sendAndWaitAck(channel, obsMsg);
            log.debug("{} sent and ACKed", obsType);

            // Step 4: Send EOT.R01
            sendAndWaitAck(channel, buildEotMessage(obsType));
            log.debug("EOT.R01 sent and ACKed");

            // Mark as transmitted
            sample.setTransmittedFlag("T");
            sampleRepository.save(sample);
            log.info("Sample {} pushed successfully and marked transmitted", sample.getSampleKeyNum());

        } finally {
            channel.close().sync();
        }
    }

    private String sendAndWaitAck(Channel channel, String xml) throws Exception {
        DmlMessage msg = parser.parse(xml);
        String messageId = msg.messageId();
        if (messageId == null || messageId.isEmpty()) {
            messageId = "MSG_" + System.currentTimeMillis();
            xml = addMessageId(xml, messageId);
        }

        CompletableFuture<String> ackFuture = new CompletableFuture<>();
        sessionPending.put(messageId, ackFuture);

        ByteBuf buffer = Unpooled.copiedBuffer(xml.getBytes(StandardCharsets.UTF_8));
        channel.writeAndFlush(buffer).sync();

        String ack;
        try {
            ack = ackFuture.get(10, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            sessionPending.remove(messageId);
            throw new TimeoutException("ACK timeout for " + messageId);
        }
        sessionPending.remove(messageId);

        DmlMessage ackMsg = parser.parse(ack);
        if (!ackMsg.isPositiveAck()) {
            throw new RuntimeException("Negative ACK for " + messageId + ": " + ackMsg.ackCode());
        }
        return ack;
    }

    private String buildHelMessage(SampleEntity sample) {
        String msgId = genMessageId();
        return "<HEL.R01>" +
                "<HDR control_id=\"" + msgId + "\" version_id=\"1.0\" creation_dttm=\"" + nowDml() + "\"/>" +
                "<DEV>" +
                "<DEV.vendor_id V=\"" + esc(sample.getDeviceType()) + "^" + esc(sample.getDeviceName()) + "^\"/>" +
                "<DEV.device_id V=\"" + esc(sample.getDeviceSerial()) + "\">" +
                esc(sample.getFacName()) + "^" + esc(sample.getLocName()) +
                "</DEV.device_id>" +
                "<DEV.model_id V=\"" + esc(sample.getDeviceType()) + "\"/>" +
                "<DEV.serial_id V=\"" + esc(sample.getDeviceSerial()) + "\"/>" +
                "<DSC><DSC.connection_profile_cd V=\"SA\"/></DSC>" +
                "</DEV>" +
                "</HEL.R01>";
    }

    private String buildDstMessage(SampleEntity sample) {
        String msgId = genMessageId();
        String dttm = nowDml();
        return "<DST.R01>" +
                "<HDR control_id=\"" + msgId + "\" version_id=\"1.0\" creation_dttm=\"" + dttm + "\"/>" +
                "<DST>" +
                "<DST.status_dttm V=\"" + dttm + "\"/>" +
                "<DST.new_observations_qty V=\"1\"/>" +
                "<DST.condition_cd V=\"R\"/>" +
                "<DST.patients_update_dttm V=\"" + dttm + "\"/>" +
                "<DST.operators_update_dttm V=\"" + dttm + "\"/>" +
                "</DST>" +
                "</DST.R01>";
    }

    private String buildObsMessage(SampleEntity sample, String obsType) {
        String msgId = genMessageId();
        String xmlText = sample.getXmlText();
        if (xmlText == null || xmlText.isEmpty()) {
            xmlText = "<SVC><SVC.control_type V=\"" + esc(sample.getControlType()) + "\"/></SVC>";
        }
        return "<" + obsType + ">" +
                "<HDR control_id=\"" + msgId + "\" version_id=\"1.0\" creation_dttm=\"" + nowDml() + "\"/>" +
                xmlText +
                "</" + obsType + ">";
    }

    private String buildEotMessage(String topicCd) {
        String msgId = genMessageId();
        return "<EOT.R01>" +
                "<HDR control_id=\"" + msgId + "\" version_id=\"1.0\" creation_dttm=\"" + nowDml() + "\"/>" +
                "<EOT>" +
                "<EOT.topic_cd V=\"" + esc(topicCd) + "\"/>" +
                "<EOT.update_dttm V=\"" + nowDml() + "\"/>" +
                "</EOT>" +
                "</EOT.R01>";
    }

    private String genMessageId() {
        return String.valueOf(4000 + (int)(System.currentTimeMillis() % 10000));
    }

    private String nowDml() {
        return java.time.LocalDateTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String esc(String s) {
        if (s == null) return "";
        return s.replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("&", "&amp;").replace("'", "&apos;");
    }

    private String addMessageId(String xml, String messageId) {
        int hdrIdx = xml.indexOf("<HDR");
        if (hdrIdx < 0) return xml;
        int endHdr = xml.indexOf(">", hdrIdx) + 1;
        String prefix = xml.substring(0, hdrIdx);
        String hdr = xml.substring(hdrIdx, endHdr);
        String suffix = xml.substring(endHdr);
        if (hdr.contains("control_id")) return xml;
        String newHdr = "<HDR control_id=\"" + messageId + "\"" + hdr.substring(4);
        return prefix + newHdr + suffix;
    }

    @PreDestroy
    public void stop() {
        running = false;
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        if (group != null) {
            group.shutdownGracefully();
        }
        log.info("DML LIS client stopped");
    }
}