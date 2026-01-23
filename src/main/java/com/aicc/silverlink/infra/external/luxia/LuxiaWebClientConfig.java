package com.aicc.silverlink.infra.external.luxia;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;


@Configuration
public class LuxiaWebClientConfig {

    @Bean
    public WebClient luxiaWebClient(LuxiaProperties props) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, props.timeout().connectMs())
                .responseTimeout(Duration.ofMillis(props.timeout().responseMs()))
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(props.timeout().responseMs(), TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(props.timeout().responseMs(), TimeUnit.MILLISECONDS))
                );

        return WebClient.builder()
                .baseUrl(props.baseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                // LUXIA 문서 기준 인증 헤더는 apikey :contentReference[oaicite:3]{index=3}
                .defaultHeader("apikey", props.apiKey())
                .build();
    }

}
