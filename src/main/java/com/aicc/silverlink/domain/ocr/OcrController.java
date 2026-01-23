package com.aicc.silverlink.domain.ocr;

import com.aicc.silverlink.infra.external.luxia.LuxiaDocumentAiClient;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
public class OcrController {

    private final LuxiaDocumentAiClient client;

    @PostMapping(value = "/document-ai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public JsonNode documentAi(@RequestPart("file") @NotNull MultipartFile file) {
        return client.callDocumentAi(file);
    }
}
