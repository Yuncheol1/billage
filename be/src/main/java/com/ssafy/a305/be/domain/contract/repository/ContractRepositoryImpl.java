package com.ssafy.a305.be.domain.contract.repository;

import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.ssafy.a305.be.domain.contract.entity.Contract;
import com.ssafy.a305.be.domain.user.entity.QUser;
import lombok.RequiredArgsConstructor;

import java.util.List;

import static com.ssafy.a305.be.domain.contract.entity.QContract.contract;

@RequiredArgsConstructor
public class ContractRepositoryImpl implements ContractRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Contract> searchContracts(Integer userSeq, String role, String status) {

        QUser creditorUser = new QUser("creditorUser");
        QUser debtorUser = new QUser("debtorUser");

        return queryFactory
                .selectFrom(contract).distinct()
                .join(contract.creditor, creditorUser).fetchJoin()
                .join(contract.debtor, debtorUser).fetchJoin()
                .leftJoin(contract.repaymentSchedules).fetchJoin()
                .where(
                        roleEq(userSeq, role),
                        statusEq(status))
                .orderBy(contract.contractSeq.desc())
                .fetch();
    }

    private BooleanExpression roleEq(Integer userSeq, String role) {
        if ("CREDITOR".equalsIgnoreCase(role)) {
            return contract.creditor.userSeq.eq(userSeq);
        } else if ("DEBTOR".equalsIgnoreCase(role)) {
            return contract.debtor.userSeq.eq(userSeq);
        } else {
            // ALL
            return contract.creditor.userSeq.eq(userSeq).or(contract.debtor.userSeq.eq(userSeq));
        }
    }

    private BooleanExpression statusEq(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        return contract.status.eq(status);
    }
}
