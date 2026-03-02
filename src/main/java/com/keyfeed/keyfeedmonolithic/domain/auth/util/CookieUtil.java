package com.keyfeed.keyfeedmonolithic.domain.auth.util;

import com.keyfeed.keyfeedmonolithic.global.auth.jwt.JwtConstants;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    public static ResponseCookie createResponseCookie(String refreshToken, Long maxAge) {
        refreshToken = refreshToken.replace(JwtConstants.TOKEN_PREFIX, "");
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(Duration.ofMillis(maxAge))
                .build();
    }

    public static void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie refreshCookie = createResponseCookie("", 0L);
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());
    }

}
