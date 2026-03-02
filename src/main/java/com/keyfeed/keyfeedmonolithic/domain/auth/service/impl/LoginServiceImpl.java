package com.keyfeed.keyfeedmonolithic.domain.auth.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.dto.*;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.Role;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.InvalidPasswordException;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.UserRepository;
import com.keyfeed.keyfeedmonolithic.domain.auth.service.LoginService;
import com.keyfeed.keyfeedmonolithic.domain.auth.util.JwtUtil;
import com.keyfeed.keyfeedmonolithic.global.error.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class LoginServiceImpl implements LoginService {

    private final JwtUtil jwtUtil;
    private final BCryptPasswordEncoder passwordEncoder;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public LoginResult login(LoginRequestDto loginRequestDto) {
        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .orElseThrow(() -> new EntityNotFoundException("User", loginRequestDto.getEmail()));

        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        TokenResult tokens = issueTokens(user.getId(), user.getRole());
        LoginResponseDto loginResponse = LoginResponseDto.from(user, tokens.getAccessToken());
        return LoginResult.from(loginResponse, tokens.getRefreshToken());
    }

    @Override
    @Transactional(readOnly = true)
    public TokenResult reissueTokens(String refreshToken) {
        LoginUser loginUser = jwtUtil.verify(refreshToken);

        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(() -> new EntityNotFoundException("User", String.valueOf(loginUser.getId())));

        return issueTokens(user.getId(), user.getRole());
    }

    private TokenResult issueTokens(Long userId, Role role) {
        return TokenResult.builder()
                .accessToken(jwtUtil.createAccessToken(userId, role))
                .refreshToken(jwtUtil.createRefreshToken(userId, role))
                .build();
    }

}