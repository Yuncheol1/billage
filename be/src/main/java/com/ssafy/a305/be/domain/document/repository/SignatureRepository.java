package com.ssafy.a305.be.domain.document.repository;

import com.ssafy.a305.be.domain.document.entity.Signature;
import com.ssafy.a305.be.domain.document.entity.SignatureId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, SignatureId> {
    boolean existsByDocumentDocumentSeqAndUserUserSeq(Integer documentSeq, Integer userSeq);
    long countByDocumentDocumentSeq(Integer documentSeq);
}
