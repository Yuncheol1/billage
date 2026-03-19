package com.ssafy.a305.be.domain.contract.repository;

import com.ssafy.a305.be.domain.contract.entity.Contract;
import java.util.List;

public interface ContractRepositoryCustom {
    List<Contract> searchContracts(Integer userSeq, String role, String status);
}
