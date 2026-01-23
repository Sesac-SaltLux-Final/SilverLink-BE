package com.aicc.silverlink.domain.file.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "파일", description = "파일 업로드/다운로드 API")
@RestController
@RequestMapping("/api/files")
public class FileController {
}
