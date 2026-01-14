package com.aicc.silverlink;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class SilverLinkApplication {

    public static void main(String[] args) {
        SpringApplication.run(SilverLinkApplication.class, args);
    }

}
