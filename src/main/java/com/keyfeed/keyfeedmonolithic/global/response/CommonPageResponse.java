package com.keyfeed.keyfeedmonolithic.global.response;

import lombok.*;

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
}
