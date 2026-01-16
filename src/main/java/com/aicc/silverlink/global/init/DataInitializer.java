package com.aicc.silverlink.global.init;

import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        // "test" 라는 아이디가 없으면 하나 만듦
        if (userRepository.findByLoginId("test").isEmpty()) {
            User testUser = User.builder()
                    .loginId("test")
                    .passwordHash(passwordEncoder.encode("1234")) // 비번 1234 암호화
                    .name("테스터")
                    .phone("010-1234-5678")
                    .role(Role.COUNSELOR) // 혹은 Role.ADMIN
                    .status(UserStatus.ACTIVE)
                    .phoneVerified(true)
                    .build();

            userRepository.save(testUser);
            System.out.println("✅ 테스트용 유저 생성 완료: ID=test / PW=1234");
        }
    }
}