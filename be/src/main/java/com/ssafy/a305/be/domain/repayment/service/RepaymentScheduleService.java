package com.ssafy.a305.be.domain.repayment.service;

import com.ssafy.a305.be.domain.contract.repository.ContractRepository;
import com.ssafy.a305.be.domain.repayment.repository.RepaymentScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.transaction.annotation.Transactional;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.repayment.dto.RepaymentScheduleListResDto;
import com.ssafy.a305.be.domain.repayment.dto.RepaymentScheduleResDto;
import com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule;
import com.ssafy.a305.be.global.exception.CustomBusinessException;
import com.ssafy.a305.be.global.exception.dto.ErrorCode;

@Service
@RequiredArgsConstructor
public class RepaymentScheduleService {
    private final ContractRepository contractRepository;
    private final RepaymentScheduleRepository repaymentScheduleRepository;
    @Transactional(readOnly = true)
    public RepaymentScheduleListResDto getRepaymentSchedules(Integer contractSeq, Integer userSeq) {
        // 1. 계약 정보 조회
        Contract contract = contractRepository.findById(contractSeq)
                .orElseThrow(() -> new CustomBusinessException(ErrorCode.CONTRACT_NOT_FOUND, "존재하지 않는 계약입니다."));
        // 2. 권한 검증 (요청자가 채권자이거나 채무자인지 확인)
        if (!contract.getCreditor().getUserSeq().equals(userSeq)
                && !contract.getDebtor().getUserSeq().equals(userSeq)) {
            throw new CustomBusinessException(ErrorCode.ACCESS_DENIED, "해당 계약의 당사자가 아닌 사용자가 요청");
        }
        // 3. 해당 계약의 상환 스케줄 전체 조회
        List<RepaymentSchedule> schedules = repaymentScheduleRepository.findAllByContractContractSeqOrderByInstallmentNoAsc(contractSeq);
        // 4. DTO 변환 및 반환
        List<RepaymentScheduleResDto> dtos = schedules.stream()
                .map(RepaymentScheduleResDto::fromEntity)
                .collect(Collectors.toList());
        return RepaymentScheduleListResDto.of(contractSeq, dtos);
    }
}