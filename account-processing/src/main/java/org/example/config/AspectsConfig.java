package org.example.config;

import org.example.aspects.starter.config.AspectsProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AspectsProperties.class)
public class AspectsConfig {
}