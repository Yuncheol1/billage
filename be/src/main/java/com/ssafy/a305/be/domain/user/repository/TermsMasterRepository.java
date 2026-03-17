package com.ssafy.a305.be.domain.user.repository;

import com.ssafy.a305.be.domain.enums.TermsType;
import com.ssafy.a305.be.domain.user.entity.TermsMaster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface TermsMasterRepository extends JpaRepository<TermsMaster, Integer> {
    List<TermsMaster> findByTermsType(TermsType termsType);
    List<TermsMaster> findByTermsSeqIn(Collection<Integer> termsSeqs);
    List<TermsMaster> findAllByOrderByTermsSeqAsc();
}
