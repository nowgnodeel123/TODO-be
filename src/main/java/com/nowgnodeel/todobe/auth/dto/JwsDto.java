package com.nowgnodeel.todobe.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class JwsDto {
    private String accessToken;
    private String refreshToken;
}
