package com.aicc.silverlink.global.config.auth;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AuthPhoneProperties.class, WebAuthnProperties.class})
public class AuthPropertiesConfig {

}
