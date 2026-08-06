package com.ewalleting.corefacade;

import com.ewalleting.corefacade.soapclient.UpstreamUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.ws.soap.client.SoapFaultClientException;

import java.time.Instant;
import java.util.Map;

/**
 * Where SOAP-shaped failures get translated into REST-shaped ones — the other half
 * of the anti-corruption layer. A REST client should never see a SOAP fault or a
 * circuit-breaker internal exception, only ordinary HTTP status codes.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    // The legacy service's recordNotFoundFault surfaces here as a SOAP client fault.
    @ExceptionHandler(SoapFaultClientException.class)
    public ResponseEntity<Object> handleNotFound(SoapFaultClientException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(errorBody(ex.getMessage()));
    }

    // Circuit open, or the fallback method ran because the SOAP call failed technically.
    @ExceptionHandler({UpstreamUnavailableException.class, CallNotPermittedException.class})
    public ResponseEntity<Object> handleUpstreamDown(RuntimeException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorBody(ex.getMessage()));
    }

    private Map<String, Object> errorBody(String message) {
        return Map.of("error", message, "timestamp", Instant.now().toString());
    }
}
