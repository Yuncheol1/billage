package com.ssafy.a305.be.domain.repayment.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QAutoTransferRegistration is a Querydsl query type for AutoTransferRegistration
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QAutoTransferRegistration extends EntityPathBase<AutoTransferRegistration> {

    private static final long serialVersionUID = -758831692L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QAutoTransferRegistration autoTransferRegistration = new QAutoTransferRegistration("autoTransferRegistration");

    public final NumberPath<java.math.BigDecimal> amount = createNumber("amount", java.math.BigDecimal.class);

    public final DateTimePath<java.time.LocalDateTime> canceledDttm = createDateTime("canceledDttm", java.time.LocalDateTime.class);

    public final com.ssafy.a305.be.domain.contract.entity.QContract contract;

    public final com.ssafy.a305.be.domain.account.entity.QBankAccount creditorAccount;

    public final com.ssafy.a305.be.domain.account.entity.QBankAccount debtorAccount;

    public final BooleanPath isActive = createBoolean("isActive");

    public final DateTimePath<java.time.LocalDateTime> registeredDttm = createDateTime("registeredDttm", java.time.LocalDateTime.class);

    public final NumberPath<Integer> registrationSeq = createNumber("registrationSeq", Integer.class);

    public final NumberPath<Integer> transferDay = createNumber("transferDay", Integer.class);

    public QAutoTransferRegistration(String variable) {
        this(AutoTransferRegistration.class, forVariable(variable), INITS);
    }

    public QAutoTransferRegistration(Path<? extends AutoTransferRegistration> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QAutoTransferRegistration(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QAutoTransferRegistration(PathMetadata metadata, PathInits inits) {
        this(AutoTransferRegistration.class, metadata, inits);
    }

    public QAutoTransferRegistration(Class<? extends AutoTransferRegistration> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new com.ssafy.a305.be.domain.contract.entity.QContract(forProperty("contract"), inits.get("contract")) : null;
        this.creditorAccount = inits.isInitialized("creditorAccount") ? new com.ssafy.a305.be.domain.account.entity.QBankAccount(forProperty("creditorAccount"), inits.get("creditorAccount")) : null;
        this.debtorAccount = inits.isInitialized("debtorAccount") ? new com.ssafy.a305.be.domain.account.entity.QBankAccount(forProperty("debtorAccount"), inits.get("debtorAccount")) : null;
    }

}

