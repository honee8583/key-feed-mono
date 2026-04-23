package com.keyfeed.keyfeedmonolithic.domain.keyword.event;

import com.keyfeed.keyfeedmonolithic.domain.keyword.repository.KeywordCacheRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class KeywordCacheEventListener {

    private final KeywordCacheRepository keywordCacheRepository;

    @TransactionalEventListener
    public void handle(KeywordCacheEvent event) {
        switch (event.operation()) {
            case ADD -> keywordCacheRepository.addUserToKeyword(event.keywordName(), event.userId());
            case REMOVE -> keywordCacheRepository.removeUserFromKeyword(event.keywordName(), event.userId());
        }
    }
}
