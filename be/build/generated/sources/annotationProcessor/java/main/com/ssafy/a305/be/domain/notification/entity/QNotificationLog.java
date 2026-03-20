package com.ssafy.a305.be.domain.notification.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QNotificationLog is a Querydsl query type for NotificationLog
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QNotificationLog extends EntityPathBase<NotificationLog> {

    private static final long serialVersionUID = 347310626L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QNotificationLog notificationLog = new QNotificationLog("notificationLog");

    public final com.ssafy.a305.be.domain.contract.entity.QContract contract;

    public final StringPath eventKey = createString("eventKey");

    public final StringPath fcmMessageId = createString("fcmMessageId");

    public final BooleanPath isRead = createBoolean("isRead");

    public final StringPath message = createString("message");

    public final NumberPath<Long> notificationSeq = createNumber("notificationSeq", Long.class);

    public final com.ssafy.a305.be.domain.repayment.entity.QRepaymentSchedule repaymentSchedule;

    public final DateTimePath<java.time.LocalDateTime> sentDttm = createDateTime("sentDttm", java.time.LocalDateTime.class);

    public final StringPath status = createString("status");

    public final StringPath title = createString("title");

    public final com.ssafy.a305.be.domain.user.entity.QUser user;

    public QNotificationLog(String variable) {
        this(NotificationLog.class, forVariable(variable), INITS);
    }

    public QNotificationLog(Path<? extends NotificationLog> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QNotificationLog(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QNotificationLog(PathMetadata metadata, PathInits inits) {
        this(NotificationLog.class, metadata, inits);
    }

    public QNotificationLog(Class<? extends NotificationLog> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new com.ssafy.a305.be.domain.contract.entity.QContract(forProperty("contract"), inits.get("contract")) : null;
        this.repaymentSchedule = inits.isInitialized("repaymentSchedule") ? new com.ssafy.a305.be.domain.repayment.entity.QRepaymentSchedule(forProperty("repaymentSchedule"), inits.get("repaymentSchedule")) : null;
        this.user = inits.isInitialized("user") ? new com.ssafy.a305.be.domain.user.entity.QUser(forProperty("user"), inits.get("user")) : null;
    }

}

