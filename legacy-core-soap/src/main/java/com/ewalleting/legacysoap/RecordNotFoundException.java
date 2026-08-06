package com.ewalleting.legacysoap;

import org.springframework.ws.soap.server.endpoint.annotation.FaultCode;
import org.springframework.ws.soap.server.endpoint.annotation.SoapFault;

/**
 * Thrown when a requested account/customer id doesn't exist. {@code @SoapFault} tells
 * Spring-WS to turn this into a SOAP 1.1 Client fault instead of a 500 — this is the
 * SOAP-native equivalent of a REST 404, and part of what a REST facade has to translate.
 */
@SoapFault(faultCode = FaultCode.CLIENT, faultStringOrReason = "Record not found")
public class RecordNotFoundException extends RuntimeException {

    public RecordNotFoundException(String requestedId) {
        super("No record found for id: " + requestedId);
    }
}
