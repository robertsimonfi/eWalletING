package com.ewalleting.corefacade.customer;

import com.ewalleting.corefacade.soapclient.CoreBankingSoapClient;
import com.ewalleting.corefacade.soapclient.generated.GetCustomerRecordResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CustomerController {

    private final CoreBankingSoapClient soapClient;

    public CustomerController(CoreBankingSoapClient soapClient) {
        this.soapClient = soapClient;
    }

    @GetMapping("/api/customers/{customerId}")
    public CustomerRecordDto getCustomer(@PathVariable String customerId) {
        GetCustomerRecordResponse soapResponse = soapClient.getCustomerRecord(customerId);
        return new CustomerRecordDto(
                soapResponse.getCustomerId(),
                soapResponse.getName(),
                soapResponse.getDateOfBirth().toString(),
                soapResponse.isKycVerified());
    }
}
