package com.ssafy.a305.be.domain.contract.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ssafy.a305.be.domain.contract.dto.request.ContractSignatureRequestDto;
import com.ssafy.a305.be.domain.contract.dto.response.ContractSignatureResponseDto;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.notification.service.NotificationCommandService;
import com.ssafy.a305.be.domain.contract.entity.ContractClause;
import com.ssafy.a305.be.domain.contract.repository.ContractRepository;
import com.ssafy.a305.be.domain.document.entity.Document;
import com.ssafy.a305.be.domain.document.entity.Signature;
import com.ssafy.a305.be.domain.document.repository.DocumentRepository;
import com.ssafy.a305.be.domain.document.repository.SignatureRepository;
import com.ssafy.a305.be.domain.enums.SignatureRole;
import com.ssafy.a305.be.domain.user.entity.User;
import com.ssafy.a305.be.domain.user.entity.UserDeviceKey;
import com.ssafy.a305.be.domain.user.entity.UserProfile;
import com.ssafy.a305.be.domain.user.repository.UserDeviceKeyRepository;
import com.ssafy.a305.be.domain.user.repository.UserProfileRepository;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@Slf4j
@RequiredArgsConstructor
public class ContractSignatureService {

    private static final Set<String> SIGNABLE_STATUSES = Set.of("DRAFT", "NEGOTIATING");
    private static final String CONTRACT_ORIGINAL = "CONTRACT_ORIGINAL";
    private static final String GENERATED_BY_SYSTEM = "SYSTEM";
    private static final String ACTIVE_USER_STATE = "ACTIVE";
    private static final String DEVICE_KEY_PLACEHOLDER = "UNREGISTERED_DEVICE_KEY";

    private final ContractRepository contractRepository;
    private final DocumentRepository documentRepository;
    private final SignatureRepository signatureRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserDeviceKeyRepository userDeviceKeyRepository;
    private final NotificationCommandService notificationCommandService;
    private final ObjectMapper objectMapper;

    @Transactional
    public ContractSignatureResponseDto submitSignature(
            Integer contractSeq,
            Integer userSeq,
            ContractSignatureRequestDto requestDto,
            String clientIp
    ) {
        validateRequest(requestDto);

        Contract contract = contractRepository.findByIdWithPessimisticLock(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND));

        User signer = resolveSigner(contract, userSeq);
        validateContractSignable(contract);

        Document document = documentRepository.findTopByContractContractSeqAndDocTypeOrderByVersionDesc(
                        contractSeq,
                        CONTRACT_ORIGINAL
                )
                .orElseGet(() -> createInitialDocument(contract));

        if (signatureRepository.existsByDocumentDocumentSeqAndUserUserSeq(document.getDocumentSeq(), userSeq)) {
            throw new CustomBusinessException(ErrorCode.SIGNATURE_ALREADY_SUBMITTED);
        }

        UserDeviceKey userDeviceKey = resolveUserDeviceKey(signer, requestDto.deviceIdHash());
        SignatureRole role = resolveRole(contract, userSeq);
        LocalDateTime now = LocalDateTime.now();

        Signature savedSignature = signatureRepository.save(Signature.builder()
                .userDeviceKey(userDeviceKey)
                .document(document)
                .user(signer)
                .role(role)
                .method(requestDto.method().trim().toUpperCase())
                .deviceIdHash(requestDto.deviceIdHash())
                .biometricVerified(requestDto.biometricVerified())
                .ipAddress(clientIp)
                .userAgent(requestDto.userAgent())
                .signaturePayload(toJson(requestDto.signaturePayload()))
                .signedDttm(now)
                .signatureValue(extractSignatureValue(requestDto.signaturePayload()))
                .build());

        boolean contractFullySigned = signatureRepository.countByDocumentDocumentSeq(document.getDocumentSeq()) >= 2;
        String documentContent = buildDocumentSnapshot(contract, contractFullySigned, now);
        document.refreshContent(documentContent, sha256(documentContent), now);

        Integer generatedDocumentSeq = null;
        if (contractFullySigned) {
            contract.markSigned(now);
            try {
                notificationCommandService.sendContractSignedNotifications(contract);
            } catch (Exception e) {
                log.error("Failed to send contract signed notifications. contractSeq={}", contractSeq, e);
            }
            generatedDocumentSeq = document.getDocumentSeq();
        }

