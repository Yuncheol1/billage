package com.ssafy.a305.be.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QUser is a Querydsl query type for User
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QUser extends EntityPathBase<User> {

    private static final long serialVersionUID = -605885054L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QUser user = new QUser("user");

    public final DateTimePath<java.time.LocalDateTime> createdDttm = createDateTime("createdDttm", java.time.LocalDateTime.class);

    public final DateTimePath<java.time.LocalDateTime> deletedDttm = createDateTime("deletedDttm", java.time.LocalDateTime.class);

    public final StringPath email = createString("email");

    public final StringPath phone = createString("phone");

    public final QUserProfile profile;

    public final StringPath state = createString("state");

    public final DateTimePath<java.time.LocalDateTime> updatedDttm = createDateTime("updatedDttm", java.time.LocalDateTime.class);

    public final NumberPath<Integer> userSeq = createNumber("userSeq", Integer.class);

    public QUser(String variable) {
        this(User.class, forVariable(variable), INITS);
    }

    public QUser(Path<? extends User> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QUser(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QUser(PathMetadata metadata, PathInits inits) {
        this(User.class, metadata, inits);
    }

    public QUser(Class<? extends User> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.profile = inits.isInitialized("profile") ? new QUserProfile(forProperty("profile"), inits.get("profile")) : null;
    }

}

