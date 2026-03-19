package com.ssafy.a305.be.domain.user.repository;

import com.ssafy.a305.be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    boolean existsByEmail(String email);
    boolean existsByPhoneAndUserSeqNot(String phone, Integer userSeq);
    boolean existsByPhoneAndState(String phone, String state);
    Optional<User> findByEmail(String email);

    java.util.List<User> findByEmailContaining(String email);
}
