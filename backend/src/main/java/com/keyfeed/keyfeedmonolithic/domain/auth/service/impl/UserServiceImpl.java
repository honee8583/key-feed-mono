package com.keyfeed.keyfeedmonolithic.domain.auth.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.dto.PasswordChangeRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.dto.WithdrawRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.InvalidPasswordException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.PasswordMismatchException;
import com.keyfeed.keyfeedmonolithic.domain.auth.exception.SamePasswordException;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.UserRepository;
import com.keyfeed.keyfeedmonolithic.domain.auth.service.UserService;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.repository.BookmarkFolderRepository;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.repository.BookmarkRepository;
import com.keyfeed.keyfeedmonolithic.domain.keyword.repository.KeywordRepository;
import com.keyfeed.keyfeedmonolithic.domain.source.repository.UserSourceRepository;
import com.keyfeed.keyfeedmonolithic.global.error.exception.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final KeywordRepository keywordRepository;
    private final UserSourceRepository userSourceRepository;
    private final BookmarkFolderRepository bookmarkFolderRepository;
    private final BookmarkRepository bookmarkRepository;

    @Override
    public void changePassword(Long userId, PasswordChangeRequestDto requestDto) {
        User user = resolveUser(userId);

        if (!passwordEncoder.matches(requestDto.getCurrentPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        if (!requestDto.getNewPassword().equals(requestDto.getConfirmPassword())) {
            throw new PasswordMismatchException();
        }

        if (passwordEncoder.matches(requestDto.getNewPassword(), user.getPassword())) {
            throw new SamePasswordException();
        }

        user.updatePassword(passwordEncoder.encode(requestDto.getNewPassword()));
    }

    @Override
    public void withdraw(Long userId, WithdrawRequestDto requestDto) {
        User user = resolveUser(userId);

        if (!passwordEncoder.matches(requestDto.getPassword(), user.getPassword())) {
            throw new InvalidPasswordException();
        }

        bookmarkRepository.deleteAllByUserId(userId);
        bookmarkFolderRepository.deleteAllByUserId(userId);
        userSourceRepository.deleteAllByUserId(userId);
        keywordRepository.deleteAllByUserId(userId);

        userRepository.delete(user);
    }

    private User resolveUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User", String.valueOf(userId)));
    }

}
