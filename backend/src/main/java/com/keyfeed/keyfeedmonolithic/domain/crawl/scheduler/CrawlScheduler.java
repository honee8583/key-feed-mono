package com.keyfeed.keyfeedmonolithic.domain.crawl.scheduler;

import com.keyfeed.keyfeedmonolithic.domain.crawl.service.CrawlService;
import com.keyfeed.keyfeedmonolithic.domain.source.entity.Source;
import com.keyfeed.keyfeedmonolithic.domain.source.repository.SourceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlScheduler {

    private final SourceRepository sourceRepository;
    private final CrawlService crawlService;

    // 30분마다 실행
    @Scheduled(fixedDelay = 1800000)
    public void scheduleCrawling() {
        StopWatch stopWatch = new StopWatch();

        log.info("=== 스케줄링 크롤링 작업 시작 ===");

        // 10분 이상 수집되지 않은 소스 찾기
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        List<Source> sources = sourceRepository.findSourcesToCrawl(tenMinutesAgo);

        log.info("크롤링 대상 소스: {}개", sources.size());

        stopWatch.start();

        ExecutorService executor = Executors.newFixedThreadPool(20);
        try {
            for (Source source : sources) {
                // 각 소스 처리를 스레드 풀에 제출 (비동기 병렬 실행)
                executor.submit(() -> {
                    try {
                        crawlService.processSource(source);
                    } catch (Exception e) {
                        log.error("소스 크롤링 실패 (소스 ID: {}, URL: {}) ERROR : {}", source.getId(), source.getUrl(), e.getMessage());
                    }
                });
            }
        } finally {
            // 더 이상 새로운 작업을 받지 않도록 종료 요청
            executor.shutdown();
        }

        try {
            // 최대 10분까지(다음 스케쥴링 주기) submit()으로 던져놓은 모든 크롤링 작업들이 끝날때까지 메인 스레드를 정지시키고 기다리는 역할
            if (!executor.awaitTermination(30, TimeUnit.MINUTES)) {
                log.warn("크롤링 작업이 시간 내에 완료되지 않아 강제 종료합니다.");
                executor.shutdownNow(); // 10분을 기다렸는데도 작업이 안끝났을 때 모든 스레드에게 정지 요청
            }
        } catch (InterruptedException e) {
            log.error("크롤링 작업 대기 중 인터럽트 발생", e);
            executor.shutdownNow();
            Thread.currentThread().interrupt(); // 인터럽트 상태 복구
        }

        stopWatch.stop();

        log.info("크롤링 완료: {}", stopWatch.shortSummary());
        log.info("상세 수행 시간:\n{}", stopWatch.prettyPrint());

        log.info("=== 스케줄링 크롤링 작업 종료 ===");
    }
}