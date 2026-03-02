//package com.keyfeed.keyfeedmonolithic.domain.auth.filter;
//
//import jakarta.servlet.FilterChain;
//import jakarta.servlet.ServletException;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.SecurityContextHolder;
//import org.springframework.stereotype.Component;
//import org.springframework.web.filter.OncePerRequestFilter;
//
//import java.io.IOException;
//import java.util.List;
//
//@Slf4j
//@Component
//@RequiredArgsConstructor
//public class GatewayHeaderAuthenticationFilter extends OncePerRequestFilter {
//
//    private static final String USER_ID_HEADER = "X-User-Id";
//    private static final String ROLE_HEADER = "X-User-Roles";
//
//    @Override
//    protected void doFilterInternal(HttpServletRequest request,
//                                    HttpServletResponse response,
//                                    FilterChain filterChain) throws ServletException, IOException {
//
//        final String userIdHeader = request.getHeader(USER_ID_HEADER);
//        final String roleHeader = request.getHeader(ROLE_HEADER);
//
//        if (userIdHeader == null || userIdHeader.isBlank() || roleHeader == null || roleHeader.isBlank()) {
//            filterChain.doFilter(request, response);
//            return;
//        }
//
//        try {
//            Long userId = Long.parseLong(userIdHeader);
//            List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(roleHeader));
//            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
//                    userId,
//                    null,
//                    authorities
//            );
//
//            SecurityContextHolder.getContext().setAuthentication(authentication);
//
//            log.info("GatewayHeaderAuthenticationFilter: [User {}, {}] authenticated.", userId, roleHeader);
//        } catch (NumberFormatException e) {
//            log.warn("Invalid X-User-Id header: {}", userIdHeader, e);
//        }
//
//        filterChain.doFilter(request, response);
//    }
//}