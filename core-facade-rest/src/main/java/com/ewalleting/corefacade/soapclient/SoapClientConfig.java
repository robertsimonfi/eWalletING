package com.ewalleting.corefacade.soapclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * Wires the client side of the anti-corruption layer: a {@link WebServiceTemplate}
 * that marshals/unmarshals against the same XSD contract legacy-core-soap publishes,
 * pointed at the SOAP service's endpoint URL.
 */
@Configuration
public class SoapClientConfig {

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setContextPath("com.ewalleting.corefacade.soapclient.generated");
        return marshaller;
    }

    @Bean
    public WebServiceTemplate webServiceTemplate(
            Jaxb2Marshaller marshaller,
            @Value("${legacy-core-soap.endpoint}") String endpoint) {
        WebServiceTemplate template = new WebServiceTemplate();
        template.setMarshaller(marshaller);
        template.setUnmarshaller(marshaller);
        template.setDefaultUri(endpoint);
        return template;
    }
}
