package com.ssafy.a305.be.domain.contract.service;

import com.lowagie.text.pdf.BaseFont;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.contract.entity.ContractClause;
import com.ssafy.a305.be.domain.contract.repository.ContractRepository;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.domain.user.entity.UserProfile;
import com.ssafy.a305.be.domain.user.repository.UserProfileRepository;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * 차용증 HTML 전문보기, PDF 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContractDocumentService {

    private final ContractRepository contractRepository;
    private final UserProfileRepository userProfileRepository;
    private final TemplateEngine templateEngine;

    private String fontAbsolutePath;

    @jakarta.annotation.PostConstruct
    public void initFont() {
        try {
            java.io.File fontFile = java.io.File.createTempFile("NanumGothic", ".ttf");
            fontFile.deleteOnExit();
            try (java.io.InputStream is = new ClassPathResource("static/fonts/NanumGothic.ttf").getInputStream();
                 java.io.FileOutputStream fos = new java.io.FileOutputStream(fontFile)) {
                org.springframework.util.StreamUtils.copy(is, fos);
            }
            this.fontAbsolutePath = fontFile.getAbsolutePath();
            log.info("폰트가 임시 디렉토리에 복사되었습니다: {}", this.fontAbsolutePath);
        } catch (Exception e) {
            log.error("폰트 임시 파일 복사 실패", e);
        }
    }

    /**
     * 차용증 HTML 문자열을 생성합니다 (WebView 전문보기용).
     */
    @Transactional(readOnly = true)
    public String generateHtml(Integer contractSeq, Integer userSeq) {
        Contract contract = loadAndAuthorize(contractSeq, userSeq);
        Context context = buildThymeleafContext(contract);
        return templateEngine.process("contract_document", context);
    }

    /**
     * 차용증 PDF 바이트 배열을 생성합니다 (다운로드용).
     */
    @Transactional(readOnly = true)
    public byte[] generatePdf(Integer contractSeq, Integer userSeq) {
        String html = generateHtml(contractSeq, userSeq);

        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();

            // 한글 폰트 등록 (Flying Saucer는 CSS @font-face만으로는 한글을 렌더링하지 못함)
            if (this.fontAbsolutePath != null) {
                renderer.getFontResolver().addFont(fontAbsolutePath, BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
            } else {
                log.warn("폰트 경로가 null입니다. 한글 렌더링이 깨질 수 있습니다.");
            }

            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(os);

            log.info("[차용증 PDF] 생성 완료 - contractSeq={}", contractSeq);
            return os.toByteArray();

        } catch (Exception e) {
            log.error("[차용증 PDF] 생성 실패 - contractSeq={}, 사유: {}", contractSeq, e.getMessage(), e);
            throw new RuntimeException("PDF 생성 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 계약을 조회하고 요청자가 당사자인지 권한 검증합니다.
     */
    private Contract loadAndAuthorize(Integer contractSeq, Integer userSeq) {
        Contract contract = contractRepository.findContractDetailById(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "계약을 찾을 수 없습니다."));

        boolean isCreditor = contract.getCreditor().getUserSeq().equals(userSeq);
        boolean isDebtor = contract.getDebtor().getUserSeq().equals(userSeq);

        if (!isCreditor && !isDebtor) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "해당 차용증을 열람할 권한이 없습니다.");
        }

        return contract;
    }

    /**
     * Thymeleaf Context를 빌드합니다.
     */
    private Context buildThymeleafContext(Contract contract) {
        Context ctx = new Context();
        User creditor = contract.getCreditor();
        User debtor = contract.getDebtor();

        // 금액
        BigDecimal principal = contract.getPrincipalAmount();
        ctx.setVariable("amountText", convertToKoreanAmount(principal));
        ctx.setVariable("amountNumber", NumberFormat.getNumberInstance(Locale.KOREA).format(principal));

        // 계약번호
        ctx.setVariable("contractId", contract.getContractId());

        // 기간 (시작일)
        LocalDate startDt = contract.getStartDt();
        ctx.setVariable("startYear", String.valueOf(startDt.getYear()));
        ctx.setVariable("startMonth", String.format("%02d", startDt.getMonthValue()));
        ctx.setVariable("startDay", String.format("%02d", startDt.getDayOfMonth()));

        // 기간 (종료일)
        LocalDate endDt = contract.getEndDt();
        ctx.setVariable("endYear", String.valueOf(endDt.getYear()));
        ctx.setVariable("endMonth", String.format("%02d", endDt.getMonthValue()));
        ctx.setVariable("endDay", String.format("%02d", endDt.getDayOfMonth()));

        // 이자율
        BigDecimal interestRate = contract.getInterestRate();
        ctx.setVariable("interestRate", interestRate != null
                ? interestRate.multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString()
                : "0");

        // 상환 방식
        ctx.setVariable("repaymentMethodText", mapRepaymentMethod(contract.getRepaymentMethod()));

        // 매월 상환일 (할부인 경우 첫 스케줄의 일(day)로 추출)
        if (!"LUMP_SUM".equals(contract.getRepaymentMethod()) && !contract.getRepaymentSchedules().isEmpty()) {
            int day = contract.getRepaymentSchedules().get(0).getDueDt().getDayOfMonth();
            ctx.setVariable("repaymentDay", String.valueOf(day));
        }

        // 당사자 성명 (UserProfile에서 조회)
        UserProfile creditorProfile = userProfileRepository.findByUserUserSeq(creditor.getUserSeq()).orElse(null);
        UserProfile debtorProfile = userProfileRepository.findByUserUserSeq(debtor.getUserSeq()).orElse(null);

        ctx.setVariable("creditorName", creditorProfile != null && creditorProfile.getRealName() != null
                ? creditorProfile.getRealName() : creditor.getEmail());
        ctx.setVariable("debtorName", debtorProfile != null && debtorProfile.getRealName() != null
                ? debtorProfile.getRealName() : debtor.getEmail());

        // 주민등록번호 마스킹
        ctx.setVariable("creditorRrn", maskRrn(creditorProfile));
        ctx.setVariable("debtorRrn", maskRrn(debtorProfile));

        // 작성일자
        LocalDateTime createdDttm = contract.getCreatedDttm();
        if (createdDttm != null) {
            ctx.setVariable("createdYear", String.valueOf(createdDttm.getYear()));
            ctx.setVariable("createdMonth", String.format("%02d", createdDttm.getMonthValue()));
            ctx.setVariable("createdDay", String.format("%02d", createdDttm.getDayOfMonth()));
        }

        // 특약사항 (orderNo 기준 정렬)
        List<ClauseDto> clauses = contract.getClauses().stream()
                .sorted(Comparator.comparingInt(ContractClause::getOrderNo))
                .map(c -> new ClauseDto(c.getTitle(), c.getContent()))
                .collect(Collectors.toList());
        ctx.setVariable("clauses", clauses);

        return ctx;
    }

    /**
     * 주민등록번호 마스킹: 앞 6자리 + '-' + 첫 1자리 + '******'
     */
    private String maskRrn(UserProfile profile) {
        if (profile == null || profile.getRrn() == null || profile.getRrn().length() < 8) {
            return "-";
        }
        String rrn = profile.getRrn().replace("-", "");
        if (rrn.length() < 7) return "-";
        return rrn.substring(0, 6) + "-" + rrn.charAt(6) + "******";
    }

    /**
     * 상환 방식 코드를 한글로 변환합니다.
     */
    private String mapRepaymentMethod(String method) {
        if (method == null) return "-";
        return switch (method) {
            case "LUMP_SUM" -> "만기 일시 상환";
            case "INSTALLMENT" -> "원리금 균등 분할 상환";
            case "BULLET" -> "만기 일시 상환";
            default -> method;
        };
    }

    /**
     * 숫자를 한글 금액 표현으로 변환합니다 (예: 5000000 → 오백만).
     */
    private String convertToKoreanAmount(BigDecimal amount) {
        if (amount == null) return "-";
        long value = amount.longValue();

        if (value == 0) return "영";

        String[] units = {"", "만", "억", "조"};
        String[] digits = {"", "일", "이", "삼", "사", "오", "육", "칠", "팔", "구"};
        String[] subUnits = {"", "십", "백", "천"};

        StringBuilder result = new StringBuilder();
        int unitIdx = 0;

        while (value > 0) {
            int chunk = (int) (value % 10000);
            if (chunk > 0) {
                StringBuilder chunkStr = new StringBuilder();
                for (int i = 0; chunk > 0; i++) {
                    int digit = chunk % 10;
                    if (digit > 0) {
                        String digitStr = (digit == 1 && i > 0) ? "" : digits[digit];
                        chunkStr.insert(0, digitStr + subUnits[i]);
                    }
                    chunk /= 10;
                }
                chunkStr.append(units[unitIdx]);
                result.insert(0, chunkStr);
            }
            value /= 10000;
            unitIdx++;
        }

        return result.toString();
    }

    /**
     * Thymeleaf 렌더링용 특약사항 DTO.
     */
    public static class ClauseDto {
        private final String title;
        private final String content;

        public ClauseDto(String title, String content) {
            this.title = title;
            this.content = content;
        }

        public String getTitle() { return title; }
        public String getContent() { return content; }
    }
}
