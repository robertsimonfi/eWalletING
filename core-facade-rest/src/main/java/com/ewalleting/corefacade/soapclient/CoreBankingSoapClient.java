package com.ewalleting.corefacade.soapclient;

import com.ewalleting.corefacade.soapclient.generated.*;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.soap.client.SoapFaultClientException;

/**
 * The one place in this service that speaks SOAP. Everything above this class
 * (controllers, DTOs) only knows JSON — this is the anti-corruption boundary.
 *
 * Each method is wrapped in a circuit breaker: after enough SOAP calls fail
 * (connection refused, timeout — anything that isn't a legitimate business
 * fault), the breaker trips open and short-circuits straight to the fallback
 * instead of hammering a downed legacy service. See application.yml for the
 * breaker thresholds and which exceptions don't count as failures.
 */
@Component
public class CoreBankingSoapClient {

    private static final String BREAKER = "legacyCoreSoap";

    private final WebServiceTemplate webServiceTemplate;

    public CoreBankingSoapClient(WebServiceTemplate webServiceTemplate) {
        this.webServiceTemplate = webServiceTemplate;
    }

    @CircuitBreaker(name = BREAKER, fallbackMethod = "accountBalanceFallback")
    public GetAccountBalanceResponse getAccountBalance(String accountId) {
        GetAccountBalanceRequest request = new GetAccountBalanceRequest();
        request.setAccountId(accountId);
        return (GetAccountBalanceResponse) webServiceTemplate.marshalSendAndReceive(request);
    }

    @CircuitBreaker(name = BREAKER, fallbackMethod = "customerRecordFallback")
    public GetCustomerRecordResponse getCustomerRecord(String customerId) {
        GetCustomerRecordRequest request = new GetCustomerRecordRequest();
        request.setCustomerId(customerId);
        return (GetCustomerRecordResponse) webServiceTemplate.marshalSendAndReceive(request);
    }

    // Fallback signature must mirror the guarded method plus a trailing Throwable.
    // Resilience4j's annotation-driven fallback runs for *every* exception out of the
    // guarded method, not just the ones that count toward the breaker's failure rate —
    // `ignore-exceptions` in application.yml only controls the failure-rate math, it
    // doesn't stop the fallback from being invoked. So a legitimate "not found" SOAP
    // fault still lands here and has to be told apart from a real technical failure
    // (breaker open, connection refused, timeout) and rethrown as-is rather than
    // reported as "upstream unavailable".
    private GetAccountBalanceResponse accountBalanceFallback(String accountId, Throwable cause) {
        if (cause instanceof SoapFaultClientException soapFault) {
            throw soapFault;
        }
        throw new UpstreamUnavailableException("legacy-core-soap", cause);
    }

    private GetCustomerRecordResponse customerRecordFallback(String customerId, Throwable cause) {
        if (cause instanceof SoapFaultClientException soapFault) {
            throw soapFault;
        }
        throw new UpstreamUnavailableException("legacy-core-soap", cause);
    }
}
