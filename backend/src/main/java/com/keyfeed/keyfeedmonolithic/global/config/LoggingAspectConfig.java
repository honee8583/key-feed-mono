package com.keyfeed.keyfeedmonolithic.global.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Slf4j
@Aspect
@Component
public class LoggingAspectConfig {

    @Pointcut("within(@org.springframework.web.bind.annotation.RestController *)")
    public void controllerLayer() {}

    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceLayerByAnnotation() {}

    @Around("controllerLayer()")
    public Object logController(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.nanoTime();
        String sig = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.info("➡️ Controller enter {} args={}", sig, Arrays.toString(args));
        try {
            Object ret = joinPoint.proceed();
            log.info("⬅️ Controller exit  {} result={} ({} ms)",
                    sig, shrink(ret), (System.nanoTime() - start) / 1_000_000);
            return ret;
        } catch (Throwable t) {
            log.error("💥 Controller error {} msg={}", sig, t.getMessage(), t);
            throw t;
        }
    }

    private String shrink(Object o) {
        if (o == null) return "null";
        String s = String.valueOf(o);
        return s.length() > 300 ? s.substring(0, 300) + "...(trunc)" : s;
    }
}
