package com.nova.bioconnect.rtm.dml;

import java.util.List;

public record DmlMessage(
    String type,
    String version,
    String messageId,
    String sessionId,
    String ackCode,
    List<DmlMessageParser.SvcData> svcDataList,
    List<DmlMessageParser.ObsData> obsDataList
) {
    public boolean isPositiveAck() {
        return DmlConstants.ACK_POSITIVE.equals(ackCode);
    }

    public boolean isNegativeAck() {
        return DmlConstants.ACK_NEGATIVE.equals(ackCode);
    }

    public boolean isObservation() {
        return DmlConstants.MSG_OBS.equals(type) || DmlConstants.MSG_SVC.equals(type);
    }
}