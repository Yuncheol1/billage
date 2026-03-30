package com.ssafy.a305.be.domain.user.repository;

import com.ssafy.a305.be.domain.user.entity.SocialAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SocialAuthRepository extends JpaRepository<SocialAuth, Integer> {
    Optional<SocialAuth> findByProviderAndProviderUserId(String provider, String providerUserId);
    void deleteByUserUserSeq(Integer userSeq);
}
