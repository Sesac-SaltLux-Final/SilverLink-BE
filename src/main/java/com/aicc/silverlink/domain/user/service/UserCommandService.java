package com.aicc.silverlink.domain.user.service;

import com.aicc.silverlink.domain.user.dto.UserRequests;
import com.aicc.silverlink.domain.user.dto.UserResponses;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final UserRepository userRepo;

    @Transactional(readOnly = true)
    public UserResponses.MyProfileResponse getMyProfile(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(()-> new IllegalArgumentException("USER_NOT_FOUND"));
        return UserResponses.MyProfileResponse.from(user);

    }

    @Transactional
    public UserResponses.MyProfileResponse updateMyProfile(Long userId, UserRequests.UpdateMyProfileRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(()-> new IllegalArgumentException("USER_NOT_FOUND"));

        if (user.getStatus() == UserStatus.DELETED) throw new IllegalStateException("USER_DELETED");
        if (user.getStatus() == UserStatus.LOCKED) throw new IllegalStateException("USER_LOCKED");

        user.updateName(req.name());
        user.updateEmail(req.email());

        return UserResponses.MyProfileResponse.from(user);

    }

    @Transactional
    public void ChangeStatus(Long targetUserId, UserStatus status) {
        User user = userRepo.findById(targetUserId)
                .orElseThrow(()-> new IllegalArgumentException("USER_NOT_FOUND"));

        switch (status) {
            case ACTIVE -> user.activate();
            case LOCKED -> user.suspend();
            case DELETED -> user.softDelete();
        }
    }



}
