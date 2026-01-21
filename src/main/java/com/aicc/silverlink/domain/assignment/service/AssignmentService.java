package com.aicc.silverlink.domain.assignment.service;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.admin.repository.AdminRepository;
import com.aicc.silverlink.domain.assignment.dto.AssignmentRequest;
import com.aicc.silverlink.domain.assignment.dto.AssignmentResponse;
import com.aicc.silverlink.domain.assignment.entity.Assignment;
import com.aicc.silverlink.domain.assignment.entity.AssignmentStatus;
import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.counselor.dto.CounselorResponse;
import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.counselor.repository.CounselorRepository;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final CounselorRepository counselorRepository;
    private final ElderlyRepository elderlyRepository;
    private final AdminRepository adminRepository;

    @Transactional
    public AssignmentResponse assignCounselor(AssignmentRequest request){
        Counselor counselor = counselorRepository.findById(request.getCounselorId())
                .orElseThrow(()->new IllegalArgumentException("존재하지 않은 상담사입니다."));
        Elderly elderly = elderlyRepository.findById(request.getElderlyId())
                .orElseThrow(()->new IllegalArgumentException("존재하지 않은 어르신입니다."));
        Admin admin = adminRepository.findById(request.getAdminId())
                .orElseThrow(()->new IllegalArgumentException("존재하지 않는 관리자입니다."));

        if(assignmentRepository.existsByElderly_IdAndStatus(elderly.getId(), AssignmentStatus.ACTIVE)){
            throw new IllegalArgumentException("해당 어르신은 이미 담당 상담사가 배정되었습니다.");
        }
        Assignment assignment = Assignment.create(counselor,elderly,admin);
        Assignment savedAssignment = assignmentRepository.save(assignment);

        log.info("배정 완료 : 상답사({}) -> 어르신({}) by 관리자({})",
                counselor.getId(),elderly.getId(),admin.getUserId());

        return AssignmentResponse.from(savedAssignment);
    }
    @Transactional
    public void unassignCounselor(Long counselorId, Long elderlyId){
        Assignment assignment = assignmentRepository.findByCounselorAndElderlyAndStatus(
                counselorId,elderlyId,AssignmentStatus.ACTIVE
        ).orElseThrow(()-> new IllegalArgumentException("현재 활성화된 배정 정보가 없습니다."));

        assignment.endAssignment();
        log.info("배정 해제: 상담사({}) - 어르신({})", counselorId, elderlyId);
    }

    public List<AssignmentResponse> getAssignmentsByCounselor(Long counselorId){
        return assignmentRepository.findAllActiveByCounselorId(counselorId).stream()
                .map(AssignmentResponse::from)
                .collect(Collectors.toList());
    }

    public AssignmentResponse getAssignmentByElderly(Long elderlyId){
        Assignment assignment = assignmentRepository.findActiveByElderlyId(elderlyId)
                .orElseThrow(()->new IllegalArgumentException("현재 담당 상담사가 없습니다."));
        return AssignmentResponse.from(assignment);
    }

}
