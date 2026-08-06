package com.nova.bioconnect.rtm.dml;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class DmlMessageParser {
    private static final Logger log = LoggerFactory.getLogger(DmlMessageParser.class);

    public DmlMessage parse(String xml) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
            factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

            Element root = doc.getDocumentElement();
            String type = root.getAttribute(DmlConstants.ATTR_MESSAGE_TYPE);
            String version = root.getAttribute(DmlConstants.ATTR_VERSION);
            String messageId = root.getAttribute(DmlConstants.ATTR_MESSAGE_ID);
            String sessionId = root.getAttribute(DmlConstants.ATTR_SESSION_ID);

            String ackCode = "";
            NodeList headerNodes = root.getElementsByTagName(DmlConstants.ELEMENT_HEADER);
            if (headerNodes.getLength() > 0) {
                Element header = (Element) headerNodes.item(0);
                ackCode = header.getAttribute(DmlConstants.ATTR_ACK_CODE);
            }

            List<SvcData> svcDataList = new ArrayList<>();
            List<ObsData> obsDataList = new ArrayList<>();
            NodeList svcNodes = root.getElementsByTagName(DmlConstants.ELEMENT_SVC);
            for (int i = 0; i < svcNodes.getLength(); i++) {
                svcDataList.add(parseSvc((Element) svcNodes.item(i)));
            }
            NodeList obsNodes = root.getElementsByTagName(DmlConstants.ELEMENT_OBS);
            for (int i = 0; i < obsNodes.getLength(); i++) {
                obsDataList.add(parseObs((Element) obsNodes.item(i)));
            }

            return new DmlMessage(type, version, messageId, sessionId, ackCode, svcDataList, obsDataList);
        } catch (Exception e) {
            log.error("Failed to parse DML message: {}", e.getMessage(), e);
            return new DmlMessage("", "", "", "", "", List.of(), List.of());
        }
    }

    private SvcData parseSvc(Element svc) {
        return new SvcData(
            svc.getAttribute(DmlConstants.ATTR_TYPE),
            svc.getAttribute(DmlConstants.ATTR_CODE),
            svc.getAttribute(DmlConstants.ATTR_VALUE),
            svc.getAttribute(DmlConstants.ATTR_UNITS),
            svc.getAttribute(DmlConstants.ATTR_DATE),
            svc.getAttribute(DmlConstants.ATTR_TIME)
        );
    }

    private ObsData parseObs(Element obs) {
        List<ResultItem> results = new ArrayList<>();
        NodeList children = obs.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && child.getNodeName().equals("Result")) {
                Element result = (Element) child;
                results.add(new ResultItem(
                    result.getAttribute(DmlConstants.ATTR_CODE),
                    result.getAttribute(DmlConstants.ATTR_NAME),
                    result.getAttribute(DmlConstants.ATTR_VALUE),
                    result.getAttribute(DmlConstants.ATTR_UNITS)
                ));
            }
        }
        return new ObsData(
            obs.getAttribute(DmlConstants.ATTR_TYPE),
            obs.getAttribute(DmlConstants.ATTR_ID),
            obs.getAttribute(DmlConstants.ATTR_SERIAL_NUMBER),
            obs.getAttribute(DmlConstants.ATTR_DEVICE_TYPE),
            obs.getAttribute(DmlConstants.ATTR_MODEL),
            obs.getAttribute(DmlConstants.ATTR_SW_VERSION),
            obs.getAttribute(DmlConstants.ATTR_FACILITY),
            obs.getAttribute(DmlConstants.ATTR_LOCATION),
            obs.getAttribute(DmlConstants.ATTR_DATE),
            obs.getAttribute(DmlConstants.ATTR_TIME),
            obs.getAttribute(DmlConstants.ATTR_OPERATOR_ID),
            obs.getAttribute(DmlConstants.ATTR_OPERATOR_NAME),
            results
        );
    }

    public record SvcData(
        String type,
        String code,
        String value,
        String units,
        String date,
        String time
    ) {}

    public record ObsData(
        String type,
        String id,
        String serialNumber,
        String deviceType,
        String model,
        String swVersion,
        String facility,
        String location,
        String date,
        String time,
        String operatorId,
        String operatorName,
        List<ResultItem> results
    ) {}

    public record ResultItem(
        String code,
        String name,
        String value,
        String units
    ) {}
}