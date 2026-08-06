package com.ewalleting.corefacade.soapclient;

/** Thrown when the circuit breaker is open (or the underlying SOAP call failed technically). */
public class UpstreamUnavailableException extends RuntimeException {

    public UpstreamUnavailableException(String upstreamName, Throwable cause) {
        super("Upstream '" + upstreamName + "' is currently unavailable", cause);
    }
}
