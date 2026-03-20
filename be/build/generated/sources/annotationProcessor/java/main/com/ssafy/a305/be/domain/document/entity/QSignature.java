package com.ssafy.a305.be.domain.document.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QSignature is a Querydsl query type for Signature
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QSignature extends EntityPathBase<Signature> {

    private static final long serialVersionUID = 104424849L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QSignature signature = new QSignature("signature");

    public final BooleanPath biometricVerified = createBoolean("biometricVerified");

    public final StringPath deviceIdHash = createString("deviceIdHash");

    public final QDocument document;

    public final StringPath ipAddress = createString("ipAddress");

    public final StringPath method = createString("method");

    public final EnumPath<com.ssafy.a305.be.domain.enums.SignatureRole> role = createEnum("role", com.ssafy.a305.be.domain.enums.SignatureRole.class);

    public final StringPath signaturePayload = createString("signaturePayload");

    public final NumberPath<Integer> signatureSeq = createNumber("signatureSeq", Integer.class);

    public final StringPath signatureValue = createString("signatureValue");

    public final DateTimePath<java.time.LocalDateTime> signedDttm = createDateTime("signedDttm", java.time.LocalDateTime.class);

    public final com.ssafy.a305.be.domain.user.entity.QUser user;

    public final StringPath userAgent = createString("userAgent");

    public final com.ssafy.a305.be.domain.user.entity.QUserDeviceKey userDeviceKey;

    public QSignature(String variable) {
        this(Signature.class, forVariable(variable), INITS);
    }

    public QSignature(Path<? extends Signature> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QSignature(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QSignature(PathMetadata metadata, PathInits inits) {
        this(Signature.class, metadata, inits);
    }

    public QSignature(Class<? extends Signature> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.document = inits.isInitialized("document") ? new QDocument(forProperty("document"), inits.get("document")) : null;
        this.user = inits.isInitialized("user") ? new com.ssafy.a305.be.domain.user.entity.QUser(forProperty("user"), inits.get("user")) : null;
        this.userDeviceKey = inits.isInitialized("userDeviceKey") ? new com.ssafy.a305.be.domain.user.entity.QUserDeviceKey(forProperty("userDeviceKey"), inits.get("userDeviceKey")) : null;
    }

}

