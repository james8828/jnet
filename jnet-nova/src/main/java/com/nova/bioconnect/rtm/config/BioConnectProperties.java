package com.nova.bioconnect.rtm.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration for the Bio-Connect RTM HL7/DML interface.
 *
 * <pre>
 * bioconnect:
 *   inbound:        # MLLP server receiving ADT from HIS (RTMADTP entry point)
 *   lis:            # MLLP client sending Results (ORU/OUL) to LIS/HIS (RTMLIS exit point)
 *   dml:            # DML TCP server receiving OBS/SVC from RTMLIS (Server mode, primary)
 *   device:         # DML TCP client pushing PAT/OPL to devices (Client mode)
 *   dml-lis-client: # DML TCP client pulling OBS/SVC from devices (Client mode, optional)
 * </pre>
 *
 * <p>Communication architecture:
 * <ul>
 *   <li>Patient/Operator data: Java → DML Client → Device (push PAT.R01/OPL.R01)</li>
 *   <li>Observation/QC data: RTMLIS → DML Server → Java (passive receive OBS.R01/SVC.R01)</li>
 * </ul>
 */
@Validated
@ConfigurationProperties(prefix = "bioconnect")
public class BioConnectProperties {

    private ServerEndpoint inbound = new ServerEndpoint();
    private ClientEndpoint lis = new ClientEndpoint();
    private DmlEndpoint dml = new DmlEndpoint();
    private DmlClientEndpoint device = new DmlClientEndpoint();
    private DmlLisClientEndpoint dmlLisClient = new DmlLisClientEndpoint();
    private String sendingApplication = "BIO-CONNECT";
    private String sendingFacility = "NOVA";
    private String processingId = "P";
    private String version = "2.4";
    private String charset = "UTF-8";

    public ServerEndpoint getInbound() { return inbound; }
    public void setInbound(ServerEndpoint inbound) { this.inbound = inbound; }

    public ClientEndpoint getLis() { return lis; }
    public void setLis(ClientEndpoint lis) { this.lis = lis; }

    public DmlEndpoint getDml() { return dml; }
    public void setDml(DmlEndpoint dml) { this.dml = dml; }

    public DmlClientEndpoint getDevice() { return device; }
    public void setDevice(DmlClientEndpoint device) { this.device = device; }

    public DmlLisClientEndpoint getDmlLisClient() { return dmlLisClient; }
    public void setDmlLisClient(DmlLisClientEndpoint dmlLisClient) { this.dmlLisClient = dmlLisClient; }

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

    /** Inbound MLLP server settings (ADT from HIS). */
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

    /** Outbound MLLP client settings (LIS/HIS). Forwarding is TBD. */
    public static class ClientEndpoint {
        private boolean enabled = false;
        private String host = "127.0.0.1";
        @Min(1)
        private int port = 2590;
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

    /** DML TCP server settings (OBS/SVC from RTMLIS - Server mode, primary). */
    public static class DmlEndpoint {
        private boolean enabled = true;
        @Min(1)
        private int port = 57381;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
    }

    /** DML TCP client settings (PAT/OPL push to device - Client mode). */
    public static class DmlClientEndpoint {
        private boolean enabled = true;
        private String host = "127.0.0.1";
        @Min(1)
        private int port = 57380;
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

    /** DML LIS client settings (pull OBS/SVC from devices - Client mode, optional). */
    public static class DmlLisClientEndpoint {
        private boolean enabled = false;
        private String host = "127.0.0.1";
        @Min(1)
        private int port = 57380;
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
