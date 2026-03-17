package com.ssafy.a305.be.domain.user.repository;

import com.ssafy.a305.be.domain.user.entity.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {
    Optional<UserProfile> findByUserUserSeq(Integer userSeq);
}
