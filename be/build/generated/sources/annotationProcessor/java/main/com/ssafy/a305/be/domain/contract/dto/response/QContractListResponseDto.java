package com.ssafy.a305.be.domain.contract.dto.response;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.ConstructorExpression;
import javax.annotation.processing.Generated;

/**
 * com.ssafy.a305.be.domain.contract.dto.response.QContractListResponseDto is a Querydsl Projection type for ContractListResponseDto
 */
@Generated("com.querydsl.codegen.DefaultProjectionSerializer")
public class QContractListResponseDto extends ConstructorExpression<ContractListResponseDto> {

    private static final long serialVersionUID = 154712061L;

    public QContractListResponseDto(com.querydsl.core.types.Expression<Integer> contractSeq, com.querydsl.core.types.Expression<String> contractId, com.querydsl.core.types.Expression<String> role, com.querydsl.core.types.Expression<String> counterpartyName, com.querydsl.core.types.Expression<? extends java.math.BigDecimal> principalAmount, com.querydsl.core.types.Expression<String> repaymentMethod, com.querydsl.core.types.Expression<String> status, com.querydsl.core.types.Expression<java.time.LocalDate> startDt, com.querydsl.core.types.Expression<java.time.LocalDate> endDt) {
        super(ContractListResponseDto.class, new Class<?>[]{int.class, String.class, String.class, String.class, java.math.BigDecimal.class, String.class, String.class, java.time.LocalDate.class, java.time.LocalDate.class}, contractSeq, contractId, role, counterpartyName, principalAmount, repaymentMethod, status, startDt, endDt);
    }

}

