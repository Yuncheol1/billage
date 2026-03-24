package com.ssafy.a305.be.domain.document.repository;

import com.ssafy.a305.be.domain.document.entity.Signature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, Integer> {
    boolean existsByDocumentDocumentSeqAndUserUserSeq(Integer documentSeq, Integer userSeq);
    long countByDocumentDocumentSeq(Integer documentSeq);
    void deleteByDocumentContractContractSeq(Integer contractSeq);
}
