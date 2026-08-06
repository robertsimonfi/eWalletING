package com.ewalleting.corefacade.account;

import java.math.BigDecimal;

/** Clean JSON shape exposed to REST clients — no trace of the SOAP contract underneath. */
public record AccountBalanceDto(String accountId, BigDecimal balance, String currency) {
}
