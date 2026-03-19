package com.ssafy.a305.be.domain.contract.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ClauseRequestDto {

    @NotBlank(message = "특약 내용은 필수입니다.")
    private String clauseContent;
}
