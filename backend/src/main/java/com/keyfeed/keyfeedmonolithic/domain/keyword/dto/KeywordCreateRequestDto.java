package com.keyfeed.keyfeedmonolithic.domain.keyword.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@Builder
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class KeywordCreateRequestDto {

    @NotBlank(message = "키워드는 공백일 수 없습니다.")
    @Size(min = 2, max = 25, message = "키워드는 2자 이상 25자 이하여야 합니다.")
    private String name;

}
