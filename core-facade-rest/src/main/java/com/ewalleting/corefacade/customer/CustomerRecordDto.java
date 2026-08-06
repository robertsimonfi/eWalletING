package com.ewalleting.corefacade.customer;

public record CustomerRecordDto(String customerId, String name, String dateOfBirth, boolean kycVerified) {
}
