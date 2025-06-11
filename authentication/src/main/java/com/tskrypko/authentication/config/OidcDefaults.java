package com.tskrypko.authentication.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.oidc.defaults")
@Getter
@Setter
public class OidcDefaults {
    private String defaultName = "User";
}
