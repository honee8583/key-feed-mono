package com.keyfeed.keyfeedmonolithic.global.mail;

import com.keyfeed.keyfeedmonolithic.domain.auth.exception.EmailSendFailedException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailClient {

    private final JavaMailSender mailSender;

    public void sendOneEmail(String to, String subject, String text) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            MimeMessageHelper mimeMessageHelper = new MimeMessageHelper(mimeMessage);
            mimeMessageHelper.setTo(to);
            mimeMessageHelper.setSubject(subject);
            mimeMessageHelper.setText(text, true);

            mailSender.send(mimeMessage);
            log.info("이메일 발송 완료. to={}, subject={}", maskEmail(to), subject);

        } catch (MessagingException e){
            log.error("이메일 발송 실패. to={}, subject={}", maskEmail(to), subject, e);
            throw new EmailSendFailedException();
        }
    }

    private String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 2) {
            return "***" + email.substring(at);
        }
        return email.substring(0, 2) + "***" + email.substring(at);
    }

}
