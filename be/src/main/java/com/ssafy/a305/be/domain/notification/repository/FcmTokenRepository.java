package com.ssafy.a305.be.domain.notification.repository;

import com.ssafy.a305.be.domain.notification.entity.FcmToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FcmTokenRepository extends JpaRepository<FcmToken, Long> {
    Optional<FcmToken> findByUserUserSeqAndTokenAndIsActiveTrue(Integer userSeq, String token);
}
