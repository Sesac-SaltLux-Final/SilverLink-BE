//package com.aicc.silverlink.domain.counseling.entity;
//
//import com.aicc.silverlink.domain.elderly.entity.Elderly;
//import com.aicc.silverlink.domain.guardian.entity.Guardian;
//import jakarta.persistence.*;
//import lombok.AccessLevel;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import java.math.BigDecimal;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "chatbot_logs")
//@Getter
//@NoArgsConstructor(access = AccessLevel.PROTECTED)
//public class ChatbotLog {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "log_id")
//    private Long id;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "guardian_user_id")
//    private Guardian guardian;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "elderly_user_id")
//    private Elderly elderly;
//
//    @Column(name = "session_id", length = 100)
//    private String sessionId;
//
//    @Column(name = "intent_label", length = 100)
//    private String intentLabel;
//
//    @Column(name = "intent_confidence", precision = 5, scale = 4)
//    private BigDecimal intentConfidence;
//
//    @Column(name = "source_type", length = 50)
//    private String sourceType;
//
//    @Column(name = "source_id")
//    private Long sourceId;
//
//    @Column(name = "answer_mode", length = 50)
//    private String answerMode;
//
//    @Column(name = "user_message_text", columnDefinition = "TEXT")
//    private String userMessageText;
//
//    @Column(name = "bot_response_text", columnDefinition = "TEXT")
//    private String botResponseText;
//
//    @Column(name = "meta", columnDefinition = "JSON")
//    private String meta;
//
//    @Column(name = "is_deleted", nullable = false)
//    private boolean isDeleted;
//
//    @Column(name = "deleted_at")
//    private LocalDateTime deletedAt;
//
//    @Column(name = "created_at", nullable = false, updatable = false)
//    private LocalDateTime createdAt;
//
//    @PrePersist
//    protected void onCreate() {
//        this.createdAt = LocalDateTime.now();
//    }
//}
