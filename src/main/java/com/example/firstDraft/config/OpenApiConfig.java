package com.example.firstDraft.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info = @Info(
        title = "Transaction Monitoring Rules API",
        version = "v1",
        description = "Rules Management APIs for monitoring rule configuration, history, and statistics.",
        contact = @Contact(name = "Rules Management Team")
    ),
    servers = @Server(url = "/", description = "Local server")
)
public class OpenApiConfig {
}

