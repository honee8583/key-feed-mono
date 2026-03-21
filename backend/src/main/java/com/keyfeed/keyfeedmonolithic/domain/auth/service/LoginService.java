package com.keyfeed.keyfeedmonolithic.domain.auth.service;

import com.keyfeed.keyfeedmonolithic.domain.auth.dto.LoginRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.dto.LoginResult;
import com.keyfeed.keyfeedmonolithic.domain.auth.dto.TokenResult;

public interface LoginService {

    LoginResult login(LoginRequestDto loginRequestDto);

    TokenResult reissueTokens(String refreshToken);

}