        return ContractSignatureResponseDto.builder()
                .signatureSeq(savedSignature.getSignatureSeq())
                .userSeq(userSeq)
                .role(role.name())
                .contractFullySigned(contractFullySigned)
                .generatedDocumentSeq(generatedDocumentSeq)
                .build();
    }

    private void validateRequest(ContractSignatureRequestDto requestDto) {
        String method = requestDto.method().trim().toUpperCase();
        if (!Set.of("BIOMETRIC", "PATTERN", "DEVICE_ID").contains(method)) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "지원하지 않는 서명 방식입니다.");
        }
        if (requestDto.deviceIdHash() == null || requestDto.deviceIdHash().isBlank()) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "deviceIdHash는 필수입니다.");
        }
        if ("BIOMETRIC".equals(method) && !Boolean.TRUE.equals(requestDto.biometricVerified())) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "생체인증 서명은 biometricVerified=true 이어야 합니다.");
        }

        JsonNode signaturePayload = requestDto.signaturePayload();
        if (signaturePayload == null || signaturePayload.isEmpty()) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "signaturePayload는 비어 있을 수 없습니다.");
        }
        if ("BIOMETRIC".equals(method) && signaturePayload.path("canvasDataUrl").asText("").isBlank()) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "생체인증 서명은 canvasDataUrl이 필요합니다.");
        }
    }

    private User resolveSigner(Contract contract, Integer userSeq) {
        if (contract.getCreditor().getUserSeq().equals(userSeq)) {
            return contract.getCreditor();
        }
        if (contract.getDebtor().getUserSeq().equals(userSeq)) {
            return contract.getDebtor();
        }
        throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "계약 당사자만 서명할 수 있습니다.");
    }

    private SignatureRole resolveRole(Contract contract, Integer userSeq) {
        return contract.getCreditor().getUserSeq().equals(userSeq)
                ? SignatureRole.CREDITOR
                : SignatureRole.DEBTOR;
    }

    private void validateContractSignable(Contract contract) {
        if (!SIGNABLE_STATUSES.contains(contract.getStatus())) {
            throw new CustomBusinessException(ErrorCode.INVALID_CONTRACT_STATUS, "서명 가능한 계약 상태가 아닙니다.");
        }
        if (!ACTIVE_USER_STATE.equals(contract.getCreditor().getState())
                || !ACTIVE_USER_STATE.equals(contract.getDebtor().getState())) {
            throw new CustomBusinessException(ErrorCode.INACTIVE_USER_TARGET);
        }
    }

    private UserDeviceKey resolveUserDeviceKey(User user, String deviceIdHash) {
        return userDeviceKeyRepository.findFirstByUserUserSeqAndDeviceIdHashAndIsActiveTrueOrderByRegisteredDttmDesc(
                        user.getUserSeq(),
                        deviceIdHash
                )
                .orElseGet(() -> userDeviceKeyRepository.save(UserDeviceKey.builder()
                        .user(user)
                        .deviceIdHash(deviceIdHash)
                        .publicKeyPem(DEVICE_KEY_PLACEHOLDER)
                        .isActive(true)
                        .registeredDttm(LocalDateTime.now())
                        .build()));
    }

    private Document createInitialDocument(Contract contract) {
        LocalDateTime now = LocalDateTime.now();
        String initialContent = buildDocumentSnapshot(contract, false, now);
        return documentRepository.save(Document.builder()
                .contract(contract)
                .docType(CONTRACT_ORIGINAL)
                .version(1)
                .generatedBy(GENERATED_BY_SYSTEM)
                .generatedDttm(now)
                .docContentJson(initialContent)
                .sha256Hash(sha256(initialContent))
                .build());
    }

    private String buildDocumentSnapshot(Contract contract, boolean fullySigned, LocalDateTime generatedAt) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("contractSeq", contract.getContractSeq());
        snapshot.put("contractId", contract.getContractId());
        snapshot.put("status", fullySigned ? "SIGNED" : contract.getStatus());
        snapshot.put("generatedAt", generatedAt);
        snapshot.put("principalAmount", contract.getPrincipalAmount());
        snapshot.put("interestRate", contract.getInterestRate());
        snapshot.put("repaymentMethod", contract.getRepaymentMethod());
        snapshot.put("totalInstallments", contract.getTotalInstallments());
        snapshot.put("startDt", contract.getStartDt());
        snapshot.put("endDt", contract.getEndDt());
        snapshot.put("family", contract.getFamily());
        snapshot.put("creditor", buildPartySnapshot(contract.getCreditor(), "CREDITOR"));
        snapshot.put("debtor", buildPartySnapshot(contract.getDebtor(), "DEBTOR"));
        snapshot.put("clauses", buildClauseSnapshot(contract.getClauses()));
        snapshot.put("repaymentSchedulesCount", contract.getRepaymentSchedules().size());
        snapshot.put("contractFullySigned", fullySigned);
        return toJson(snapshot);
    }

    private Map<String, Object> buildPartySnapshot(User user, String role) {
        UserProfile profile = userProfileRepository.findByUserUserSeq(user.getUserSeq()).orElse(null);
        Map<String, Object> party = new LinkedHashMap<>();
        party.put("userSeq", user.getUserSeq());
        party.put("role", role);
        party.put("realName", profile != null ? profile.getRealName() : null);
        party.put("email", user.getEmail());
        party.put("phone", user.getPhone());
        party.put("state", user.getState());
        return party;
    }

    private List<Map<String, Object>> buildClauseSnapshot(List<ContractClause> clauses) {
        return clauses.stream()
                .map(clause -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("clauseSeq", clause.getClauseSeq());
                    item.put("title", clause.getTitle());
                    item.put("content", clause.getContent());
                    item.put("orderNo", clause.getOrderNo());
                    return item;
                })
                .toList();
    }

    private String extractSignatureValue(JsonNode signaturePayload) {
        String canvasDataUrl = signaturePayload.path("canvasDataUrl").asText("");
        if (!canvasDataUrl.isBlank()) {
            return canvasDataUrl;
        }
        return toJson(signaturePayload);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new CustomBusinessException(ErrorCode.INVALID_INPUT_VALUE, "JSON 직렬화에 실패했습니다.");
        }
    }

    private String sha256(String rawValue) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawValue.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (byte b : hash) {
                builder.append(String.format("%02x", b));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available.", e);
        }
    }
}
