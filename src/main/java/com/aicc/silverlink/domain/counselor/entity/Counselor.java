package com.aicc.silverlink.domain.counselor.entity;

import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;

@Entity
@Table(name = "counselors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Counselor {
    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "employee_no", length = 20)
    private String employeeNo;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "office_phone", length = 20)
    private String officePhone;

    @Column(name = "joined_at")
    private LocalDate joinedAt;

    @Column(name = "adm_dong_code", nullable = false, length = 20)
    private String admDongCode;

    public Counselor(User user, String admDongCode) {
        this.user = user;
        this.admDongCode = admDongCode;
    }
}
