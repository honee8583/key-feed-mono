package com.keyfeed.keyfeedmonolithic.domain.content.service;

import com.keyfeed.keyfeedmonolithic.domain.crawl.dto.CrawledContentDto;

public interface ContentService {

    void saveContent(CrawledContentDto dto);
}
