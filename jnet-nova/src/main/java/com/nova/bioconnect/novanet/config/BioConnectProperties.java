package com.nova.bioconnect.novanet.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the Bio-Connect HL7 interface.
 *
 * <pre>
 * bioconnect:
 *   inbound:        # MLLP server receiving ADT (from HIS) and Results (from device)
 *   device:         # MLLP client sending ADT to device/instrument
 *   lis:            # MLLP client sending Results to LIS/HIS
 * </pre>
 */
@Validated
@ConfigurationProperties(prefix = "bioconnect")
public class BioConnectProperties {

    private ServerEndpoint inbound = new ServerEndpoint();
    private ClientEndpoint device = new ClientEndpoint();
    private ClientEndpoint lis = new ClientEndpoint();
    private String sendingApplication = "BIO-CONNECT";
    private String sendingFacility = "NOVA";
    private String processingId = "P";
    private String version = "2.4";
    private String charset = "UTF-8";

    public ServerEndpoint getInbound() { return inbound; }
    public void setInbound(ServerEndpoint inbound) { this.inbound = inbound; }

    public ClientEndpoint getDevice() { return device; }
    public void setDevice(ClientEndpoint device) { this.device = device; }

    public ClientEndpoint getLis() { return lis; }
    public void setLis(ClientEndpoint lis) { this.lis = lis; }

    public String getSendingApplication() { return sendingApplication; }
    public void setSendingApplication(String sendingApplication) { this.sendingApplication = sendingApplication; }

    public String getSendingFacility() { return sendingFacility; }
    public void setSendingFacility(String sendingFacility) { this.sendingFacility = sendingFacility; }

    public String getProcessingId() { return processingId; }
    public void setProcessingId(String processingId) { this.processingId = processingId; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getCharset() { return charset; }
    public void setCharset(String charset) { this.charset = charset; }

    /** Inbound MLLP server settings. */
    public static class ServerEndpoint {
        private boolean enabled = true;
        @Min(1)
        private int port = 2575;
        private int workerThreads = 4;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public int getWorkerThreads() { return workerThreads; }
        public void setWorkerThreads(int workerThreads) { this.workerThreads = workerThreads; }
    }

    /** Outbound MLLP client settings (device or LIS). */
    public static class ClientEndpoint {
        private boolean enabled = true;
        private String host = "127.0.0.1";
        @Min(1)
        private int port = 2580;
        private long reconnectDelayMs = 5000;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public long getReconnectDelayMs() { return reconnectDelayMs; }
        public void setReconnectDelayMs(long reconnectDelayMs) { this.reconnectDelayMs = reconnectDelayMs; }
    }
}
