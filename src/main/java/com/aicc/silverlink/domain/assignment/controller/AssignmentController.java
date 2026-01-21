package com.aicc.silverlink.domain.assignment.controller;

import com.aicc.silverlink.domain.assignment.dto.AssignmentRequest;
import com.aicc.silverlink.domain.assignment.dto.AssignmentResponse;
import com.aicc.silverlink.domain.assignment.service.AssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AssignmentResponse> assignCounselor(@RequestBody AssignmentRequest request){
        AssignmentResponse response = assignmentService.assignCounselor(request);
        return ResponseEntity.created(URI.create("/api/assignments/elderly/" + response.getElderlyId())).body(response);
    }

    @PostMapping("/unassign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unassignCounselor(
            @RequestParam Long counselorId,
            @RequestParam Long elderlyId
    ){
        assignmentService.unassignCounselor(counselorId, elderlyId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/counselor/{counselorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR')")
    public ResponseEntity<List<AssignmentResponse>> getAssignmentByCounselor(@PathVariable Long counselorId){
        return ResponseEntity.ok(assignmentService.getAssignmentsByCounselor(counselorId));
    }

    @GetMapping("/elderly/{elderlyId}")
    public ResponseEntity<AssignmentResponse>getAssignmentByElderly(@PathVariable Long elderlyId){
    return ResponseEntity.ok(assignmentService.getAssignmentByElderly(elderlyId));
    }
}
