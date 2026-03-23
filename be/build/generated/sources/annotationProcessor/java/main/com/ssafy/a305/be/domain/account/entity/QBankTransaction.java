package com.ssafy.a305.be.domain.account.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBankTransaction is a Querydsl query type for BankTransaction
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBankTransaction extends EntityPathBase<BankTransaction> {

    private static final long serialVersionUID = -892872313L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBankTransaction bankTransaction = new QBankTransaction("bankTransaction");

    public final NumberPath<java.math.BigDecimal> amount = createNumber("amount", java.math.BigDecimal.class);

    public final QBankAccount bankAccount;

    public final StringPath counterpartyName = createString("counterpartyName");

    public final EnumPath<com.ssafy.a305.be.domain.enums.BankTransactionDirection> direction = createEnum("direction", com.ssafy.a305.be.domain.enums.BankTransactionDirection.class);

    public final StringPath hash = createString("hash");

    public final NumberPath<java.math.BigDecimal> matchConfidence = createNumber("matchConfidence", java.math.BigDecimal.class);

    public final com.ssafy.a305.be.domain.repayment.entity.QRepaymentSchedule matchedSchedule;

    public final StringPath matchStatus = createString("matchStatus");

    public final StringPath memo = createString("memo");

    public final StringPath provider = createString("provider");

    public final StringPath rawPayload = createString("rawPayload");

    public final DateTimePath<java.time.LocalDateTime> txDttm = createDateTime("txDttm", java.time.LocalDateTime.class);

    public final NumberPath<Long> txSeq = createNumber("txSeq", Long.class);

    public QBankTransaction(String variable) {
        this(BankTransaction.class, forVariable(variable), INITS);
    }

    public QBankTransaction(Path<? extends BankTransaction> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBankTransaction(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBankTransaction(PathMetadata metadata, PathInits inits) {
        this(BankTransaction.class, metadata, inits);
    }

    public QBankTransaction(Class<? extends BankTransaction> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.bankAccount = inits.isInitialized("bankAccount") ? new QBankAccount(forProperty("bankAccount"), inits.get("bankAccount")) : null;
        this.matchedSchedule = inits.isInitialized("matchedSchedule") ? new com.ssafy.a305.be.domain.repayment.entity.QRepaymentSchedule(forProperty("matchedSchedule"), inits.get("matchedSchedule")) : null;
    }

}

