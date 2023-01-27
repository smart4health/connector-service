package com.healthmetrix.connector.inbox.api.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.security.SecurityScheme
import org.hl7.fhir.r4.model.DomainResource
import org.springdoc.core.GroupedOpenApi
import org.springdoc.core.SpringDocUtils
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Profile("swagger")
@Configuration
class SwaggerConfig {

    private val basicAuthScheme = SecurityScheme()
        .type(SecurityScheme.Type.HTTP)
        .scheme("basic")
        .description("Basic auth credentials")

    @Bean
    fun openApi(): OpenAPI {
        // DO NOT generate docs for the whole of HAPI, it takes a long time
        SpringDocUtils.getConfig().replaceWithSchema(DomainResource::class.java, Schema<Void>())

        return OpenAPI()
            .info(
                Info()
                    .title("Connector Documentation")
                    .description("The Smart4Health Connector is an application that will connect a patient's hospital data to an eHR of his or her choice.")
                    .contact(
                        Contact()
                            .name("Healthmetrix GmbH")
                            .email("admin@healthmetrix.com"),
                    ),
            )
            .components(
                Components().addSecuritySchemes(
                    "BasicAuth",
                    basicAuthScheme,
                ),
            )
    }

    @Bean
    fun v1(): GroupedOpenApi = GroupedOpenApi.builder()
        .group("Version 1")
        .pathsToMatch("/v1/**")
        .build()

    @Bean
    fun v2(): GroupedOpenApi = GroupedOpenApi.builder()
        .group("Version 2")
        .pathsToMatch("/v2/**")
        .build()

    @Bean
    fun v3(): GroupedOpenApi = GroupedOpenApi.builder()
        .group("Version 3")
        .pathsToMatch("/v3/**")
        .build()
}
