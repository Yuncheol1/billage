package com.ssafy.a305.be.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserConsent is a Querydsl query type for UserConsent
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserConsent extends EntityPathBase<UserConsent> {

    private static final long serialVersionUID = 883987512L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserConsent userConsent = new QUserConsent("userConsent");

    public final DateTimePath<java.time.LocalDateTime> agreedDttm = createDateTime("agreedDttm", java.time.LocalDateTime.class);

    public final NumberPath<Integer> consentSeq = createNumber("consentSeq", Integer.class);

    public final BooleanPath isAgreed = createBoolean("isAgreed");

    public final QTermsMaster termsMaster;

    public final QUser user;

    public QUserConsent(String variable) {
        this(UserConsent.class, forVariable(variable), INITS);
    }

    public QUserConsent(Path<? extends UserConsent> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserConsent(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserConsent(PathMetadata metadata, PathInits inits) {
        this(UserConsent.class, metadata, inits);
    }

    public QUserConsent(Class<? extends UserConsent> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.termsMaster = inits.isInitialized("termsMaster") ? new QTermsMaster(forProperty("termsMaster")) : null;
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

