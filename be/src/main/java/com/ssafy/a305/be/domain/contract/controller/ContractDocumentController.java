package com.ssafy.a305.be.domain.contract.controller;

import com.ssafy.a305.be.domain.contract.service.ContractDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;

/**
 * 차용증 문서 Controller.
 *
 * <p>HTML 전문보기 및 PDF 다운로드 엔드포인트를 제공합니다.</p>
 */
@RestController
@RequestMapping("/api/v1/contracts/{contractSeq}/document")
@RequiredArgsConstructor
public class ContractDocumentController {

    private final ContractDocumentService contractDocumentService;

    /**
     * 차용증 HTML 전문보기 (WebView 렌더링용).
     *
     * @param contractSeq 계약 식별자
     * @return text/html 문자열
     */
    @GetMapping("/html")
    public ResponseEntity<String> getContractHtml(
            @PathVariable Integer contractSeq,
            @RequestAttribute("userSeq") Integer userSeq) {

        String html = contractDocumentService.generateHtml(contractSeq, userSeq);

        return ResponseEntity.ok()
                .contentType(new MediaType("text", "html", StandardCharsets.UTF_8))
                .body(html);
    }

    /**
     * 차용증 PDF 다운로드 (바이트 스트림).
     *
     * @param contractSeq 계약 식별자
     * @return application/pdf 바이트 배열
     */
    @GetMapping("/pdf")
    public ResponseEntity<byte[]> downloadContractPdf(
            @PathVariable Integer contractSeq,
            @RequestAttribute("userSeq") Integer userSeq) {

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
