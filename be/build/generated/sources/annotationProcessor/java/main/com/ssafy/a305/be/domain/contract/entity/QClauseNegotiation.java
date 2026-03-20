package com.ssafy.a305.be.domain.contract.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QClauseNegotiation is a Querydsl query type for ClauseNegotiation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QClauseNegotiation extends EntityPathBase<ClauseNegotiation> {

    private static final long serialVersionUID = 2139808436L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QClauseNegotiation clauseNegotiation = new QClauseNegotiation("clauseNegotiation");

    public final StringPath comment = createString("comment");

    public final QContractClause contractClause;

    public final DateTimePath<java.time.LocalDateTime> createdDttm = createDateTime("createdDttm", java.time.LocalDateTime.class);

    public final NumberPath<Integer> negotiationSeq = createNumber("negotiationSeq", Integer.class);

    public final StringPath proposedContent = createString("proposedContent");

    public final com.ssafy.a305.be.domain.user.entity.QUser requestedByUser;

    public final DateTimePath<java.time.LocalDateTime> resolvedDttm = createDateTime("resolvedDttm", java.time.LocalDateTime.class);

    public final StringPath status = createString("status");

    public QClauseNegotiation(String variable) {
        this(ClauseNegotiation.class, forVariable(variable), INITS);
    }

    public QClauseNegotiation(Path<? extends ClauseNegotiation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QClauseNegotiation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QClauseNegotiation(PathMetadata metadata, PathInits inits) {
        this(ClauseNegotiation.class, metadata, inits);
    }

    public QClauseNegotiation(Class<? extends ClauseNegotiation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contractClause = inits.isInitialized("contractClause") ? new QContractClause(forProperty("contractClause"), inits.get("contractClause")) : null;
        this.requestedByUser = inits.isInitialized("requestedByUser") ? new com.ssafy.a305.be.domain.user.entity.QUser(forProperty("requestedByUser"), inits.get("requestedByUser")) : null;
    }

}

