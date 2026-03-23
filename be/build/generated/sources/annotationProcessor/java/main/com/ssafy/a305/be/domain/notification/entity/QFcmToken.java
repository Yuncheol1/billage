package com.ssafy.a305.be.domain.notification.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QFcmToken is a Querydsl query type for FcmToken
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QFcmToken extends EntityPathBase<FcmToken> {

    private static final long serialVersionUID = -2005762848L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QFcmToken fcmToken = new QFcmToken("fcmToken");

    public final DateTimePath<java.time.LocalDateTime> createdDttm = createDateTime("createdDttm", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> invalidatedDttm = createDateTime("invalidatedDttm", java.time.LocalDateTime.class);

    public final BooleanPath isActive = createBoolean("isActive");

    public final DateTimePath<java.time.LocalDateTime> lastUsedDttm = createDateTime("lastUsedDttm", java.time.LocalDateTime.class);

    public final StringPath token = createString("token");

    public final NumberPath<Long> tokenSeq = createNumber("tokenSeq", Long.class);

    public final com.ssafy.a305.be.domain.user.entity.QUser user;

    public QFcmToken(String variable) {
        this(FcmToken.class, forVariable(variable), INITS);
    }

    public QFcmToken(Path<? extends FcmToken> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QFcmToken(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QFcmToken(PathMetadata metadata, PathInits inits) {
        this(FcmToken.class, metadata, inits);
    }

    public QFcmToken(Class<? extends FcmToken> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new com.ssafy.a305.be.domain.user.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

