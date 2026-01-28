package com.aicc.silverlink.domain.elderly.service;

import com.aicc.silverlink.domain.elderly.dto.CallScheduleDto.*;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.entity.ElderlyHealthInfo;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.elderly.repository.HealthInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 통화 스케줄 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CallScheduleService {

    private final ElderlyRepository elderlyRepository;
    private final HealthInfoRepository healthInfoRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // ===== 스케줄 조회/수정 =====

    /**
     * 어르신 통화 스케줄 조회
     */
    public Response getSchedule(Long elderlyId) {
        Elderly elderly = elderlyRepository.findWithUserById(elderlyId)
                .orElseThrow(() -> new IllegalArgumentException("어르신을 찾을 수 없습니다."));
        return Response.from(elderly);
    }

    /**
     * 어르신 통화 스케줄 설정/수정
     */
    @Transactional
    public Response updateSchedule(Long elderlyId, UpdateRequest request) {
        Elderly elderly = elderlyRepository.findWithUserById(elderlyId)
                .orElseThrow(() -> new IllegalArgumentException("어르신을 찾을 수 없습니다."));

        elderly.updateCallSchedule(
                request.getPreferredCallTime(),
                request.getDaysAsString(),
                request.getCallScheduleEnabled());

        elderlyRepository.save(elderly);
        log.info("[CallSchedule] 스케줄 업데이트: elderlyId={}, time={}, days={}, enabled={}",
                elderlyId, request.getPreferredCallTime(), request.getDaysAsString(), request.getCallScheduleEnabled());

        return Response.from(elderly);
    }

    // ===== CallBot용 =====

    /**
     * 현재 시간에 전화해야 할 어르신 목록
     * 
     * @return 전화 발신 요청 목록
     */
    public List<StartCallRequest> getDueForCall() {
        String currentTime = LocalTime.now().format(TIME_FORMATTER);
        String dayCode = LocalDate.now().getDayOfWeek().name().substring(0, 3); // MON, TUE, ...

        log.debug("[CallSchedule] 스케줄 체크: time={}, day={}", currentTime, dayCode);

        List<Elderly> dueList = elderlyRepository.findDueForCall(currentTime, dayCode);

        return dueList.stream()
                .map(this::toStartCallRequest)
                .collect(Collectors.toList());
    }

    /**
     * 전체 활성화된 스케줄 목록 조회 (관리자용)
     */
    public List<Response> getAllSchedules() {
        return elderlyRepository.findAllWithCallScheduleEnabled().stream()
                .map(Response::from)
                .collect(Collectors.toList());
    }

    // ===== Private Methods =====

    private StartCallRequest toStartCallRequest(Elderly elderly) {
        List<String> chronicDiseases = getChronicDiseases(elderly.getId());

        return StartCallRequest.builder()
                .elderlyId(elderly.getId())
                .elderlyName(elderly.getUser().getName())
                .phone(elderly.getUser().getPhone())
                .chronicDiseases(chronicDiseases)
                .build();
    }

    private List<String> getChronicDiseases(Long elderlyId) {
        Optional<ElderlyHealthInfo> healthInfoOpt = healthInfoRepository.findById(elderlyId);
        if (healthInfoOpt.isEmpty()) {
            return List.of();
        }

        String chronicDiseases = healthInfoOpt.get().getChronicDiseases();
        return StartCallRequest.parseChronicDiseases(chronicDiseases);
    }
}
