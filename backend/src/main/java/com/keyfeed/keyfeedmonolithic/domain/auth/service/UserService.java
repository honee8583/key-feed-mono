package com.keyfeed.keyfeedmonolithic.domain.auth.service;

import com.keyfeed.keyfeedmonolithic.domain.auth.dto.PasswordChangeRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.dto.WithdrawRequestDto;

public interface UserService {

    void changePassword(Long userId, PasswordChangeRequestDto requestDto);

    void withdraw(Long userId, WithdrawRequestDto requestDto);

}
