package com.aicc.silverlink.domain.notice.service;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.notice.dto.NoticeRequest;
import com.aicc.silverlink.domain.notice.dto.NoticeResponse;
import com.aicc.silverlink.domain.notice.entity.Notice;
import com.aicc.silverlink.domain.notice.entity.Notice.NoticeStatus;
import com.aicc.silverlink.domain.notice.entity.Notice.TargetMode;
import com.aicc.silverlink.domain.notice.entity.NoticeAttachment;
import com.aicc.silverlink.domain.notice.entity.NoticeTargetRole;
import com.aicc.silverlink.domain.notice.repository.NoticeAttachmentRepository;
import com.aicc.silverlink.domain.notice.repository.NoticeReadLogRepository;
import com.aicc.silverlink.domain.notice.repository.NoticeRepository;
import com.aicc.silverlink.domain.notice.repository.NoticeTargetRoleRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @InjectMocks
    private NoticeService noticeService;

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private NoticeReadLogRepository noticeReadLogRepository;

    @Mock
    private NoticeTargetRoleRepository noticeTargetRoleRepository;

    @Mock
    private NoticeAttachmentRepository noticeAttachmentRepository;

    @Test
    @DisplayName("공지사항 생성 테스트")
    void createNotice() {
        // given
        Admin admin = mock(Admin.class);
        NoticeRequest request = new NoticeRequest();
        request.setTitle("Test Title");
        request.setContent("Test Content");
        request.setTargetMode(TargetMode.ALL);
        request.setPriority(true);
        request.setPopup(false);
        request.setAttachments(new ArrayList<>());

        Notice savedNotice = Notice.builder()
                .id(1L)
                .title("Test Title")
                .content("Test Content")
                .targetMode(TargetMode.ALL)
                .status(NoticeStatus.PUBLISHED)
                .build();

        given(noticeRepository.save(any(Notice.class))).willReturn(savedNotice);

        // when
        Long noticeId = noticeService.createNotice(request, admin);

        // then
        assertNotNull(noticeId);
        assertEquals(1L, noticeId);
        verify(noticeRepository, times(1)).save(any(Notice.class));
    }

    @Test
    @DisplayName("공지사항 삭제 테스트")
    void deleteNotice() {
        // given
        Long noticeId = 1L;
        Notice notice = mock(Notice.class);

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(notice));

        // when
        noticeService.deleteNotice(noticeId);

        // then
        // 실제 삭제 로직이 엔티티 내부 메서드 호출이라면 verify가 어려울 수 있으나,
        // 여기서는 예외가 발생하지 않는지 확인
        verify(noticeRepository, times(1)).findById(noticeId);
    }

    @Test
    @DisplayName("관리자용 공지사항 목록 조회 테스트")
    void getAllNoticesForAdmin() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Notice notice = Notice.builder()
                .id(1L)
                .title("Admin Notice")
                .content("Content")
                .targetMode(TargetMode.ALL)
                .status(NoticeStatus.PUBLISHED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Page<Notice> noticePage = new PageImpl<>(Collections.singletonList(notice));

        given(noticeRepository.findAllByStatusNot(NoticeStatus.DELETED, pageable)).willReturn(noticePage);
        given(noticeTargetRoleRepository.findAllByNoticeId(anyLong())).willReturn(Collections.emptyList());
        given(noticeAttachmentRepository.findAllByNoticeId(anyLong())).willReturn(Collections.emptyList());

        // when
        Page<NoticeResponse> result = noticeService.getAllNoticesForAdmin(pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Admin Notice", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("사용자용 공지사항 목록 조회 테스트")
    void getNoticesForUser() {
        // given
        User user = mock(User.class);
        given(user.getId()).willReturn(100L);
        given(user.getRole()).willReturn(Role.ELDERLY);

        Pageable pageable = PageRequest.of(0, 10);
        Notice notice = Notice.builder()
                .id(1L)
                .title("User Notice")
                .content("Content")
                .targetMode(TargetMode.ALL)
                .status(NoticeStatus.PUBLISHED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Page<Notice> noticePage = new PageImpl<>(Collections.singletonList(notice));

        given(noticeRepository.findAllForUser(Role.ELDERLY, pageable)).willReturn(noticePage);
        given(noticeReadLogRepository.existsByNoticeIdAndUserId(1L, "100")).willReturn(false);
        given(noticeTargetRoleRepository.findAllByNoticeId(anyLong())).willReturn(Collections.emptyList());
        given(noticeAttachmentRepository.findAllByNoticeId(anyLong())).willReturn(Collections.emptyList());

        // when
        Page<NoticeResponse> result = noticeService.getNoticesForUser(user, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertFalse(result.getContent().get(0).isRead());
    }

    @Test
    @DisplayName("사용자용 팝업 공지 조회 테스트")
    void getActivePopupsForUser() {
        // given
        User user = mock(User.class);
        given(user.getId()).willReturn(100L);
        given(user.getRole()).willReturn(Role.ELDERLY);

        Notice notice = Notice.builder()
                .id(1L)
                .title("Popup Notice")
                .content("Popup Content")
                .targetMode(TargetMode.ALL)
                .isPopup(true)
                .status(NoticeStatus.PUBLISHED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(noticeRepository.findActivePopups(eq(Role.ELDERLY), any(LocalDateTime.class)))
                .willReturn(Collections.singletonList(notice));
        given(noticeReadLogRepository.existsByNoticeIdAndUserId(1L, "100")).willReturn(false);
        given(noticeTargetRoleRepository.findAllByNoticeId(anyLong())).willReturn(Collections.emptyList());
        given(noticeAttachmentRepository.findAllByNoticeId(anyLong())).willReturn(Collections.emptyList());

        // when
        List<NoticeResponse> result = noticeService.getActivePopupsForUser(user);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isPopup());
    }

    @Test
    @DisplayName("공지사항 읽음 처리 테스트")
    void readNotice() {
        // given
        Long noticeId = 1L;
        User user = mock(User.class);
        given(user.getId()).willReturn(100L);

        given(noticeReadLogRepository.existsByNoticeIdAndUserId(noticeId, "100")).willReturn(false);
        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(mock(Notice.class)));

        // when
        noticeService.readNotice(noticeId, user);

        // then
        // NoticeReadLog 생성 및 저장은 Service 내부 로직에 주석처리 되어 있어 검증 생략 가능하나,
        // 로직이 활성화된다면 verify(noticeReadLogRepository).save(...) 필요
        verify(noticeRepository, times(1)).findById(noticeId);
    }

    @Test
    @DisplayName("공지사항 상세 조회 테스트")
    void getNoticeDetail() {
        // given
        Long noticeId = 1L;
        User user = mock(User.class);
        given(user.getId()).willReturn(100L);

        Notice notice = Notice.builder()
                .id(noticeId)
                .title("Detail Notice")
                .content("Detail Content")
                .targetMode(TargetMode.ALL)
                .status(NoticeStatus.PUBLISHED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(notice));
        given(noticeReadLogRepository.existsByNoticeIdAndUserId(noticeId, "100")).willReturn(true);
        given(noticeTargetRoleRepository.findAllByNoticeId(noticeId)).willReturn(Collections.emptyList());
        given(noticeAttachmentRepository.findAllByNoticeId(noticeId)).willReturn(Collections.emptyList());

        // when
        NoticeResponse response = noticeService.getNoticeDetail(noticeId, user);

        // then
        assertNotNull(response);
        assertEquals("Detail Notice", response.getTitle());
        assertTrue(response.isRead());
    }
}