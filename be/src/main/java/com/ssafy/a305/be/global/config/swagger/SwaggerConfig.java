package com.ssafy.a305.be.global.config.swagger;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Billage API")
                        .description("Billage backend API documentation")
                        .version("v1")
                        .contact(new Contact().name("A305 Team"))
                        .license(new License().name("Internal Use Only")));
    }
}
