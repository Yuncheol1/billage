package com.ssafy.a305.be.domain.document.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QDocument is a Querydsl query type for Document
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QDocument extends EntityPathBase<Document> {

    private static final long serialVersionUID = 276268322L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QDocument document = new QDocument("document");

    public final com.ssafy.a305.be.domain.contract.entity.QContract contract;

    public final StringPath docContentJson = createString("docContentJson");

    public final StringPath docType = createString("docType");

    public final NumberPath<Integer> documentSeq = createNumber("documentSeq", Integer.class);

    public final StringPath fileUrl = createString("fileUrl");

    public final StringPath generatedBy = createString("generatedBy");

    public final DateTimePath<java.time.LocalDateTime> generatedDttm = createDateTime("generatedDttm", java.time.LocalDateTime.class);

    public final StringPath sha256Hash = createString("sha256Hash");

    public final NumberPath<Integer> version = createNumber("version", Integer.class);

    public QDocument(String variable) {
        this(Document.class, forVariable(variable), INITS);
    }

    public QDocument(Path<? extends Document> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QDocument(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QDocument(PathMetadata metadata, PathInits inits) {
        this(Document.class, metadata, inits);
    }

    public QDocument(Class<? extends Document> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new com.ssafy.a305.be.domain.contract.entity.QContract(forProperty("contract"), inits.get("contract")) : null;
    }

}

