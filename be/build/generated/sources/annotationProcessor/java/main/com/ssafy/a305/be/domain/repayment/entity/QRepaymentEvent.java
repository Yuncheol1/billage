package com.ssafy.a305.be.domain.repayment.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRepaymentEvent is a Querydsl query type for RepaymentEvent
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRepaymentEvent extends EntityPathBase<RepaymentEvent> {

    private static final long serialVersionUID = -224913336L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRepaymentEvent repaymentEvent = new QRepaymentEvent("repaymentEvent");

    public final NumberPath<java.math.BigDecimal> amountPaid = createNumber("amountPaid", java.math.BigDecimal.class);

    public final com.ssafy.a305.be.domain.account.entity.QBankTransaction bankTransaction;

    public final com.ssafy.a305.be.domain.user.entity.QUser confirmedByUser;

    public final DateTimePath<java.time.LocalDateTime> confirmedDttm = createDateTime("confirmedDttm", java.time.LocalDateTime.class);

    public final com.ssafy.a305.be.domain.contract.entity.QContract contract;

    public final NumberPath<Long> eventSeq = createNumber("eventSeq", Long.class);

    public final StringPath evidenceGrade = createString("evidenceGrade");

    public final StringPath field = createString("field");

    public final StringPath note = createString("note");

    public final QRepaymentSchedule repaymentSchedule;

    public final com.ssafy.a305.be.domain.user.entity.QUser reportedByUser;

    public final DateTimePath<java.time.LocalDateTime> requestedDttm = createDateTime("requestedDttm", java.time.LocalDateTime.class);

    public final StringPath status = createString("status");

    public QRepaymentEvent(String variable) {
        this(RepaymentEvent.class, forVariable(variable), INITS);
    }

    public QRepaymentEvent(Path<? extends RepaymentEvent> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRepaymentEvent(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRepaymentEvent(PathMetadata metadata, PathInits inits) {
        this(RepaymentEvent.class, metadata, inits);
    }

    public QRepaymentEvent(Class<? extends RepaymentEvent> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.bankTransaction = inits.isInitialized("bankTransaction") ? new com.ssafy.a305.be.domain.account.entity.QBankTransaction(forProperty("bankTransaction"), inits.get("bankTransaction")) : null;
        this.confirmedByUser = inits.isInitialized("confirmedByUser") ? new com.ssafy.a305.be.domain.user.entity.QUser(forProperty("confirmedByUser"), inits.get("confirmedByUser")) : null;
        this.contract = inits.isInitialized("contract") ? new com.ssafy.a305.be.domain.contract.entity.QContract(forProperty("contract"), inits.get("contract")) : null;
        this.repaymentSchedule = inits.isInitialized("repaymentSchedule") ? new QRepaymentSchedule(forProperty("repaymentSchedule"), inits.get("repaymentSchedule")) : null;
        this.reportedByUser = inits.isInitialized("reportedByUser") ? new com.ssafy.a305.be.domain.user.entity.QUser(forProperty("reportedByUser"), inits.get("reportedByUser")) : null;
    }

}

