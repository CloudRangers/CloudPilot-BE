package com.cloudrangers.cloudpilot.dto.common;

import lombok.*;
import java.util.Collections;
import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class PageResponse<T> {

    private List<T> items;
    private int page;
    private int size;
    private long total;
    private boolean hasNext;

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long total) {
        boolean hasNext = (long)(page + 1) * size < total;
        return PageResponse.<T>builder()
                .items(items)
                .page(page)
                .size(size)
                .total(total)
                .hasNext(hasNext)
                .build();
    }

    /**
     * ✅ 조회 실패 / 예외 발생 시 사용할 빈 페이지 헬퍼
     */
    public static <T> PageResponse<T> empty(int page, int size) {
        return PageResponse.<T>builder()
                .items(Collections.emptyList())
                .page(page)
                .size(size)
                .total(0L)
                .hasNext(false)
                .build();
    }
}
