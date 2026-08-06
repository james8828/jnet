package com.nova.bioconnect.rtm.client;

import com.nova.bioconnect.rtm.config.BioConnectProperties;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.Charset;

/**
 * Manages the outbound HL7 MLLP client used by Bio-Connect RTM services.
 *
 * <p><b>LIS client</b> &mdash; forwards result messages (ORU^R01 / OUL^R21) to the LIS/HIS.
 * Used by the RTMLIS flow: device DML OBS/SVC results are converted to HL7 ORU and sent to LIS.
 *
 * <p>The device communication (patient/operator push) uses the DML protocol
 * (PAT.R01 / OPL.R01) via {@code DmlTcpClient}, not an HL7 MLLP client.
 *
 * <p>Clients are started/stopped with the Spring application context.
 */
@Slf4j
@Component
public class Hl7ClientManager {

    private final BioConnectProperties properties;
    private Hl7Client lisClient;

    public Hl7ClientManager(BioConnectProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void start() {
        Charset charset = Charset.forName(properties.getCharset());
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
        if (lisClient != null) {
            lisClient.stop();
        }
    }

    /**
     * The client used to forward result messages (ORU^R01 / OUL^R21) to the LIS/HIS.
     * May be null if disabled.
     */
    public Hl7Client getLisClient() {
        return lisClient;
    }
}