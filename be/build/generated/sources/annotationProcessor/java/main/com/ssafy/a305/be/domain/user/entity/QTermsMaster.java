package com.ssafy.a305.be.domain.user.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;


/**
 * QTermsMaster is a Querydsl query type for TermsMaster
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTermsMaster extends EntityPathBase<TermsMaster> {

    private static final long serialVersionUID = 1951060274L;

    public static final QTermsMaster termsMaster = new QTermsMaster("termsMaster");

    public final DateTimePath<java.time.LocalDateTime> createdDttm = createDateTime("createdDttm", java.time.LocalDateTime.class);

    public final StringPath termsContent = createString("termsContent");

    public final StringPath termsKey = createString("termsKey");

    public final NumberPath<Integer> termsSeq = createNumber("termsSeq", Integer.class);

    public final EnumPath<com.ssafy.a305.be.domain.enums.TermsType> termsType = createEnum("termsType", com.ssafy.a305.be.domain.enums.TermsType.class);

    public final StringPath termsVersion = createString("termsVersion");

    public QTermsMaster(String variable) {
        super(TermsMaster.class, forVariable(variable));
    }

    public QTermsMaster(Path<? extends TermsMaster> path) {
        super(path.getType(), path.getMetadata());
    }

    public QTermsMaster(PathMetadata metadata) {
        super(TermsMaster.class, metadata);
    }

}

