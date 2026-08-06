package com.ewalleting.corefacade.account;

import com.ewalleting.corefacade.soapclient.CoreBankingSoapClient;
import com.ewalleting.corefacade.soapclient.generated.GetAccountBalanceResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AccountController {

    private final CoreBankingSoapClient soapClient;

    public AccountController(CoreBankingSoapClient soapClient) {
        this.soapClient = soapClient;
    }

    @GetMapping("/api/accounts/{accountId}/balance")
    public AccountBalanceDto getBalance(@PathVariable String accountId) {
        GetAccountBalanceResponse soapResponse = soapClient.getAccountBalance(accountId);
        return new AccountBalanceDto(
                soapResponse.getAccountId(),
                soapResponse.getBalance(),
                soapResponse.getCurrency());
    }
}
