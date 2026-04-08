package com.keyfeed.keyfeedmonolithic.global.response;

import lombok.*;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CommonPageResponse<T> {
    private List<T> content;
    private Long nextCursorId;
    private boolean hasNext;

    public static <T> CommonPageResponse<T> empty() {
        return CommonPageResponse.<T>builder()
                .content(Collections.emptyList())
                .hasNext(false)
                .build();
    }
}
