package com.keyfeed.keyfeedmonolithic.domain.auth.filter;

import com.keyfeed.keyfeedmonolithic.domain.auth.util.AuthenticationResponseUtil;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        AuthenticationResponseUtil.authenticateFail(response, HttpStatus.FORBIDDEN, ErrorMessage.FORBIDDEN.getMessage());
    }

}
