package com.ssafy.a305.be.domain.document.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SignatureId implements Serializable {
    private Integer signatureSeq;
    private Integer userDeviceKey;
}
