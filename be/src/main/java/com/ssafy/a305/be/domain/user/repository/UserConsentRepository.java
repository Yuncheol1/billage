package com.ssafy.a305.be.domain.user.repository;

import com.ssafy.a305.be.domain.user.entity.UserConsent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserConsentRepository extends JpaRepository<UserConsent, Integer> {
    Optional<UserConsent> findByUserUserSeqAndTermsMasterTermsSeq(Integer userSeq, Integer termsSeq);
    List<UserConsent> findByUserUserSeq(Integer userSeq);
}
