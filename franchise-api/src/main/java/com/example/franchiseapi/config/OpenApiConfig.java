package com.example.franchiseapi.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI franchiseOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Franchise Management API")
                        .version("1.0")
                        .description("""
                                REST API to manage a list of franchises, their branches and the \
                                products held by each branch.

                                **Domain**: `Franchise (1..N) Branch (1..N) Product`.

                                **Error format** — every failing request returns the same envelope:
                                `timestamp`, `status`, `error`, `message`, `path`, and `validationErrors` \
                                for field-level validation failures.

                                **Status codes**: `400` invalid payload or broken business rule, \
                                `404` unknown franchise / branch / product, `409` duplicated name, \
                                `500` unexpected failure.""")
                        .contact(new Contact().name("Backend Team").email("backend@example.com"))
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("/").description("Current host")
                ));
    }
}
