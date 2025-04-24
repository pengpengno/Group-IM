package com.github.im.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> content;
    private PageMeta page;

    @Data
    public static class PageMeta {
        private int size;
        private int number;
        private long totalElements;
        private int totalPages;
    }
}
