package org.avni_integration_service.avni.domain;

public enum MessageDeliveryStatus {
    NotSent,
    NotSentNoPhoneNumberInAvni,
    PartiallySent,
    Sent,
    Failed
}
