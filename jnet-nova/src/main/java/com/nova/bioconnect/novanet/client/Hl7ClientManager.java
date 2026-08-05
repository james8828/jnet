package com.nova.bioconnect.novanet.client;

import com.nova.bioconnect.novanet.config.BioConnectProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

/**
 * Manages the two outbound HL7 MLLP clients used by Bio-Connect:
 * <ul>
 *   <li><b>device</b> &mdash; forwards ADT messages to the device/instrument (e.g. NOVANET).</li>
 *   <li><b>lis</b> &mdash; forwards result messages (ORU^R01 / OUL^R21) to the LIS/HIS.</li>
 * </ul>
 *
 * <p>Clients are started/stopped with the Spring application context.
 */
@Slf4j
@Component
public class Hl7ClientManager {

    private final BioConnectProperties properties;
    private Hl7Client deviceClient;
    private Hl7Client lisClient;

    public Hl7ClientManager(BioConnectProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void start() {
        Charset charset = Charset.forName(properties.getCharset());
        BioConnectProperties.ClientEndpoint device = properties.getDevice();
        if (device.isEnabled()) {
            deviceClient = new Hl7Client("device", device.getHost(), device.getPort(),
                    device.getReconnectDelayMs(), charset);
            deviceClient.start();
            log.info("Device HL7 client -> {}:{}", device.getHost(), device.getPort());
        }
        BioConnectProperties.ClientEndpoint lis = properties.getLis();
        if (lis.isEnabled()) {
            lisClient = new Hl7Client("lis", lis.getHost(), lis.getPort(),
                    lis.getReconnectDelayMs(), charset);
            lisClient.start();
            log.info("LIS HL7 client -> {}:{}", lis.getHost(), lis.getPort());
        }
    }

    @PreDestroy
    public void stop() {
        if (deviceClient != null) {
            deviceClient.stop();
        }
        if (lisClient != null) {
            lisClient.stop();
        }
    }

    /** The client used to forward ADT messages to the device/instrument. May be null if disabled. */
    public Hl7Client getDeviceClient() {
        return deviceClient;
    }

    /** The client used to forward result messages to the LIS/HIS. May be null if disabled. */
    public Hl7Client getLisClient() {
        return lisClient;
    }
}
