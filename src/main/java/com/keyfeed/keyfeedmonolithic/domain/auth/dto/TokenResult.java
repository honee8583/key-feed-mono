package com.keyfeed.keyfeedmonolithic.domain.auth.dto;

import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TokenResult {
    private String accessToken;
    private String refreshToken;
}
