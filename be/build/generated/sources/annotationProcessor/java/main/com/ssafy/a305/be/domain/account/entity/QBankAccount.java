package com.ssafy.a305.be.domain.account.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QBankAccount is a Querydsl query type for BankAccount
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QBankAccount extends EntityPathBase<BankAccount> {

    private static final long serialVersionUID = 1976716630L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QBankAccount bankAccount = new QBankAccount("bankAccount");

    public final StringPath accountHolderName = createString("accountHolderName");

    public final StringPath accountNumberMasked = createString("accountNumberMasked");

    public final NumberPath<Integer> accountSeq = createNumber("accountSeq", Integer.class);

    public final StringPath accountToken = createString("accountToken");

    public final StringPath bankCode = createString("bankCode");

    public final BooleanPath isPrimary = createBoolean("isPrimary");

    public final DateTimePath<java.time.LocalDateTime> linkedDttm = createDateTime("linkedDttm", java.time.LocalDateTime.class);

    public final StringPath provider = createString("provider");

    public final EnumPath<com.ssafy.a305.be.domain.enums.BankAccountStatus> status = createEnum("status", com.ssafy.a305.be.domain.enums.BankAccountStatus.class);

    public final com.ssafy.a305.be.domain.user.entity.QUser user;

    public QBankAccount(String variable) {
        this(BankAccount.class, forVariable(variable), INITS);
    }

    public QBankAccount(Path<? extends BankAccount> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QBankAccount(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QBankAccount(PathMetadata metadata, PathInits inits) {
        this(BankAccount.class, metadata, inits);
    }

    public QBankAccount(Class<? extends BankAccount> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.ssafy.a305.be.domain.user.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

