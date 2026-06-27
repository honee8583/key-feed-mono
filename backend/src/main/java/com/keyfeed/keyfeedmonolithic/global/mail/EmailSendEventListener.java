package com.keyfeed.keyfeedmonolithic.global.mail;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class EmailSendEventListener {

    private final EmailClient emailClient;

    @Async("emailTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(EmailSendEvent event) {
        emailClient.sendOneEmail(event.to(), event.subject(), event.html());
    }
}
