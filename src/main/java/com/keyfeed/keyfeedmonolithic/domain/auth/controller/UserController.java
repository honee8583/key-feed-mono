package com.keyfeed.keyfeedmonolithic.domain.auth.controller;

import com.keyfeed.keyfeedmonolithic.domain.auth.dto.PasswordChangeRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.dto.WithdrawRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.service.UserService;
import com.keyfeed.keyfeedmonolithic.global.message.SuccessMessage;
import com.keyfeed.keyfeedmonolithic.global.response.HttpResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PatchMapping("/password")
    public ResponseEntity<?> changePassword(@AuthenticationPrincipal Long userId,
                                            @Valid @RequestBody PasswordChangeRequestDto requestDto) {
        userService.changePassword(userId, requestDto);
        return ResponseEntity.ok()
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.PASSWORD_CHANGE_SUCCESS.getMessage(), null));
    }

    @DeleteMapping
    public ResponseEntity<?> withdraw(@AuthenticationPrincipal Long userId,
                                      @Valid @RequestBody WithdrawRequestDto requestDto) {
        userService.withdraw(userId, requestDto);
        return ResponseEntity.ok()
                .body(new HttpResponse(HttpStatus.OK, SuccessMessage.WITHDRAW_SUCCESS.getMessage(), null));
    }

}
