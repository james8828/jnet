package com.nova.bioconnect.rtm.service;

import com.nova.bioconnect.rtm.config.BioConnectProperties;
import com.nova.bioconnect.rtm.dml.DmlConstants;
import com.nova.bioconnect.rtm.dml.DmlMessage;
import com.nova.bioconnect.rtm.dml.DmlMessageBuilder;
import com.nova.bioconnect.rtm.dml.DmlMessageParser;
import com.nova.bioconnect.rtm.entity.ObservationResultEntity;
import com.nova.bioconnect.rtm.entity.QcResultEntity;
import com.nova.bioconnect.rtm.entity.SampleEntity;
import com.nova.bioconnect.rtm.repository.ObservationResultRepository;
import com.nova.bioconnect.rtm.repository.QcResultRepository;
import com.nova.bioconnect.rtm.repository.SampleRepository;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * RTMLIS equivalent service - receives DML observation and QC data from devices.
 *
 * <p>Communication mode: Server (passive reception)
 * <ul>
 *   <li>RTMLIS (C#) actively connects to Java TCP Server and pushes DML messages</li>
 *   <li>Java passively receives, persists data, and sends ACK back</li>
 * </ul>
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Parse DML OBS.R01/SVC.R01 messages from RTMLIS</li>
 *   <li>Store sample, observation, and QC data to database</li>
 *   <li>Forwarding to LIS is TBD (configurable via {@code bioconnect.lis.enabled})</li>
 * </ul>
 */
@Slf4j
@Service
public class DmlObservationService {

    private final DmlMessageParser dmlParser;
    private final DmlMessageBuilder dmlBuilder;
    private final SampleRepository sampleRepository;
    private final ObservationResultRepository observationRepository;
    private final QcResultRepository qcResultRepository;
    private final BioConnectProperties properties;

    public DmlObservationService(DmlMessageParser dmlParser,
                                  DmlMessageBuilder dmlBuilder,
                                  SampleRepository sampleRepository,
                                  ObservationResultRepository observationRepository,
                                  QcResultRepository qcResultRepository,
                                  BioConnectProperties properties) {
        this.dmlParser = dmlParser;
        this.dmlBuilder = dmlBuilder;
        this.sampleRepository = sampleRepository;
        this.observationRepository = observationRepository;
        this.qcResultRepository = qcResultRepository;
        this.properties = properties;
    }

    /**
     * Process inbound DML message from RTMLIS.
     * Persists data to database. LIS forwarding is optional (TBD).
     *
     * @param xmlMessage raw DML XML string
     * @return parsed DML message
     */
    @Transactional
    public DmlMessage processDmlMessage(String xmlMessage) {
        DmlMessage message = dmlParser.parse(xmlMessage);
        String type = message.type();
        String messageId = message.messageId();

        log.info("DML message received: type={}, messageId={}, obsCount={}, svcCount={}",
                type, messageId,
                message.obsDataList() != null ? message.obsDataList().size() : 0,
                message.svcDataList() != null ? message.svcDataList().size() : 0);

        if (DmlConstants.MSG_OBS.equals(type) || DmlConstants.MSG_SVC.equals(type)) {
            String sampleKeyNum = persistSampleData(message, xmlMessage);
            log.info("Sample data persisted: sampleKeyNum={}, type={}", sampleKeyNum, type);

            // Forwarding to LIS is TBD - currently disabled
            // When bioconnect.lis.enabled=true, forwarding logic will be added here
            if (properties.getLis().isEnabled()) {
                log.debug("LIS forwarding enabled but not yet implemented");
            }
        }

        return message;
    }

    /**
     * Persist sample, observation, and QC data to database.
     *
     * @param message parsed DML message
     * @param xmlText raw XML text
     * @return generated sample key number
     */
    @Transactional
    public String persistSampleData(DmlMessage message, String xmlText) {
        String sampleKeyNum = UUID.randomUUID().toString();

        SampleEntity sample = new SampleEntity();
        sample.setSampleKeyNum(sampleKeyNum);
        sample.setXmlText(xmlText);
        sample.setSampleDate(LocalDateTime.now());
        sample.setTransmittedFlag("F");
        sample.setControlType(message.type());
        sample.setIsQc(DmlConstants.MSG_SVC.equals(message.type()));

        if (!message.obsDataList().isEmpty()) {
            DmlMessageParser.ObsData firstObs = message.obsDataList().get(0);
            sample.setDeviceName(firstObs.model());
            sample.setDeviceType(firstObs.deviceType());
            sample.setDeviceSerial(firstObs.serialNumber());
            sample.setDeviceSwVer(firstObs.swVersion());
            sample.setLocName(firstObs.facility());
            sample.setFacName(firstObs.facility());
            sample.setPatientNum(firstObs.id());
        }

        sampleRepository.save(sample);
        log.info("Sample saved: sampleKeyNum={}, type={}, obsCount={}",
                sampleKeyNum, message.type(), message.obsDataList().size());

        for (DmlMessageParser.ObsData obsData : message.obsDataList()) {
            for (DmlMessageParser.ResultItem result : obsData.results()) {
                ObservationResultEntity obsEnt = new ObservationResultEntity();
                obsEnt.setSampleKeyNum(sampleKeyNum);
                obsEnt.setTestCode(result.code());
                obsEnt.setTestName(result.name());
                obsEnt.setResultValue(result.value());
                obsEnt.setResultUnits(result.units());
                obsEnt.setControlType(message.type());
                obsEnt.setXmlText(xmlText);
                observationRepository.save(obsEnt);
            }
        }

        for (DmlMessageParser.SvcData svcData : message.svcDataList()) {
            QcResultEntity qcEnt = new QcResultEntity();
            qcEnt.setSampleKeyNum(sampleKeyNum);
            qcEnt.setControlType(svcData.type());
            qcEnt.setLotNumber(svcData.code());
            qcEnt.setTestCode(svcData.code());
            qcEnt.setResultValue(svcData.value());
            qcEnt.setResultUnits(svcData.units());
            qcResultRepository.save(qcEnt);
        }

        return sampleKeyNum;
    }

    public List<SampleEntity> findPendingTransmission() {
        return sampleRepository.findByTransmittedFlag("F");
    }

    public List<SampleEntity> findByDeviceSerial(String deviceSerial) {
        return sampleRepository.findByDeviceSerial(deviceSerial);
    }

    public void markAsTransmitted(String sampleKeyNum) {
        sampleRepository.findBySampleKeyNum(sampleKeyNum).ifPresent(s -> {
            s.setTransmittedFlag("T");
            sampleRepository.save(s);
        });
    }

    public String buildObservationAck(String sessionId, String messageId) {
        return dmlBuilder.buildObservationAck(sessionId, messageId);
    }
}
