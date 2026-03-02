package com.keyfeed.keyfeedmonolithic.domain.auth.filter;

import com.keyfeed.keyfeedmonolithic.domain.auth.dto.LoginUser;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.InvalidJwtTokenException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.JwtTokenExpiredException;
import com.keyfeed.keyfeedmonolithic.domain.auth.util.AuthenticationResponseUtil;
import com.keyfeed.keyfeedmonolithic.domain.auth.util.JwtUtil;
import com.keyfeed.keyfeedmonolithic.global.message.ErrorMessage;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String JWT_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        // Authorization 헤더 없으면 다음 필터로 (permitAll 엔드포인트 허용)
        if (authHeader == null || !authHeader.startsWith(JWT_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(JWT_PREFIX.length());

        try {
            LoginUser loginUser = jwtUtil.verify(token);
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    loginUser.getId(),
                    null,
                    List.of(new SimpleGrantedAuthority(loginUser.getRole()))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.info("JwtAuthenticationFilter: [User {}, {}] authenticated.", loginUser.getId(), loginUser.getRole());
        } catch (JwtTokenExpiredException e) {
            log.warn("만료된 토큰입니다.");
            AuthenticationResponseUtil.authenticateFail(response, HttpStatus.UNAUTHORIZED, ErrorMessage.TOKEN_EXPIRED.getMessage());
            return;
        } catch (InvalidJwtTokenException e) {
            log.warn("유효하지 않은 토큰입니다.");
            AuthenticationResponseUtil.authenticateFail(response, HttpStatus.UNAUTHORIZED, ErrorMessage.INVALID_TOKEN.getMessage());
            return;
        }

        filterChain.doFilter(request, response);
    }
}
