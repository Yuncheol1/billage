package com.ssafy.a305.be.domain.document.repository;

import com.ssafy.a305.be.domain.document.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentRepository extends JpaRepository<Document, Integer> {
    Optional<Document> findTopByContractContractSeqAndDocTypeOrderByVersionDesc(Integer contractSeq, String docType);
}
