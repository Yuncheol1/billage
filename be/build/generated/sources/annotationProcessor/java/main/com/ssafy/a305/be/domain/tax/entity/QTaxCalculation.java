package com.ssafy.a305.be.domain.tax.entity;

import static com.querydsl.core.types.PathMetadataFactory.*;

import com.querydsl.core.types.dsl.*;

import com.querydsl.core.types.PathMetadata;
import javax.annotation.processing.Generated;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.dsl.PathInits;


/**
 * QTaxCalculation is a Querydsl query type for TaxCalculation
 */
@Generated("com.querydsl.codegen.DefaultEntitySerializer")
public class QTaxCalculation extends EntityPathBase<TaxCalculation> {

    private static final long serialVersionUID = 1018251895L;

    private static final PathInits INITS = PathInits.DIRECT2;

    public static final QTaxCalculation taxCalculation = new QTaxCalculation("taxCalculation");

    public final StringPath calculatedYmd = createString("calculatedYmd");

    public final com.ssafy.a305.be.domain.contract.entity.QContract contract;

    public final NumberPath<java.math.BigDecimal> cumulativeTax = createNumber("cumulativeTax", java.math.BigDecimal.class);

    public final NumberPath<java.math.BigDecimal> interestIncome = createNumber("interestIncome", java.math.BigDecimal.class);

    public final com.ssafy.a305.be.domain.repayment.entity.QRepaymentEvent repaymentEvent;

    public final NumberPath<Integer> taxSeq = createNumber("taxSeq", Integer.class);

    public final NumberPath<java.math.BigDecimal> withholdingTax = createNumber("withholdingTax", java.math.BigDecimal.class);

    public QTaxCalculation(String variable) {
        this(TaxCalculation.class, forVariable(variable), INITS);
    }

    public QTaxCalculation(Path<? extends TaxCalculation> path) {
        this(path.getType(), path.getMetadata(), PathInits.getFor(path.getMetadata(), INITS));
    }

    public QTaxCalculation(PathMetadata metadata) {
        this(metadata, PathInits.getFor(metadata, INITS));
    }

    public QTaxCalculation(PathMetadata metadata, PathInits inits) {
        this(TaxCalculation.class, metadata, inits);
    }

    public QTaxCalculation(Class<? extends TaxCalculation> type, PathMetadata metadata, PathInits inits) {
        super(type, metadata, inits);
        this.contract = inits.isInitialized("contract") ? new com.ssafy.a305.be.domain.contract.entity.QContract(forProperty("contract"), inits.get("contract")) : null;
        this.repaymentEvent = inits.isInitialized("repaymentEvent") ? new com.ssafy.a305.be.domain.repayment.entity.QRepaymentEvent(forProperty("repaymentEvent"), inits.get("repaymentEvent")) : null;
    }

}

