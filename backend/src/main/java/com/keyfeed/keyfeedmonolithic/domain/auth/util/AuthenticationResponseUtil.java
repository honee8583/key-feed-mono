package com.keyfeed.keyfeedmonolithic.domain.auth.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.keyfeed.keyfeedmonolithic.global.response.HttpResponse;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class AuthenticationResponseUtil {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static void authenticateFail(HttpServletResponse response,
                                        HttpStatus httpStatus,
                                        String message) throws IOException, ServletException {
        HttpResponse httpResponse = new HttpResponse(httpStatus, message, null);
        String responseData = objectMapper.writeValueAsString(httpResponse);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(responseData);
    }

}
