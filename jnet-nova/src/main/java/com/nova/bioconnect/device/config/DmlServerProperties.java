package com.nova.bioconnect.device.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * DML Server Configuration Properties
 */
@Data
@ConfigurationProperties(prefix = "dml.server")
public class DmlServerProperties {

    /**
     * DML TCP server port
     */
    private int port = 57380;

    /**
     * Boss (acceptor) thread count
     */
    private int bossThreads = 1;

    /**
     * Worker thread count (0 = default)
     */
    private int workerThreads = 0;

    /**
     * Read idle timeout in seconds
     */
    private int readIdleSeconds = 60;

    /**
     * Write idle timeout in seconds
     */
    private int writeIdleSeconds = 60;

    /**
     * KPA keep alive interval in seconds
     */
    private int kpaIntervalSeconds = 60;

    /**
     * Max KPA timeouts before disconnect
     */
    private int maxKpaTimeouts = 4;

    /**
     * Max message size in bytes
     */
    private int maxMessageSize = 32768;
}