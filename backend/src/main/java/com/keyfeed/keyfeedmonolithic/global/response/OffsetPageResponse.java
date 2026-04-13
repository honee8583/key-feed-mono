package com.keyfeed.keyfeedmonolithic.global.response;

import lombok.*;
import org.springframework.data.domain.Page;

import java.util.Collections;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OffsetPageResponse<T> {
    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;

    public static <T> OffsetPageResponse<T> from(Page<T> page) {
        return OffsetPageResponse.<T>builder()
                .content(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    public static <T> OffsetPageResponse<T> empty() {
        return OffsetPageResponse.<T>builder()
                .content(Collections.emptyList())
                .page(0)
                .size(0)
                .totalElements(0L)
                .totalPages(0)
                .hasNext(false)
                .build();
    }
}
