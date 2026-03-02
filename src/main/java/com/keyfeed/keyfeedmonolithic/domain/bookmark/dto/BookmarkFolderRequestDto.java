package com.keyfeed.keyfeedmonolithic.domain.bookmark.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BookmarkFolderRequestDto {

    @NotBlank(message = "폴더 이름은 필수입니다.")
    private String name;

    private String icon;

    private String color;

}