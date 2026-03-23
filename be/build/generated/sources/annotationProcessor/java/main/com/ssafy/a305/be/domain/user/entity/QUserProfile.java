package com.ssafy.a305.be.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUserProfile is a Querydsl query type for UserProfile
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUserProfile extends EntityPathBase<UserProfile> {

    private static final long serialVersionUID = -376939065L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUserProfile userProfile = new QUserProfile("userProfile");

    public final NumberPath<java.math.BigDecimal> avgDelayDays = createNumber("avgDelayDays", java.math.BigDecimal.class);

    public final BooleanPath badgeIsPublic = createBoolean("badgeIsPublic");

    public final StringPath badgeLevel = createString("badgeLevel");

    public final BooleanPath lockEnabled = createBoolean("lockEnabled");

    public final StringPath lockType = createString("lockType");

    public final NumberPath<Integer> onTimeCount = createNumber("onTimeCount", Integer.class);

    public final NumberPath<Integer> overdueCount = createNumber("overdueCount", Integer.class);

    public final StringPath patternHash = createString("patternHash");

    public final BooleanPath phoneVerified = createBoolean("phoneVerified");

    public final DatePath<java.time.LocalDate> profileCompletedDt = createDate("profileCompletedDt", java.time.LocalDate.class);

    public final StringPath realName = createString("realName");

    public final NumberPath<Integer> resolvedCount = createNumber("resolvedCount", Integer.class);

    public final StringPath rrn = createString("rrn");

    public final DateTimePath<java.time.LocalDateTime> updatedDttm = createDateTime("updatedDttm", java.time.LocalDateTime.class);

    public final QUser user;

    public final NumberPath<Integer> userProfileSeq = createNumber("userProfileSeq", Integer.class);

    public QUserProfile(String variable) {
        this(UserProfile.class, forVariable(variable), INITS);
    }

    public QUserProfile(Path<? extends UserProfile> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUserProfile(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUserProfile(PathMetadata metadata, PathInits inits) {
        this(UserProfile.class, metadata, inits);
    }

    public QUserProfile(Class<? extends UserProfile> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.user = inits.isInitialized("user") ? new QUser(forProperty("user"), inits.get("user")) : null;
    }

}

