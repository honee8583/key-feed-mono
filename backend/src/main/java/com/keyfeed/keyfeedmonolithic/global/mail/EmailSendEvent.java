package com.keyfeed.keyfeedmonolithic.global.mail;

public record EmailSendEvent(String to, String subject, String html) {
}
