package com.keyfeed.keyfeedmonolithic.domain.bookmark.service.impl;

import com.keyfeed.keyfeedmonolithic.domain.auth.entity.User;
import com.keyfeed.keyfeedmonolithic.domain.auth.repository.UserRepository;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.dto.BookmarkFolderRequestDto;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.entity.BookmarkFolder;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.exception.FolderLimitExceededException;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.repository.BookmarkFolderRepository;
import com.keyfeed.keyfeedmonolithic.domain.bookmark.repository.BookmarkRepository;
import com.keyfeed.keyfeedmonolithic.domain.content.repository.ContentRepository;
import com.keyfeed.keyfeedmonolithic.domain.payment.entity.SubscriptionStatus;
import com.keyfeed.keyfeedmonolithic.domain.payment.repository.SubscriptionRepository;
import com.keyfeed.keyfeedmonolithic.global.error.exception.EntityAlreadyExistsException;
import com.keyfeed.keyfeedmonolithic.global.error.exception.EntityNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class BookmarkServiceImplTest {

    @InjectMocks
    private BookmarkServiceImpl bookmarkService;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private BookmarkFolderRepository folderRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    private static final int FOLDER_MAX_COUNT = 3;
    private static final int FOLDER_SUBSCRIBER_MAX_COUNT = 20;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(bookmarkService, "folderMaxCount", FOLDER_MAX_COUNT);
        ReflectionTestUtils.setField(bookmarkService, "folderSubscriberMaxCount", FOLDER_SUBSCRIBER_MAX_COUNT);
    }

    private User makeUser(Long id) {
        return User.builder()
                .email("test@test.com")
                .username("testUser")
                .build();
    }

    private BookmarkFolderRequestDto makeRequest(String name) {
        return BookmarkFolderRequestDto.builder()
                .name(name)
                .icon("📁")
                .color("#FFFFFF")
                .build();
    }

    private BookmarkFolder makeFolder(User user, String name) {
        return BookmarkFolder.builder()
                .user(user)
                .name(name)
                .icon("📁")
                .color("#FFFFFF")
                .build();
    }

    @Test
    @DisplayName("비구독자 - 한도 미만이면 폴더 생성 성공")
    void 비구독자_한도_미만_폴더_생성_성공() {
        // given
        Long userId = 1L;
        User user = makeUser(userId);
        BookmarkFolderRequestDto request = makeRequest("개발");
        BookmarkFolder folder = makeFolder(user, "개발");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(folderRepository.existsByUserIdAndName(userId, "개발")).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(false);
        given(folderRepository.countByUserId(userId)).willReturn(2L);
        given(folderRepository.save(any(BookmarkFolder.class))).willReturn(folder);

        // when
        bookmarkService.createFolder(userId, request);

        // then
        then(folderRepository).should(times(1)).save(any(BookmarkFolder.class));
    }

    @Test
    @DisplayName("비구독자 - 한도(3개)에 도달하면 FolderLimitExceededException 발생")
    void 비구독자_한도_도달_예외_발생() {
        // given
        Long userId = 2L;
        User user = makeUser(userId);
        BookmarkFolderRequestDto request = makeRequest("알고리즘");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(folderRepository.existsByUserIdAndName(userId, "알고리즘")).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(false);
        given(folderRepository.countByUserId(userId)).willReturn((long) FOLDER_MAX_COUNT);

        // when & then
        assertThatThrownBy(() -> bookmarkService.createFolder(userId, request))
                .isInstanceOf(FolderLimitExceededException.class)
                .hasMessageContaining("구독 시 최대 20개까지 이용하실 수 있습니다");

        then(folderRepository).should(never()).save(any(BookmarkFolder.class));
    }

    @Test
    @DisplayName("비구독자 - 한도(3개) 초과 시 FolderLimitExceededException 발생")
    void 비구독자_한도_초과_예외_발생() {
        // given
        Long userId = 3L;
        User user = makeUser(userId);
        BookmarkFolderRequestDto request = makeRequest("스터디");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(folderRepository.existsByUserIdAndName(userId, "스터디")).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(false);
        given(folderRepository.countByUserId(userId)).willReturn(5L);

        // when & then
        assertThatThrownBy(() -> bookmarkService.createFolder(userId, request))
                .isInstanceOf(FolderLimitExceededException.class);

        then(folderRepository).should(never()).save(any(BookmarkFolder.class));
    }

    @Test
    @DisplayName("비구독자 - 한도보다 1개 적을 때 폴더 생성 성공")
    void 비구독자_한도보다_1개_적을때_성공() {
        // given
        Long userId = 4L;
        User user = makeUser(userId);
        BookmarkFolderRequestDto request = makeRequest("독서");
        BookmarkFolder folder = makeFolder(user, "독서");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(folderRepository.existsByUserIdAndName(userId, "독서")).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(false);
        given(folderRepository.countByUserId(userId)).willReturn((long) FOLDER_MAX_COUNT - 1);
        given(folderRepository.save(any(BookmarkFolder.class))).willReturn(folder);

        // when
        bookmarkService.createFolder(userId, request);

        // then
        then(folderRepository).should(times(1)).save(any(BookmarkFolder.class));
    }

    @Test
    @DisplayName("구독자(ACTIVE) - 비구독 한도(3개) 초과해도 폴더 생성 성공")
    void 구독자_비구독_한도_초과해도_폴더_생성_성공() {
        // given
        Long userId = 5L;
        User user = makeUser(userId);
        BookmarkFolderRequestDto request = makeRequest("프로젝트");
        BookmarkFolder folder = makeFolder(user, "프로젝트");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(folderRepository.existsByUserIdAndName(userId, "프로젝트")).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(true);
        given(folderRepository.countByUserId(userId)).willReturn(10L);
        given(folderRepository.save(any(BookmarkFolder.class))).willReturn(folder);

        // when
        bookmarkService.createFolder(userId, request);

        // then
        then(folderRepository).should(times(1)).save(any(BookmarkFolder.class));
    }

    @Test
    @DisplayName("구독자(ACTIVE) - 구독자 한도(20개)에 도달하면 구독자용 메시지로 예외 발생")
    void 구독자_한도_도달_예외_발생() {
        // given
        Long userId = 6L;
        User user = makeUser(userId);
        BookmarkFolderRequestDto request = makeRequest("사이드프로젝트");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(folderRepository.existsByUserIdAndName(userId, "사이드프로젝트")).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(true);
        given(folderRepository.countByUserId(userId)).willReturn((long) FOLDER_SUBSCRIBER_MAX_COUNT);

        // when & then
        assertThatThrownBy(() -> bookmarkService.createFolder(userId, request))
                .isInstanceOf(FolderLimitExceededException.class)
                .hasMessageContaining("북마크 폴더 생성 한도(20개)에 도달했습니다");

        then(folderRepository).should(never()).save(any(BookmarkFolder.class));
    }

    @Test
    @DisplayName("구독자(ACTIVE) - 구독자 한도(20개) 미만이면 폴더 생성 성공")
    void 구독자_한도_미만_폴더_생성_성공() {
        // given
        Long userId = 7L;
        User user = makeUser(userId);
        BookmarkFolderRequestDto request = makeRequest("취미");
        BookmarkFolder folder = makeFolder(user, "취미");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(folderRepository.existsByUserIdAndName(userId, "취미")).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(true);
        given(folderRepository.countByUserId(userId)).willReturn(19L);
        given(folderRepository.save(any(BookmarkFolder.class))).willReturn(folder);

        // when
        bookmarkService.createFolder(userId, request);

        // then
        then(folderRepository).should(times(1)).save(any(BookmarkFolder.class));
    }

    @Test
    @DisplayName("구독자(CANCELED, 만료 전) - 구독 한도 내에서 폴더 생성 성공")
    void 구독취소_만료전_한도_내_폴더_생성_성공() {
        // given
        Long userId = 8L;
        User user = makeUser(userId);
        BookmarkFolderRequestDto request = makeRequest("아이디어");
        BookmarkFolder folder = makeFolder(user, "아이디어");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(folderRepository.existsByUserIdAndName(userId, "아이디어")).willReturn(false);
        given(subscriptionRepository.existsByUserIdAndStatusIn(userId, List.of(SubscriptionStatus.ACTIVE, SubscriptionStatus.CANCELED))).willReturn(true);
        given(folderRepository.countByUserId(userId)).willReturn(5L);
        given(folderRepository.save(any(BookmarkFolder.class))).willReturn(folder);

        // when
        bookmarkService.createFolder(userId, request);

        // then
        then(folderRepository).should(times(1)).save(any(BookmarkFolder.class));
    }

    @Test
    @DisplayName("중복 폴더 이름으로 생성 시 EntityAlreadyExistsException 발생")
    void 중복_폴더명_예외_발생() {
        // given
        Long userId = 9L;
        User user = makeUser(userId);
        BookmarkFolderRequestDto request = makeRequest("중복폴더");

        given(userRepository.findById(userId)).willReturn(Optional.of(user));
        given(folderRepository.existsByUserIdAndName(userId, "중복폴더")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> bookmarkService.createFolder(userId, request))
                .isInstanceOf(EntityAlreadyExistsException.class);

        then(subscriptionRepository).should(never()).existsByUserIdAndStatusIn(any(), any());
        then(folderRepository).should(never()).save(any(BookmarkFolder.class));
    }

    @Test
    @DisplayName("존재하지 않는 사용자로 폴더 생성 시 EntityNotFoundException 발생")
    void 존재하지_않는_사용자_예외_발생() {
        // given
        Long userId = 999L;
        BookmarkFolderRequestDto request = makeRequest("테스트폴더");

        given(userRepository.findById(userId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> bookmarkService.createFolder(userId, request))
                .isInstanceOf(EntityNotFoundException.class);

        then(folderRepository).should(never()).save(any(BookmarkFolder.class));
    }
}
