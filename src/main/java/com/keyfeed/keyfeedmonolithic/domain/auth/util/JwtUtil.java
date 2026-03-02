package com.keyfeed.keyfeedmonolithic.domain.auth.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.keyfeed.keyfeedmonolithic.domain.auth.dto.LoginUser;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.Role;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.InvalidJwtTokenException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.JwtTokenExpiredException;
import com.keyfeed.keyfeedmonolithic.global.auth.jwt.JwtConstants;
import com.keyfeed.keyfeedmonolithic.global.auth.jwt.JwtProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtil {

    private final JwtProperties jwtProperties;

    public String createAccessToken(Long userId, Role role) {
        return createToken(userId, role, jwtProperties.getExpirationTime());
    }

    public String createRefreshToken(Long userId, Role role) {
        return createToken(userId, role, jwtProperties.getRefreshExpirationTime());
    }

    private String createToken(Long id, Role role, long expirationTime) {
        return JWT.create()
                .withSubject(JwtConstants.CLAIM_SUBJECT)
                .withExpiresAt(new Date(System.currentTimeMillis() + expirationTime))
                .withClaim(JwtConstants.CLAIM_ID, id)
                .withClaim(JwtConstants.CLAIM_ROLE, role.name())
                .sign(Algorithm.HMAC512(jwtProperties.getSecret()));
    }

    public LoginUser verify(String token) {
        try {
            DecodedJWT decodedJWT = JWT.require(Algorithm.HMAC512(jwtProperties.getSecret()))
                    .build()
                    .verify(token);

            return LoginUser.builder()
                    .id(decodedJWT.getClaim(JwtConstants.CLAIM_ID).asLong())
                    .role(decodedJWT.getClaim(JwtConstants.CLAIM_ROLE).asString())
                    .build();
        } catch (TokenExpiredException e) {
            throw new JwtTokenExpiredException();
        } catch (JWTVerificationException e) {
            throw new InvalidJwtTokenException();
        }
    }

}