package com.ssafy.a305.be.domain.contract.controller;

import com.ssafy.a305.be.domain.auth.security.AuthenticatedUser;
import com.ssafy.a305.be.domain.contract.service.ContractDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

/**
 * 차용증 문서 Controller
 */
@RestController
@RequestMapping("/api/v1/contracts/{contractSeq}/document")
@RequiredArgsConstructor
public class ContractDocumentController {

    private final ContractDocumentService contractDocumentService;

    /**
     * 차용증 HTML 전문보기 (WebView용)
     */
    @GetMapping("/html")
    public ResponseEntity<String> getContractHtml(
            @PathVariable Integer contractSeq,
            @AuthenticationPrincipal AuthenticatedUser authUser) {

        Integer userSeq = authUser.userSeq();
        String html = contractDocumentService.generateHtml(contractSeq, userSeq);

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }

    /**
     * 차용증 PDF 다운로드
     */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadContractPdf(
            @PathVariable Integer contractSeq,
            @AuthenticationPrincipal AuthenticatedUser authUser) {

        Integer userSeq = authUser.userSeq();
        byte[] pdfBytes = contractDocumentService.generatePdf(contractSeq, userSeq);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDisposition(ContentDisposition.builder("attachment")
                .filename("차용증_" + contractSeq + ".pdf", StandardCharsets.UTF_8)
                .build());
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}
