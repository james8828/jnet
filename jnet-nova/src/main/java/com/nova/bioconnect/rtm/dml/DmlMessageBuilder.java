package com.nova.bioconnect.rtm.dml;

import com.nova.bioconnect.rtm.model.PatientInfo;
import com.nova.bioconnect.rtm.model.VisitInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringWriter;
import java.util.UUID;

@Component
public class DmlMessageBuilder {
    private static final Logger log = LoggerFactory.getLogger(DmlMessageBuilder.class);

    public String buildHelloAck(String sessionId, String messageId) {
        return wrapInMessage(DmlConstants.MSG_ACK_HELLO, sessionId, messageId,
            "<Body><Header AckCode=\"AA\"/></Body>");
    }

    public String buildSetupAck(String sessionId, String messageId) {
        return wrapInMessage(DmlConstants.MSG_ACK_SETUP, sessionId, messageId,
            "<Body><Header AckCode=\"AA\"/></Body>");
    }

    public String buildDailySetupAck(String sessionId, String messageId) {
        return wrapInMessage(DmlConstants.MSG_ACK_DAILY_SETUP, sessionId, messageId,
            "<Body><Header AckCode=\"AA\"/></Body>");
    }

    public String buildObservationAck(String sessionId, String messageId) {
        return wrapInMessage(DmlConstants.MSG_ACK_OBS, sessionId, messageId,
            "<Body><Header AckCode=\"AA\"/></Body>");
    }

    public String buildTerminateAck(String sessionId, String messageId) {
        return wrapInMessage(DmlConstants.MSG_ACK_TERMINATE, sessionId, messageId,
            "<Body><Header AckCode=\"AA\"/></Body>");
    }

    public String buildPatientMessage(String sessionId, PatientInfo patient, VisitInfo visit) {
        StringBuilder body = new StringBuilder();
        body.append("<Body>");
        body.append("<PAT ");
        body.append("Type=\"").append(patient == null ? "" : patient.accountNumber()).append("\" ");
        body.append("First=\"").append(escapeXml(patient == null ? "" : patient.firstName())).append("\" ");
        body.append("Last=\"").append(escapeXml(patient == null ? "" : patient.lastName())).append("\" ");
        body.append("Middle=\"").append(escapeXml(patient == null ? "" : patient.middleName())).append("\" ");
        body.append("Sex=\"").append(patient == null ? "" : patient.gender()).append("\" ");
        body.append("Birthdate=\"").append(patient == null || patient.dateOfBirth() == null ? "" : patient.dateOfBirth().toString()).append("\" ");
        body.append("MRN=\"").append(escapeXml(patient == null ? "" : patient.internalPatientId())).append("\" ");
        if (visit != null) {
            body.append("Facility=\"").append(escapeXml(visit.facility() != null ? visit.facility() : "")).append("\" ");
            body.append("Location=\"").append(escapeXml(visit.assignedLocation() != null ? visit.assignedLocation() : "")).append("\" ");
            body.append("Department=\"").append(escapeXml(visit.room() != null ? visit.room() : "")).append("\" ");
        }
        body.append("/>");
        body.append("</Body>");
        return wrapInMessage(DmlConstants.MSG_PAT, sessionId, UUID.randomUUID().toString(), body.toString());
    }

    public String buildOperatorMessage(String sessionId, OperatorData operator) {
        StringBuilder body = new StringBuilder();
        body.append("<Body>");
        body.append("<OPL ");
        body.append("Id=\"").append(escapeXml(operator.operatorId())).append("\" ");
        body.append("OperatorId=\"").append(escapeXml(operator.operatorId())).append("\" ");
        body.append("OperatorName=\"").append(escapeXml(operator.firstName() + " " + operator.lastName())).append("\" ");
        body.append("First=\"").append(escapeXml(operator.firstName())).append("\" ");
        body.append("Last=\"").append(escapeXml(operator.lastName())).append("\" ");
        body.append("Supervisor=\"").append(operator.isSupervisor() ? "T" : "F").append("\" ");
        body.append("Privilege=\"").append(String.valueOf(operator.privilege())).append("\" ");
        body.append("/>");
        body.append("</Body>");
        return wrapInMessage(DmlConstants.MSG_OPL, sessionId, UUID.randomUUID().toString(), body.toString());
    }

    private String wrapInMessage(String type, String sessionId, String messageId, String bodyContent) {
        return "<Message " +
               "Type=\"" + type + "\" " +
               "Version=\"" + DmlConstants.DML_VERSION + "\" " +
               "MessageId=\"" + messageId + "\" " +
               "SessionId=\"" + (sessionId != null ? sessionId : "") + "\">" +
               bodyContent +
               "<Trailer/>" +
               "</Message>";
    }

    private String escapeXml(String value) {
        if (value == null) return "";
        return value.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&apos;");
    }

    public record OperatorData(
        String operatorId,
        String firstName,
        String lastName,
        boolean isSupervisor,
        int privilege,
        String facility,
        String location
    ) {}
}