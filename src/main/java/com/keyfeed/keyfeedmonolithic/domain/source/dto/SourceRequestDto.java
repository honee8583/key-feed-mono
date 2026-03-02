package com.keyfeed.keyfeedmonolithic.domain.source.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SourceRequestDto {

    @NotBlank(message = "소스 이름은 필수 항목입니다.")
    private String name;

    @NotBlank(message = "URL은 필수 항목입니다.")
    @Pattern(regexp = "^(http|https)://.*", message = "URL 형식이 올바르지 않습니다.")
    private String url;

    @Builder.Default
    private Boolean receiveFeed = true;

}
