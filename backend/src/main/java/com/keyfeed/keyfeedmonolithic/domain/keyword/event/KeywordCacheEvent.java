package com.keyfeed.keyfeedmonolithic.domain.keyword.event;

public record KeywordCacheEvent(String keywordName, Long userId, Operation operation) {

    public enum Operation { ADD, REMOVE }
}
