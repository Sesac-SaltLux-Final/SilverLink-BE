package com.aicc.silverlink.domain.counselor.controller;

import com.aicc.silverlink.domain.counselor.dto.CounselorRequest;
import com.aicc.silverlink.domain.counselor.dto.CounselorResponse;
import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.counselor.repository.CounselorRepository;
import com.aicc.silverlink.domain.counselor.service.CounselorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/counselors")
public class CounselorController {

    private final CounselorService counselorService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CounselorResponse> register(@RequestBody @Valid CounselorRequest request){

        CounselorResponse response = counselorService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CounselorResponse>getCounselorByAdmin(@PathVariable("id")Long id){
        CounselorResponse response = counselorService.getCounselor(id);

        return ResponseEntity.ok(response);
    }
    @GetMapping("/me")
    @PreAuthorize("hasRole('COUNSELOR')")
    public ResponseEntity<CounselorResponse>getCounselor(@AuthenticationPrincipal Long currentUserId){
        CounselorResponse response = counselorService.getCounselor(currentUserId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<CounselorResponse>>getAllCounselors(){
        List<CounselorResponse> responses = counselorService.getAllCounselors();
        return ResponseEntity.ok(responses);
    }

}
