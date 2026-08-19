package com.nova.bioconnect;

import com.nova.bioconnect.rtm.config.BioConnectProperties;
import com.nova.bioconnect.schedule.config.SyncProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Bio-Connect HL7 Interface application.
 *
 * <p>Implements an HL7 v2.4 middleware over TCP/IP using the Minimal Lower Layer
 * Protocol (MLLP). Acts as an intermediary between HIS/LIS and devices/instruments:
 * <ul>
 *   <li>Receives ADT messages from HIS (inbound server) and forwards them to devices.</li>
 *   <li>Receives Result messages (ORU^R01 / OUL^R21) from devices and forwards them to LIS.</li>
 *   <li>Returns/accepts MLLP acknowledgements (MSH + MSA) for every message.</li>
 *   <li>Synchronizes patient and operator data from HIS via scheduled tasks.</li>
 * </ul>
 */
@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({BioConnectProperties.class, SyncProperties.class})
public class BioConnectApplication {

    public static void main(String[] args) {
        SpringApplication.run(BioConnectApplication.class, args);
    }
}
