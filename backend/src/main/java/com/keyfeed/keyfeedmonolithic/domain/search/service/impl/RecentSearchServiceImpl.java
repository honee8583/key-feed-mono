package com.keyfeed.keyfeedmonolithic.domain.search.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.search.repository.RecentSearchRedisRepository;
import com.keyfeed.keyfeedmonolithic.domain.search.service.RecentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecentSearchServiceImpl implements RecentSearchService {

    private final RecentSearchRedisRepository recentSearchRedisRepository;

    @Override
    public void record(Long userId, String keyword) {
        if (userId == null || !StringUtils.hasText(keyword)) {
            return;
        }
        recentSearchRedisRepository.add(userId, keyword.trim());
    }

    @Override
    public List<String> getRecentSearches(Long userId) {
        return recentSearchRedisRepository.findAll(userId);
    }

    @Override
    public void delete(Long userId, String keyword) {
        recentSearchRedisRepository.remove(userId, keyword);
    }

    @Override
    public void deleteAll(Long userId) {
        recentSearchRedisRepository.removeAll(userId);
    }
}
