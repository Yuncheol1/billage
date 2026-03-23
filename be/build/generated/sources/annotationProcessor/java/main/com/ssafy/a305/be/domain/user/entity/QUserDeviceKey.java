package com.ssafy.a305.be.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserDeviceKey is a Querydsl query type for UserDeviceKey
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserDeviceKey extends EntityPathBase<UserDeviceKey> {

    private static final long serialVersionUID = -430594873L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserDeviceKey userDeviceKey = new QUserDeviceKey("userDeviceKey");

    public final StringPath deviceIdHash = createString("deviceIdHash");

    public final BooleanPath isActive = createBoolean("isActive");

    public final NumberPath<Integer> keySeq = createNumber("keySeq", Integer.class);

    public final StringPath publicKeyPem = createString("publicKeyPem");

    public final DateTimePath<java.time.LocalDateTime> registeredDttm = createDateTime("registeredDttm", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> revokedDttm = createDateTime("revokedDttm", java.time.LocalDateTime.class);

    public final QUser user;

    public QUserDeviceKey(String variable) {
        this(UserDeviceKey.class, forVariable(variable), INITS);
    }

    public QUserDeviceKey(Path<? extends UserDeviceKey> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserDeviceKey(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserDeviceKey(PathMetadata metadata, PathInits inits) {
        this(UserDeviceKey.class, metadata, inits);
    }

    public QUserDeviceKey(Class<? extends UserDeviceKey> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

