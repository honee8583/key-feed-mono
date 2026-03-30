package com.keyfeed.keyfeedmonolithic.infra.outbox.alert;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OutboxExhaustedEvent {
    private final Long outboxId;
    private final Long contentId;
}
