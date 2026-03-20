package com.ssafy.a305.be.domain.contract.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QContractClause is a Querydsl query type for ContractClause
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QContractClause extends EntityPathBase<ContractClause> {

    private static final long serialVersionUID = -704502625L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QContractClause contractClause = new QContractClause("contractClause");

    public final StringPath clauseId = createString("clauseId");

    public final NumberPath<Integer> clauseSeq = createNumber("clauseSeq", Integer.class);

    public final StringPath content = createString("content");

    public final QContract contract;

    public final DateTimePath<java.time.LocalDateTime> createdDttm = createDateTime("createdDttm", java.time.LocalDateTime.class);

    public final NumberPath<Integer> orderNo = createNumber("orderNo", Integer.class);

    public final StringPath title = createString("title");

    public final DateTimePath<java.time.LocalDateTime> updatedDttm = createDateTime("updatedDttm", java.time.LocalDateTime.class);

    public QContractClause(String variable) {
        this(ContractClause.class, forVariable(variable), INITS);
    }

    public QContractClause(Path<? extends ContractClause> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QContractClause(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QContractClause(PathMetadata metadata, PathInits inits) {
        this(ContractClause.class, metadata, inits);
    }

    public QContractClause(Class<? extends ContractClause> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new QContract(forProperty("contract"), inits.get("contract")) : null;
    }

}

