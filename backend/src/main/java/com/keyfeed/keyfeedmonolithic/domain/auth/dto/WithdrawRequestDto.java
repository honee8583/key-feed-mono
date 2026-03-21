package com.keyfeed.keyfeedmonolithic.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawRequestDto {

    @NotBlank(message = "비밀번호를 입력해주세요.")
    private String password;

}
