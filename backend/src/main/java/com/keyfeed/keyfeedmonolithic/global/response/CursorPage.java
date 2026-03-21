package com.keyfeed.keyfeedmonolithic.global.response;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
public class CursorPage<T> {
    private List<T> content;
    private Long nextCursorId;
    private boolean hasNext;

    public CursorPage(List<T> content, Long nextCursorId, boolean hasNext) {
        this.content = content;
        this.nextCursorId = nextCursorId;
        this.hasNext = hasNext;
    }
}
