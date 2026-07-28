package com.keyfeed.keyfeedmonolithic.domain.search.service;

import java.util.List;

public interface RecentSearchService {

    void record(Long userId, String keyword);

    List<String> getRecentSearches(Long userId);

    void delete(Long userId, String keyword);

    void deleteAll(Long userId);

}
