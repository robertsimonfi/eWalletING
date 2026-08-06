package com.ewalleting.legacysoap;

import com.ewalleting.legacysoap.generated.*;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * Stands in for a real core-banking mainframe/CICS transaction. Two operations,
 * fixed in-memory "records" — deliberately old-fashioned: no pagination, no partial
 * responses, whole-record-or-fault, exactly the shape a REST facade has to smooth over.
 */
@Endpoint
public class CoreBankingEndpoint {

    private static final String NAMESPACE_URI = "http://ewalleting.com/legacycore";

    private static final Map<String, BalanceRecord> ACCOUNTS = Map.of(
            "ACC-1001", new BalanceRecord(new BigDecimal("2450.75"), "EUR"),
            "ACC-1002", new BalanceRecord(new BigDecimal("187.20"), "EUR")
    );

    private static final Map<String, CustomerRecord> CUSTOMERS = Map.of(
            "CUST-500", new CustomerRecord("Robert Simonfi", LocalDate.of(1990, 4, 12), true),
            "CUST-501", new CustomerRecord("Jane Doe", LocalDate.of(1985, 11, 3), false)
    );

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getAccountBalanceRequest")
    @ResponsePayload
    public GetAccountBalanceResponse getAccountBalance(@RequestPayload GetAccountBalanceRequest request) {
        BalanceRecord record = ACCOUNTS.get(request.getAccountId());
        if (record == null) {
            throw new RecordNotFoundException(request.getAccountId());
        }
        GetAccountBalanceResponse response = new GetAccountBalanceResponse();
        response.setAccountId(request.getAccountId());
        response.setBalance(record.balance());
        response.setCurrency(record.currency());
        return response;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "getCustomerRecordRequest")
    @ResponsePayload
    public GetCustomerRecordResponse getCustomerRecord(@RequestPayload GetCustomerRecordRequest request) {
        CustomerRecord record = CUSTOMERS.get(request.getCustomerId());
        if (record == null) {
            throw new RecordNotFoundException(request.getCustomerId());
        }
        GetCustomerRecordResponse response = new GetCustomerRecordResponse();
        response.setCustomerId(request.getCustomerId());
        response.setName(record.name());
        response.setDateOfBirth(toXmlDate(record.dateOfBirth()));
        response.setKycVerified(record.kycVerified());
        return response;
    }

    private javax.xml.datatype.XMLGregorianCalendar toXmlDate(LocalDate date) {
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendarDate(
                    date.getYear(), date.getMonthValue(), date.getDayOfMonth(),
                    javax.xml.datatype.DatatypeConstants.FIELD_UNDEFINED);
        } catch (DatatypeConfigurationException e) {
            throw new IllegalStateException("XML datatype factory unavailable", e);
        }
    }

    private record BalanceRecord(BigDecimal balance, String currency) {}

    private record CustomerRecord(String name, LocalDate dateOfBirth, boolean kycVerified) {}
}
