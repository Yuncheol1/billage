package com.ssafy.a305.be.domain.notification.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QPromiseReminder is a Querydsl query type for PromiseReminder
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QPromiseReminder extends EntityPathBase<PromiseReminder> {

    private static final long serialVersionUID = 757114486L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QPromiseReminder promiseReminder = new QPromiseReminder("promiseReminder");

    public final NumberPath<java.math.BigDecimal> amount = createNumber("amount", java.math.BigDecimal.class);

    public final StringPath content = createString("content");

    public final DateTimePath<java.time.LocalDateTime> createdDttm = createDateTime("createdDttm", java.time.LocalDateTime.class);

    public final com.ssafy.a305.be.domain.user.entity.QUser creator;

    public final DateTimePath<java.time.LocalDateTime> dueDttm = createDateTime("dueDttm", java.time.LocalDateTime.class);

    public final BooleanPath isCompleted = createBoolean("isCompleted");

    public final DateTimePath<java.time.LocalDateTime> remindDttm = createDateTime("remindDttm", java.time.LocalDateTime.class);

    public final NumberPath<Integer> reminderSeq = createNumber("reminderSeq", Integer.class);

    public final com.ssafy.a305.be.domain.user.entity.QUser target;

    public QPromiseReminder(String variable) {
        this(PromiseReminder.class, forVariable(variable), INITS);
    }

    public QPromiseReminder(Path<? extends PromiseReminder> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QPromiseReminder(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QPromiseReminder(PathMetadata metadata, PathInits inits) {
        this(PromiseReminder.class, metadata, inits);
    }

    public QPromiseReminder(Class<? extends PromiseReminder> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.creator = inits.isInitialized("creator") ? new com.ssafy.a305.be.domain.user.entity.QUser(forProperty("creator"), inits.get("creator")) : null;
        this.target = inits.isInitialized("target") ? new com.ssafy.a305.be.domain.user.entity.QUser(forProperty("target"), inits.get("target")) : null;
    }

}

