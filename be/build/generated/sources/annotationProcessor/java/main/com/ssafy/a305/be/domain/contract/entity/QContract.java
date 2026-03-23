package com.ssafy.a305.be.domain.contract.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QContract is a Querydsl query type for Contract
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QContract extends EntityPathBase<Contract> {

    private static final long serialVersionUID = 1471100688L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContract contract = new QContract("contract");

    public final StringPath aiDraftContent = createString("aiDraftContent");

    public final ListPath<ContractClause, QContractClause> clauses = this.<ContractClause, QContractClause>createList("clauses", ContractClause.class, QContractClause.class, PathInits.DIRECT2);

    public final StringPath contractId = createString("contractId");

    public final NumberPath<Integer> contractSeq = createNumber("contractSeq", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> createdDttm = createDateTime("createdDttm", java.time.LocalDateTime.class);

    public final com.ssafy.a305.be.domain.user.entity.QUser creditor;

    public final com.ssafy.a305.be.domain.user.entity.QUser debtor;

    public final DatePath<java.time.LocalDate> endDt = createDate("endDt", java.time.LocalDate.class);

    public final BooleanPath family = createBoolean("family");

    public final NumberPath<java.math.BigDecimal> interestRate = createNumber("interestRate", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> principalAmount = createNumber("principalAmount", java.math.BigDecimal.class);

    public final StringPath repaymentMethod = createString("repaymentMethod");

    public final ListPath<com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule, com.ssafy.a305.be.domain.repayment.entity.QRepaymentSchedule> repaymentSchedules = this.<com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule, com.ssafy.a305.be.domain.repayment.entity.QRepaymentSchedule>createList("repaymentSchedules", com.ssafy.a305.be.domain.repayment.entity.RepaymentSchedule.class, com.ssafy.a305.be.domain.repayment.entity.QRepaymentSchedule.class, PathInits.DIRECT2);

    public final DateTimePath<java.time.LocalDateTime> signedDttm = createDateTime("signedDttm", java.time.LocalDateTime.class);

    public final DatePath<java.time.LocalDate> startDt = createDate("startDt", java.time.LocalDate.class);

    public final StringPath status = createString("status");

    public final NumberPath<Integer> totalInstallments = createNumber("totalInstallments", Integer.class);

    public final DateTimePath<java.time.LocalDateTime> updatedDttm = createDateTime("updatedDttm", java.time.LocalDateTime.class);

    public QContract(String variable) {
        this(Contract.class, forVariable(variable), INITS);
    }

    public QContract(Path<? extends Contract> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QContract(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QContract(PathMetadata metadata, PathInits inits) {
        this(Contract.class, metadata, inits);
    }

    public QContract(Class<? extends Contract> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.creditor = inits.isInitialized("creditor") ? new com.ssafy.a305.be.domain.user.entity.QUser(forProperty("creditor"), inits.get("creditor")) : null;
        this.debtor = inits.isInitialized("debtor") ? new com.ssafy.a305.be.domain.user.entity.QUser(forProperty("debtor"), inits.get("debtor")) : null;
    }

}

