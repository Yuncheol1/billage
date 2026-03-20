package com.ssafy.a305.be.domain.repayment.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QRepaymentSchedule is a Querydsl query type for RepaymentSchedule
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QRepaymentSchedule extends EntityPathBase<RepaymentSchedule> {

    private static final long serialVersionUID = -1220155863L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QRepaymentSchedule repaymentSchedule = new QRepaymentSchedule("repaymentSchedule");

    public final com.ssafy.a305.be.domain.contract.entity.QContract contract;

    public final DateTimePath<java.time.LocalDateTime> createdDttm = createDateTime("createdDttm", java.time.LocalDateTime.class);

    public final DatePath<java.time.LocalDate> dueDt = createDate("dueDt", java.time.LocalDate.class);

    public final NumberPath<Integer> installmentNo = createNumber("installmentNo", Integer.class);

    public final NumberPath<java.math.BigDecimal> interestDue = createNumber("interestDue", java.math.BigDecimal.class);

    public final DateTimePath<java.time.LocalDateTime> paidDttm = createDateTime("paidDttm", java.time.LocalDateTime.class);

    public final NumberPath<java.math.BigDecimal> principalDue = createNumber("principalDue", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> remainingBalanceAfter = createNumber("remainingBalanceAfter", java.math.BigDecimal.class);

    public final NumberPath<Integer> scheduleSeq = createNumber("scheduleSeq", Integer.class);

    public final StringPath status = createString("status");

    public final NumberPath<java.math.BigDecimal> totalDue = createNumber("totalDue", java.math.BigDecimal.class);

    public QRepaymentSchedule(String variable) {
        this(RepaymentSchedule.class, forVariable(variable), INITS);
    }

    public QRepaymentSchedule(Path<? extends RepaymentSchedule> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QRepaymentSchedule(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QRepaymentSchedule(PathMetadata metadata, PathInits inits) {
        this(RepaymentSchedule.class, metadata, inits);
    }

    public QRepaymentSchedule(Class<? extends RepaymentSchedule> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new com.ssafy.a305.be.domain.contract.entity.QContract(forProperty("contract"), inits.get("contract")) : null;
    }

}

