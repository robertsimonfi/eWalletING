package com.ewalleting.legacysoap;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/**
 * Wires up Spring-WS: the servlet that dispatches SOAP requests to {@code @Endpoint}
 * beans, and the WSDL definition generated from the XSD contract at
 * {@code /ws/core-banking.wsdl}.
 */
@EnableWs
@Configuration
public class WebServiceConfig {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            org.springframework.context.ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "core-banking")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema coreBankingSchema) {
        DefaultWsdl11Definition wsdl11Definition = new DefaultWsdl11Definition();
        wsdl11Definition.setPortTypeName("CoreBankingPort");
        wsdl11Definition.setLocationUri("/ws");
        wsdl11Definition.setTargetNamespace("http://ewalleting.com/legacycore");
        wsdl11Definition.setSchema(coreBankingSchema);
        return wsdl11Definition;
    }

    @Bean
    public XsdSchema coreBankingSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/core-banking.xsd"));
    }
}
