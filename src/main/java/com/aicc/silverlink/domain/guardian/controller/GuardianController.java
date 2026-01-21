package com.aicc.silverlink.domain.guardian.controller;

import com.aicc.silverlink.domain.elderly.service.ElderlyService;
import com.aicc.silverlink.domain.guardian.dto.GuardianElderlyResponse;
import com.aicc.silverlink.domain.guardian.dto.GuardianRequest;
import com.aicc.silverlink.domain.guardian.dto.GuardianResponse;
import com.aicc.silverlink.domain.guardian.entity.RelationType;
import com.aicc.silverlink.domain.guardian.service.GuardianService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/guardians")
@RequiredArgsConstructor
public class GuardianController {

    private final GuardianService guardianService;

    @PostMapping("/signup")
    public ResponseEntity<GuardianResponse> signup(@RequestBody GuardianRequest request){
        GuardianResponse response = guardianService.register(request);
        return ResponseEntity.created((URI.create("/api/guardians/" + response.getId())))
                .body(response);
    }
    @GetMapping("/{id}")
    public ResponseEntity<GuardianResponse> getGuardian(@PathVariable Long id){
        return ResponseEntity.ok(guardianService.getGuardian(id));
    }
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GuardianResponse>> getAllGuardians(){
        return ResponseEntity.ok(guardianService.getAllGuardian());
    }
    @PostMapping("/{id}/connect")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> connectElderly(
            @PathVariable("id")Long guardianId,
            @RequestParam Long elderlyId,
            @RequestParam RelationType relationType
            ){
        guardianService.connectElderly(guardianId,elderlyId,relationType);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/{id}/elderly")
    public ResponseEntity<GuardianElderlyResponse> getMyElderly(@PathVariable("id")Long guardianId){
        GuardianElderlyResponse response = guardianService.getElderlyByGuardian(guardianId);
        return ResponseEntity.ok(response);
    }
    @GetMapping("/find-by-elderly/{id}")
    public ResponseEntity<GuardianElderlyResponse> getGuardianOfElderly(@PathVariable("id")Long elderlyId){
        GuardianElderlyResponse response = guardianService.getGuardianByElderly(elderlyId);
        return ResponseEntity.ok(response);
    }
}
