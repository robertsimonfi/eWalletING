package com.ewalleting.apigateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.web.servlet.function.RequestPredicates.path;

/**
 * Every route this gateway knows about. Right now there's one downstream service
 * (core-facade-rest); identity-provider and the wallet services join this list in
 * later modules — this is the single place a new backend gets wired in.
 */
@Configuration
public class GatewayRoutesConfig {

    @Bean
    public RouterFunction<ServerResponse> coreFacadeRoute(
            @Value("${downstream.core-facade-rest.base-url}") String coreFacadeBaseUrl,
            ServiceTokenProvider serviceTokenProvider) {
        return route("core_facade_rest")
                .route(path("/api/accounts/**").or(path("/api/customers/**")), http())
                .before(uri(coreFacadeBaseUrl))
                .before(request -> ServerRequest.from(request)
                        .header("Authorization", "Bearer " + serviceTokenProvider.getAccessToken())
                        .build())
                .build();
    }
}
