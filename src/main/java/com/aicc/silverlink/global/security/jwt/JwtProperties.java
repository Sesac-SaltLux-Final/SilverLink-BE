package com.aicc.silverlink.global.security.jwt;

import io.jsonwebtoken.Jwt;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private Jwt jwt = new Jwt();

    @Getter
    @Setter
    public static class Jwt {
        private String issuer;
        private String secret;
        private Long accessExpMinutes;
        private Long refreshExpDays;
    }

}
