package com.keyfeed.keyfeedmonolithic.global.util;

import com.keyfeed.keyfeedmonolithic.global.response.CursorPage;

import java.util.List;
import java.util.function.Function;

public final class CursorPagination {

    private CursorPagination() {
        // 유틸 클래스이므로 인스턴스 생성 방지
    }

    /**
     * @param items       size + 1 개까지 조회된 결과 리스트
     * @param pageSize    한 페이지 크기
     * @param idExtractor 각 아이템에서 Long 타입 커서 ID를 추출하는 함수
     */
    public static <T> CursorPage<T> paginate(List<T> items,
                                             int pageSize,
                                             Function<T, Long> idExtractor) {
        boolean hasNext = items.size() > pageSize;

        List<T> trimmedItems = items;
        if (hasNext) {
            trimmedItems = items.subList(0, pageSize);
        }

        Long nextCursorId = null;
        if (!trimmedItems.isEmpty()) {
            T lastItem = trimmedItems.get(trimmedItems.size() - 1);
            nextCursorId = idExtractor.apply(lastItem);
        }

        return new CursorPage<>(trimmedItems, nextCursorId, hasNext);
    }
}