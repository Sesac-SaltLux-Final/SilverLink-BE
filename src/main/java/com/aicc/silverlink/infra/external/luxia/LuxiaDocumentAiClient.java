package com.aicc.silverlink.infra.external.luxia;

import com.aicc.silverlink.global.exception.LuxiaHttpException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;

import java.util.Base64;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LuxiaDocumentAiClient {

    private final org.springframework.web.reactive.function.client.WebClient webClient;
    private final LuxiaProperties props;

    public Map<String, Object> callDocumentAi(MultipartFile file) {
        validateImage(file);

        String contentType = (file.getContentType() != null) ? file.getContentType() : "image/png";

        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (Exception e) {
            throw new RuntimeException("failed to read file bytes", e);
        }

        String b64 = Base64.getEncoder().encodeToString(bytes);
        String dataUrl = "data:" + contentType + ";base64," + b64;

        // ✅ curl 성공 케이스와 동일하게 "image"만 보냄
        Map<String, String> req = Map.of("image", dataUrl);

        Map<String, Object> res = webClient.post()
                .uri(props.documentAi().path()) // /luxia/v1/document-ai
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .retrieve()
                .onStatus(
                        s -> s.is4xxClientError() || s.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .flatMap(body -> Mono.error(new LuxiaHttpException(resp.statusCode().value(), body))))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .block();

        if (res == null)
            throw new LuxiaHttpException(502, "Empty response from LUXIA");
        return res;
    }

    private void validateImage(MultipartFile file) {
        if (file == null || file.isEmpty())
            throw new IllegalArgumentException("file is empty");
        String ct = file.getContentType();
        if (ct == null || !ct.startsWith("image/")) {
            throw new IllegalArgumentException("file is not image. contentType=" + ct);
        }
    }
}
