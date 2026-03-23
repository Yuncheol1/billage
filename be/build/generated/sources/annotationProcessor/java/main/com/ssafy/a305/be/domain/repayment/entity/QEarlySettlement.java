package com.ssafy.a305.be.domain.repayment.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QEarlySettlement is a Querydsl query type for EarlySettlement
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QEarlySettlement extends EntityPathBase<EarlySettlement> {

    private static final long serialVersionUID = 1542065835L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QEarlySettlement earlySettlement = new QEarlySettlement("earlySettlement");

    public final com.ssafy.a305.be.domain.contract.entity.QContract contract;

    public final DateTimePath<java.time.LocalDateTime> createdDttm = createDateTime("createdDttm", java.time.LocalDateTime.class);

    public final com.ssafy.a305.be.domain.document.entity.QDocument document;

    public final NumberPath<java.math.BigDecimal> remainingInterest = createNumber("remainingInterest", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> remainingPrincipal = createNumber("remainingPrincipal", java.math.BigDecimal.class);

    public final com.ssafy.a305.be.domain.user.entity.QUser requestedByUser;

    public final DateTimePath<java.time.LocalDateTime> resolvedDttm = createDateTime("resolvedDttm", java.time.LocalDateTime.class);

    public final NumberPath<java.math.BigDecimal> settlementAmount = createNumber("settlementAmount", java.math.BigDecimal.class);

    public final NumberPath<Integer> settlementSeq = createNumber("settlementSeq", Integer.class);

    public final StringPath status = createString("status");

    public QEarlySettlement(String variable) {
        this(EarlySettlement.class, forVariable(variable), INITS);
    }

    public QEarlySettlement(Path<? extends EarlySettlement> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QEarlySettlement(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QEarlySettlement(PathMetadata metadata, PathInits inits) {
        this(EarlySettlement.class, metadata, inits);
    }

    public QEarlySettlement(Class<? extends EarlySettlement> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new com.ssafy.a305.be.domain.contract.entity.QContract(forProperty("contract"), inits.get("contract")) : null;
        this.document = inits.isInitialized("document") ? new com.ssafy.a305.be.domain.document.entity.QDocument(forProperty("document"), inits.get("document")) : null;
        this.requestedByUser = inits.isInitialized("requestedByUser") ? new com.ssafy.a305.be.domain.user.entity.QUser(forProperty("requestedByUser"), inits.get("requestedByUser")) : null;
    }

}

