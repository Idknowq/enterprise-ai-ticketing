package com.enterprise.ticketing.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI enterpriseAiTicketingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Enterprise AI Ticketing MVP API")
                        .version("v0.1.0")
                        .description("Enterprise internal IT service desk orchestration backend foundation.")
                        .contact(new Contact().name("Platform Foundation Thread")));
    }
}
