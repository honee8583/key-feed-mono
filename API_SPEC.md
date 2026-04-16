# Key-Feed API 명세서

> Base URL: `http://localhost:8080`
> Content-Type: `application/json`

---

## 공통 응답 형식

모든 API는 아래 구조로 응답합니다.

```json
{
  "status": 200,
  "message": "요청이 성공적으로 처리되었습니다.",
  "data": { }
}
```

### 커서 페이지네이션 응답 (`data` 필드)

```json
{
  "content": [ ],
  "nextCursorId": 123,
  "hasNext": true
}
```

### 에러 응답

```json
{
  "status": 400,
  "message": "에러 메시지",
  "data": null
}
```

### Validation 에러 응답 (`data` 필드)

```json
{
  "fieldName": "에러 메시지"
}
```

---

## 인증 방식

| 구분 | 방식 |
|------|------|
| Access Token | 응답 바디 `data.accessToken` — 이후 요청 시 `Authorization: Bearer <token>` 헤더로 전달 |
| Refresh Token | HTTP-only Cookie (`refreshToken`) |

---

## 1. 인증 (Auth)

> Base Path: `/api/auth` — **인증 불필요**

---

### 1-1. 회원가입

```
POST /api/auth/join
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | String | O | 이메일 |
| `password` | String | O | 비밀번호 |
| `name` | String | O | 사용자 이름 |

```json
{
  "email": "user@example.com",
  "password": "password123",
  "name": "홍길동"
}
```

**동작 설명**

- 회원 저장 완료 후 이메일 인증 코드를 자동으로 발송합니다.
- 메일 발송에 실패하더라도 회원 저장은 유지되며 201을 반환합니다. 이 경우 `POST /api/auth/email-verification/request`로 재요청할 수 있습니다.
- 동일 이메일이 이미 존재하는 경우:
  - **인증 미완료 상태** → 기존 계정을 삭제하고 재가입을 허용합니다. 새 인증 코드가 발송됩니다.
  - **인증 완료 상태** → `409 Conflict`를 반환합니다.

**Response** `201 Created`

```json
{
  "status": 201,
  "message": "작성되었습니다.",
  "data": null
}
```

**Error Response** `409 Conflict`

```json
{
  "status": 409,
  "message": "이미 존재하는 사용자입니다.",
  "data": null
}
```

---

### 1-2. 로그인

```
POST /api/auth/login
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | String | O | 이메일 |
| `password` | String | O | 비밀번호 |

```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response** `200 OK`

> `Set-Cookie: refreshToken=<token>; HttpOnly` 헤더 포함

```json
{
  "status": 200,
  "message": "로그인에 성공하였습니다.",
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "role": "USER",
    "accessToken": "<jwt_access_token>"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | 사용자 ID |
| `email` | String | 이메일 |
| `name` | String | 사용자 이름 |
| `role` | String | 권한 (`USER` \| `ADMIN`) |
| `accessToken` | String | JWT 액세스 토큰 |

---

### 1-3. 토큰 재발급

```
POST /api/auth/refresh
```

> Cookie: `refreshToken=<token>` 필요

**Response** `200 OK`

> `Set-Cookie: refreshToken=<new_token>; HttpOnly` 헤더 포함

```json
{
  "status": 200,
  "message": "토큰이 발급되었습니다.",
  "data": "<new_jwt_access_token>"
}
```

---

### 1-4. 이메일 인증 코드 발송 (회원가입용)

```
POST /api/auth/email-verification/request
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | String | O | 인증 코드를 받을 이메일 |

```json
{
  "email": "user@example.com"
}
```

**Response** `201 Created`

```json
{
  "status": 201,
  "message": "이메일이 성공적으로 전송되었습니다.",
  "data": null
}
```

---

### 1-5. 이메일 인증 코드 확인 (회원가입용)

```
POST /api/auth/email-verification/confirm
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | String | O | 이메일 |
| `code` | String | O | 인증 코드 |

```json
{
  "email": "user@example.com",
  "code": "123456"
}
```

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "이메일 인증이 완료되었습니다.",
  "data": {
    "status": "VERIFIED",
    "attempts": 1,
    "retryAt": null,
    "expiresAt": "2026-03-01T12:30:00"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `status` | String | 인증 상태 (`PENDING` \| `VERIFIED` \| `EXPIRED` \| `LOCKED`) |
| `attempts` | int | 시도 횟수 |
| `retryAt` | LocalDateTime | 잠금 해제 시각 (잠금 시에만 존재) |
| `expiresAt` | LocalDateTime | 코드 만료 시각 |

---

### 1-6. 비밀번호 재설정 이메일 발송

```
POST /api/auth/password-reset/request
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | String | O | 가입된 이메일 |

```json
{
  "email": "user@example.com"
}
```

**Response** `201 Created`

```json
{
  "status": 201,
  "message": "비밀번호 재설정 이메일이 발송되었습니다.",
  "data": null
}
```

---

### 1-7. 비밀번호 재설정 코드 확인

```
POST /api/auth/password-reset/verify
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | String | O | 이메일 |
| `code` | String | O | 인증 코드 |

```json
{
  "email": "user@example.com",
  "code": "654321"
}
```

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "이메일 인증이 완료되었습니다.",
  "data": {
    "status": "VERIFIED",
    "attempts": 1,
    "retryAt": null,
    "expiresAt": "2026-03-01T12:30:00"
  }
}
```

---

### 1-8. 비밀번호 재설정 확정

```
POST /api/auth/password-reset/confirm
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `email` | String | O | 이메일 |
| `newPassword` | String | O | 새 비밀번호 |
| `confirmPassword` | String | O | 새 비밀번호 확인 |

```json
{
  "email": "user@example.com",
  "newPassword": "newPassword123",
  "confirmPassword": "newPassword123"
}
```

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "비밀번호가 성공적으로 재설정되었습니다.",
  "data": null
}
```

---

## 2. 사용자 (Users)

> Base Path: `/api/users` — **인증 필요** (`Authorization: Bearer <token>`)

---

### 2-1. 비밀번호 변경

```
PATCH /api/users/password
```

**Request Body**

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `currentPassword` | String | O | - | 현재 비밀번호 |
| `newPassword` | String | O | 8~20자 | 새 비밀번호 |
| `confirmPassword` | String | O | - | 새 비밀번호 확인 |

```json
{
  "currentPassword": "oldPassword123",
  "newPassword": "newPassword456",
  "confirmPassword": "newPassword456"
}
```

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "비밀번호가 변경되었습니다.",
  "data": null
}
```

---

### 2-2. 회원 탈퇴

```
DELETE /api/users
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `password` | String | O | 현재 비밀번호 |

```json
{
  "password": "myPassword123"
}
```

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "회원 탈퇴가 완료되었습니다.",
  "data": null
}
```

---

## 3. 북마크 (Bookmarks)

> Base Path: `/api/bookmarks` — **인증 필요**

---

### 3-1. 북마크 폴더 생성

```
POST /api/bookmarks/folders
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `name` | String | O | 폴더 이름 |
| `icon` | String | X | 폴더 아이콘 |
| `color` | String | X | 폴더 색상 |

```json
{
  "name": "개발 아티클",
  "icon": "📂",
  "color": "#3B82F6"
}
```

**Response** `201 Created`

```json
{
  "status": 201,
  "message": "작성되었습니다.",
  "data": 1
}
```

> `data`: 생성된 폴더 ID (Long)

---

### 3-2. 북마크 폴더 수정

```
PATCH /api/bookmarks/folders/{folderId}
```

**Path Variable**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `folderId` | Long | 수정할 폴더 ID |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `name` | String | O | 폴더 이름 |
| `icon` | String | X | 폴더 아이콘 |
| `color` | String | X | 폴더 색상 |

```json
{
  "name": "수정된 폴더명",
  "icon": "📁",
  "color": "#EF4444"
}
```

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "수정되었습니다.",
  "data": null
}
```

---

### 3-3. 북마크 폴더 삭제

```
DELETE /api/bookmarks/folders/{folderId}
```

**Path Variable**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `folderId` | Long | 삭제할 폴더 ID |

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "삭제되었습니다.",
  "data": null
}
```

---

### 3-4. 북마크 폴더 목록 조회

```
GET /api/bookmarks/folders
```

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "조회되었습니다.",
  "data": [
    {
      "folderId": 1,
      "name": "개발 아티클",
      "icon": "📂",
      "color": "#3B82F6"
    }
  ]
}
```

---

### 3-5. 북마크 등록

```
POST /api/bookmarks
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `contentId` | String | O | 북마크할 콘텐츠 ID (MySQL Content ID의 문자열 표현) |
| `folderId` | Long | X | 담을 폴더 ID (미지정 시 미분류) |

```json
{
  "contentId": "123",
  "folderId": 1
}
```

**Response** `201 Created`

```json
{
  "status": 201,
  "message": "작성되었습니다.",
  "data": 42
}
```

> `data`: 생성된 북마크 ID (Long)

---

### 3-6. 북마크 목록 조회

```
GET /api/bookmarks
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `folderId` | Long | X | - | 특정 폴더로 필터링 |
| `lastId` | Long | X | - | 이전 페이지 마지막 북마크 ID (커서) |
| `size` | int | X | 20 | 페이지 크기 |

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "조회되었습니다.",
  "data": {
    "content": [
      {
        "bookmarkId": 42,
        "folderId": 1,
        "folderName": "개발 아티클",
        "createdAt": "2026-03-01T10:00:00",
        "content": {
          "contentId": "123",
          "title": "Spring Boot 3.x 마이그레이션 가이드",
          "summary": "Spring Boot 3.x로 업그레이드하는 방법을 알아봅니다.",
          "sourceName": "Spring Blog",
          "sourceLogoUrl": "https://spring.io/img/spring-logo.svg",
          "originalUrl": "https://spring.io/blog/...",
          "thumbnailUrl": "https://example.com/thumb.jpg",
          "publishedAt": "2026-02-28T09:00:00",
          "bookmarkId": 42
        }
      }
    ],
    "nextCursorId": 41,
    "hasNext": true
  }
}
```

---

### 3-7. 북마크 폴더 이동

```
PATCH /api/bookmarks/{bookmarkId}/folder
```

**Path Variable**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `bookmarkId` | Long | 이동할 북마크 ID |

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `folderId` | Long | O | 이동할 대상 폴더 ID |

```json
{
  "folderId": 2
}
```

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "수정되었습니다.",
  "data": null
}
```

---

### 3-8. 북마크 폴더에서 제거 (미분류로 이동)

```
DELETE /api/bookmarks/{bookmarkId}/folder
```

**Path Variable**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `bookmarkId` | Long | 대상 북마크 ID |

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "수정되었습니다.",
  "data": null
}
```

---

### 3-9. 북마크 해제

```
DELETE /api/bookmarks/{bookmarkId}
```

**Path Variable**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `bookmarkId` | Long | 해제할 북마크 ID |

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "삭제되었습니다.",
  "data": null
}
```

---

## 4. 키워드 (Keywords)

> Base Path: `/api/keywords` — **인증 필요** (단, 트렌딩 키워드는 인증 불필요)

---

### 4-1. 내 키워드 목록 조회

```
GET /api/keywords
```

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "조회되었습니다.",
  "data": [
    {
      "keywordId": 1,
      "name": "Spring Boot",
      "isNotificationEnabled": true
    }
  ]
}
```

---

### 4-2. 트렌딩 키워드 조회

```
GET /api/keywords/trending
```

> **인증 불필요**

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `size` | int | X | 10 | 조회할 키워드 수 |

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "조회되었습니다.",
  "data": [
    {
      "name": "AI",
      "userCount": 352
    },
    {
      "name": "Spring Boot",
      "userCount": 128
    }
  ]
}
```

---

### 4-3. 키워드 추가

```
POST /api/keywords
```

**Request Body**

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `name` | String | O | 2~25자 | 키워드 |

```json
{
  "name": "Kubernetes"
}
```

**Response** `201 Created`

```json
{
  "status": 201,
  "message": "작성되었습니다.",
  "data": {
    "keywordId": 5,
    "name": "Kubernetes",
    "isNotificationEnabled": true
  }
}
```

---

### 4-4. 키워드 알림 토글

```
PATCH /api/keywords/{keywordId}/toggle
```

**Path Variable**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `keywordId` | Long | 토글할 키워드 ID |

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "수정되었습니다.",
  "data": {
    "keywordId": 5,
    "name": "Kubernetes",
    "isNotificationEnabled": false
  }
}
```

---

### 4-5. 키워드 삭제

```
DELETE /api/keywords/{keywordId}
```

**Path Variable**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `keywordId` | Long | 삭제할 키워드 ID |

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "삭제되었습니다.",
  "data": null
}
```

---

## 5. 소스 (Sources)

> Base Path: `/api/sources` — **인증 필요**

---

### 5-1. 내 소스 목록 조회

```
GET /api/sources/my
```

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "조회되었습니다.",
  "data": [
    {
      "sourceId": 10,
      "userSourceId": 3,
      "userDefinedName": "Spring 공식 블로그",
      "url": "https://spring.io/blog.atom",
      "logoUrl": "https://spring.io/img/spring-logo.svg",
      "lastCrawledAt": "2026-03-01T06:00:00",
      "receiveFeed": true
    }
  ]
}
```

---

### 5-2. 내 소스 검색

```
GET /api/sources/my/search
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `keyword` | String | O | 검색어 (소스 이름 기준) |

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "조회되었습니다.",
  "data": [
    {
      "sourceId": 10,
      "userSourceId": 3,
      "userDefinedName": "Spring 공식 블로그",
      "url": "https://spring.io/blog.atom",
      "logoUrl": "https://spring.io/img/spring-logo.svg",
      "lastCrawledAt": "2026-03-01T06:00:00",
      "receiveFeed": true
    }
  ]
}
```

---

### 5-3. 소스 추가 (구독)

```
POST /api/sources
```

**Request Body**

| 필드 | 타입 | 필수 | 제약 | 설명 |
|------|------|------|------|------|
| `name` | String | O | - | 소스 이름 (사용자 정의) |
| `url` | String | O | `http://` 또는 `https://`로 시작 | RSS 피드 URL |
| `receiveFeed` | Boolean | X | 기본값: `true` | 피드 수신 여부 |

```json
{
  "name": "Spring 공식 블로그",
  "url": "https://spring.io/blog.atom",
  "receiveFeed": true
}
```

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "작성되었습니다.",
  "data": {
    "sourceId": 10,
    "userSourceId": 3,
    "userDefinedName": "Spring 공식 블로그",
    "url": "https://spring.io/blog.atom",
    "logoUrl": null,
    "lastCrawledAt": null,
    "receiveFeed": true
  }
}
```

---

### 5-4. 소스 구독 해제

```
DELETE /api/sources/my/{userSourceId}
```

**Path Variable**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `userSourceId` | Long | 구독 해제할 UserSource ID |

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "삭제되었습니다.",
  "data": null
}
```

---

### 5-5. 소스 피드 수신 토글

```
PATCH /api/sources/my/{userSourceId}/receive-feed
```

**Path Variable**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `userSourceId` | Long | 토글할 UserSource ID |

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "수정되었습니다.",
  "data": {
    "sourceId": 10,
    "userSourceId": 3,
    "userDefinedName": "Spring 공식 블로그",
    "url": "https://spring.io/blog.atom",
    "logoUrl": "https://spring.io/img/spring-logo.svg",
    "lastCrawledAt": "2026-03-01T06:00:00",
    "receiveFeed": false
  }
}
```

---

### 5-6. 추천 소스 목록 조회

```
GET /api/sources/recommended
```

**Query Parameters** (Spring Pageable)

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `page` | int | X | 0 | 페이지 번호 (0부터 시작) |
| `size` | int | X | 10 | 페이지 크기 |

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "조회되었습니다.",
  "data": [
    {
      "sourceId": 10,
      "url": "https://spring.io/blog.atom",
      "subscriberCount": 412
    }
  ]
}
```

---

## 6. 피드 (Feed)

### GET /api/feed — 개인화 피드 조회

구독 중인 소스의 최신 콘텐츠를 커서 기반 페이지네이션으로 반환합니다.

- **인증**: 필요 (Bearer Token)
- **Query Parameters**:

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| `lastId` | Long | X | 이전 페이지 마지막 항목의 `publishedAt` epoch millis (커서) |
| `size` | Integer | X | 페이지 크기 (기본값: 10) |
| `keyword` | String | X | 키워드 필터링 |

- **응답**:

```json
{
  "status": 200,
  "message": "조회되었습니다.",
  "data": {
    "content": [
      {
        "contentId": "123",
        "title": "Spring Boot 3.5 Released",
        "summary": "Spring Boot 3.5 brings...",
        "sourceName": "Spring Blog",
        "sourceLogoUrl": "https://spring.io/img/spring-logo.svg",
        "originalUrl": "https://spring.io/blog/2025/...",
        "thumbnailUrl": "https://example.com/thumb.jpg",
        "publishedAt": "2025-03-01T12:00:00",
        "bookmarkId": 42
      }
    ],
    "hasNext": true,
    "nextCursorId": 1740830400000
  }
}
```

> `nextCursorId`는 마지막 항목의 `publishedAt`을 epoch milliseconds로 변환한 값입니다. 다음 요청 시 `lastId`에 전달합니다.
> `bookmarkId`는 해당 콘텐츠를 북마크한 경우 북마크 ID, 북마크하지 않은 경우 `null`입니다.

---

## 7. 알림 (Notifications)

> Base Path: `/api/notifications` — **인증 필요** (`Authorization: Bearer <token>`)

---

### 7-1. SSE 구독 (실시간 알림 연결)

```
GET /api/notifications/subscribe
```

> `Content-Type: text/event-stream`

**Request Headers**

| 헤더 | 필수 | 설명 |
|------|------|------|
| `Last-Event-ID` | X | 마지막으로 수신한 알림 ID. 재연결 시 유실된 알림을 재전송 받기 위해 사용 |

**Response** `200 OK` — SSE 스트림

연결 직후 더미 이벤트가 전송됩니다 (503 방지용):

```
event: notification
id: 0
data: connected
```

이후 새 알림 발생 시:

```
event: notification
id: 42
data: {"id":42,"title":"Spring Boot 3.5 Released","message":"새로운 글이 등록되었습니다.","isRead":false,"createdAt":"2026-03-23T10:00:00"}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | 알림 ID (SSE `id` 필드와 동일) |
| `title` | String | 콘텐츠 제목 |
| `message` | String | 알림 메시지 |
| `isRead` | Boolean | 읽음 여부 |
| `createdAt` | LocalDateTime | 알림 생성 시각 |

---

### 7-2. 알림 목록 조회

```
GET /api/notifications
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `lastId` | Long | X | - | 이전 페이지 마지막 알림 ID (커서) |
| `size` | int | X | 20 | 페이지 크기 (최대 50) |

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "조회에 성공하였습니다.",
  "data": {
    "content": [
      {
        "id": 42,
        "title": "Spring Boot 3.5 Released",
        "message": "새로운 글이 등록되었습니다.",
        "isRead": false,
        "createdAt": "2026-03-23T10:00:00"
      }
    ],
    "nextCursorId": 41,
    "hasNext": true
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `id` | Long | 알림 ID |
| `title` | String | 콘텐츠 제목 |
| `message` | String | 알림 메시지 |
| `isRead` | Boolean | 읽음 여부 |
| `createdAt` | LocalDateTime | 알림 생성 시각 |

---

## 8. 결제 수단 (Payment Methods)

> Base Path: `/api/payment-methods` — **인증 필요** (`Authorization: Bearer <token>`)

---

### 8-1. 고객 키 발급

```
GET /api/payment-methods/customer-key
```

**동작 설명**

- 토스 SDK 호출 전 프론트엔드에서 먼저 요청합니다.
- 이미 발급된 키가 있으면 그대로 반환하고, 없으면 UUID를 생성하여 저장 후 반환합니다.

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "고객 키가 발급되었습니다.",
  "data": "550e8400-e29b-41d4-a716-446655440000"
}
```

> `data`: 토스 SDK `requestBillingAuth()` 호출 시 `customerKey`로 사용합니다.

---

### 8-2. 결제 수단 등록

```
POST /api/payment-methods/register
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `authKey` | String | O | 토스 SDK에서 발급한 인증 키 (5분 내 사용) |

```json
{
  "authKey": "auth_gKL4CsqdQn_Q1eAJeQe..."
}
```

**동작 설명**

- `GET /api/payment-methods/customer-key`로 발급받은 customerKey를 토스 SDK에 전달하여 authKey를 먼저 받아야 합니다.
- 토스페이먼츠 API로 빌링키를 발급하고 결제 수단을 저장합니다.
- 첫 번째 카드 등록 시 자동으로 기본 결제 수단으로 설정됩니다.

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "결제 수단이 등록되었습니다.",
  "data": {
    "methodId": 1,
    "methodType": "CARD",
    "providerName": "신한",
    "displayNumber": "4330****0000",
    "isDefault": true,
    "createdAt": "2026-04-03T10:00:00"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `methodId` | Long | 등록된 결제 수단 ID |
| `methodType` | String | 결제 수단 타입 (`CARD` \| `BANK`) |
| `providerName` | String | 카드사명 |
| `displayNumber` | String | 마스킹 카드번호 |
| `isDefault` | Boolean | 기본 결제 수단 여부 |
| `createdAt` | String | 등록 일시 |

**Error Response**

| 상황 | 상태 코드 | 메시지 |
|------|-----------|--------|
| 토스 빌링키 발급 실패 | `402 Payment Required` | 결제 처리에 실패했습니다. |
| authKey 만료 (5분 초과) | `422 Unprocessable Entity` | 만료되었거나 유효하지 않은 인증 키입니다. |
| 동일 카드 중복 등록 | `409 Conflict` | 이미 등록된 결제 수단입니다. |

---

### 8-3. 결제 수단 목록 조회

```
GET /api/payment-methods
```

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "결제 수단 목록 조회에 성공했습니다.",
  "data": [
    {
      "methodId": 1,
      "methodType": "CARD",
      "providerName": "신한",
      "displayNumber": "4330****0000",
      "isDefault": true,
      "createdAt": "2026-04-03T10:00:00"
    },
    {
      "methodId": 2,
      "methodType": "CARD",
      "providerName": "현대",
      "displayNumber": "5500****1111",
      "isDefault": false,
      "createdAt": "2026-04-03T11:00:00"
    }
  ]
}
```

> 소프트 삭제된 수단(`isActive = false`)은 결과에서 제외됩니다.

---

### 8-4. 결제 수단 삭제

```
DELETE /api/payment-methods/{methodId}
```

**Path Variable**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `methodId` | Long | 삭제할 결제 수단 ID |

**동작 설명**

- 토스페이먼츠 빌링키를 먼저 삭제한 뒤 DB를 소프트 삭제합니다.
- 빌링키 삭제 실패 시 DB 소프트 삭제를 진행하지 않습니다.
- 삭제된 수단이 기본 결제 수단이었다면, 남은 수단 중 가장 최근 등록된 수단을 자동으로 기본 수단으로 지정합니다.

**Response** `204 No Content`

**Error Response**

| 상황 | 상태 코드 | 메시지 |
|------|-----------|--------|
| 존재하지 않는 수단 | `404 Not Found` | 결제 수단을 찾을 수 없습니다. |
| 본인 소유가 아닌 수단 | `403 Forbidden` | 해당 결제 수단에 대한 권한이 없습니다. |
| 활성 구독에 연결된 수단 | `409 Conflict` | 구독 중인 결제 수단은 삭제할 수 없습니다. 먼저 결제 수단을 변경해주세요. |

---

### 8-5. 기본 결제 수단 변경

```
PATCH /api/payment-methods/{methodId}/default
```

**Path Variable**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `methodId` | Long | 기본으로 설정할 결제 수단 ID |

**동작 설명**

- 기존 기본 수단은 자동으로 해제됩니다.
- 이미 기본 수단인 경우 변경 없이 `200 OK`를 반환합니다 (멱등성 보장).
- 활성(`ACTIVE`) 또는 일시 중지(`PAUSED`) 상태의 구독이 있다면 해당 구독의 결제 수단도 함께 변경됩니다.

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "기본 결제 수단이 변경되었습니다.",
  "data": null
}
```

**Error Response**

| 상황 | 상태 코드 | 메시지 |
|------|-----------|--------|
| 존재하지 않거나 삭제된 수단 | `404 Not Found` | 결제 수단을 찾을 수 없습니다. |
| 본인 소유가 아닌 수단 | `403 Forbidden` | 해당 결제 수단에 대한 권한이 없습니다. |

---

## 9. 구독 (Subscriptions)

> Base Path: `/api/subscriptions` — **JWT 인증 필요**

---

### 9-1. 구독 시작

```
POST /api/subscriptions/start
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `methodId` | Long | O | 결제에 사용할 결제 수단 ID |

```json
{
  "methodId": 1
}
```

**동작 설명**

- 등록된 결제 수단의 빌링키로 첫 결제(9,900원)를 즉시 실행합니다.
- 결제 성공 시에만 구독 레코드가 생성됩니다.
- 중복 결제 방지를 위해 `payment_history`에 READY 상태로 선저장 후 결제를 진행합니다.

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "구독이 시작되었습니다.",
  "data": {
    "subscriptionId": 1,
    "status": "ACTIVE",
    "price": 9900,
    "nextBillingAt": "2024-02-15T10:30:00",
    "expiredAt": "2024-02-15T10:30:00"
  }
}
```

**Error Response**

| 상황 | 상태 코드 | 메시지 |
|------|-----------|--------|
| 이미 ACTIVE 구독 존재 | `409 Conflict` | 이미 구독 중입니다. |
| 존재하지 않는 결제 수단 | `404 Not Found` | 결제 수단을 찾을 수 없습니다. |
| 토스 결제 실패 | `402 Payment Required` | 결제 처리에 실패했습니다. |

---

### 9-2. 내 구독 상태 조회

```
GET /api/subscriptions/me
```

**Request** : 없음 (JWT에서 userId 추출)

**동작 설명**

- 가장 최근 구독 정보를 반환합니다.
- 구독이 없는 경우 `status: "NONE"`으로 반환합니다.

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "구독 상태 조회에 성공했습니다.",
  "data": {
    "subscriptionId": 1,
    "status": "ACTIVE",
    "price": 9900,
    "startedAt": "2024-01-15T10:30:00",
    "nextBillingAt": "2024-02-15T10:30:00",
    "expiredAt": "2024-02-15T10:30:00",
    "canceledAt": null,
    "retryCount": 0,
    "providerName": "신한",
    "displayNumber": "4330****0000"
  }
}
```

**구독 없는 경우**

```json
{
  "status": 200,
  "message": "구독 상태 조회에 성공했습니다.",
  "data": {
    "subscriptionId": null,
    "status": "NONE"
  }
}
```

**status 값 목록**

| 값 | 설명 |
|----|------|
| `NONE` | 구독 없음 |
| `ACTIVE` | 구독 활성 |
| `PAUSED` | 결제 실패로 일시 정지 |
| `CANCELED` | 해지 신청 (만료일까지 서비스 유지) |
| `REFUNDED` | 결제일 1일 이내 즉시 취소 및 환불 완료 |
| `INACTIVE` | 만료일 도래로 비활성화 |

---

### 9-3. 구독 해지

```
POST /api/subscriptions/cancel
```

**Request** : 없음 (JWT에서 userId 추출)

**동작 설명**

- 즉시 중단이 아닌, 이미 결제된 만료일(`expiredAt`)까지 서비스를 유지합니다.
- 토스 빌링키는 삭제하지 않아 재구독 시 재사용 가능합니다.
- 만료일 이후 스케줄러가 자동으로 `INACTIVE`로 전환합니다.

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "구독이 해지되었습니다.",
  "data": {
    "status": "CANCELED",
    "expiredAt": "2024-02-15T10:30:00",
    "canceledAt": "2024-01-20T15:00:00"
  }
}
```

**Error Response**

| 상황 | 상태 코드 | 메시지 |
|------|-----------|--------|
| ACTIVE 구독 없음 | `404 Not Found` | 활성화된 구독이 없습니다. |
| 이미 해지된 구독 | `409 Conflict` | 이미 해지 신청된 구독입니다. |

---

### 9-4. 구독 재개

```
POST /api/subscriptions/resume
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `methodId` | Long | O | 재개에 사용할 결제 수단 ID |

```json
{
  "methodId": 2
}
```

**동작 설명**

- 결제 실패로 PAUSED된 구독을 재개합니다.
- 밀린 결제를 즉시 재시도합니다.
- 재시도 성공 시 `retryCount`가 0으로 초기화되고 `nextBillingAt`이 재설정됩니다.
- 재시도 실패 시 PAUSED 상태 유지, `payment_history` FAILED 기록.

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "구독이 재개되었습니다.",
  "data": {
    "subscriptionId": 1,
    "status": "ACTIVE",
    "nextBillingAt": "2024-02-20T10:30:00"
  }
}
```

**Error Response**

| 상황 | 상태 코드 | 메시지 |
|------|-----------|--------|
| PAUSED 구독 없음 | `404 Not Found` | 일시 정지된 구독이 없습니다. |
| 존재하지 않는 결제 수단 | `404 Not Found` | 결제 수단을 찾을 수 없습니다. |
| 재시도 결제 실패 | `402 Payment Required` | 결제 처리에 실패했습니다. |

---

### 9-5. 구독 취소 (즉시 환불)

```
POST /api/subscriptions/refund
```

**Request** : 없음 (JWT에서 userId 추출)

**동작 설명**

- 결제일로부터 **1일 이내**에만 취소 가능합니다.
- 토스페이먼츠 결제 취소 API를 호출하여 전액 환불 처리합니다.
- 환불 성공 시 구독이 즉시 만료(`expiredAt` → 현재 시각)되고 상태가 `REFUNDED`로 전환됩니다.

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "구독이 취소되었습니다. 환불이 처리됩니다.",
  "data": {
    "subscriptionId": 1,
    "status": "REFUNDED",
    "canceledAt": "2024-01-15T11:00:00"
  }
}
```

**Error Response**

| 상황 | 상태 코드 | 메시지 |
|------|-----------|--------|
| ACTIVE 구독 없음 | `404 Not Found` | 활성화된 구독이 없습니다. |
| 결제일로부터 1일 초과 | `422 Unprocessable Entity` | 결제일로부터 1일이 지나 취소할 수 없습니다. |
| 토스 환불 API 실패 | `502 Bad Gateway` | 환불 처리에 실패했습니다. |

---

## 10. 결제 이력 (Payment History)

> Base Path: `/api/payment-history` — **JWT 인증 필요**

---

### 10-1. 결제 이력 목록 조회

```
GET /api/payment-history
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|----------|------|------|--------|------|
| `cursorId` | Long | X | - | 이전 응답의 `nextCursorId` (첫 요청 시 생략) |
| `size` | Integer | X | 10 | 페이지당 항목 수 (최대 50) |
| `status` | String | X | 전체 | 상태 필터 (DONE, FAILED, CANCELED 등) |

**동작 설명**

- 로그인한 사용자의 결제 이력을 최신순(paymentId 내림차순)으로 반환합니다.
- 커서 기반 페이징: `cursorId` 미지정 시 최신 이력부터, 지정 시 해당 ID 미만 이력을 반환합니다.
- `READY`, `IN_PROGRESS`는 내부 처리 상태이므로 응답에서 제외됩니다.
- `payment_method`가 소프트 삭제된 경우에도 이력은 정상 반환됩니다.
- 결제 이력이 없는 경우 `content: []`로 정상 반환합니다 (404 아님).

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "결제 이력 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "paymentId": 5,
        "orderId": "42-20260401100000-A1B2",
        "orderName": "프리미엄 구독 1개월",
        "amount": 9900,
        "status": "DONE",
        "failReason": null,
        "approvedAt": "2026-04-01T10:00:00",
        "createdAt": "2026-04-01T10:00:00",
        "paymentMethod": {
          "providerName": "현대",
          "displayNumber": "1234****5678",
          "methodType": "CARD"
        }
      },
      {
        "paymentId": 4,
        "orderId": "42-20260301100000-C3D4",
        "orderName": "프리미엄 구독 1개월",
        "amount": 9900,
        "status": "FAILED",
        "failReason": "EXCEED_MAX_DAILY_PAYMENT_COUNT",
        "approvedAt": null,
        "createdAt": "2026-03-01T10:00:00",
        "paymentMethod": {
          "providerName": "현대",
          "displayNumber": "1234****5678",
          "methodType": "CARD"
        }
      }
    ],
    "nextCursorId": 4,
    "hasNext": false
  }
}
```

**status 필터 가능 값**

| 값 | 설명 |
|----|------|
| `DONE` | 결제 완료 |
| `FAILED` | 결제 실패 |
| `CANCELED` | 결제 취소 (환불) |
| `PARTIAL_CANCELED` | 부분 취소 |
| `ABORTED` | 결제 중단 |
| `EXPIRED` | 결제 만료 |

**Error Response**

| 상황 | 상태 코드 | 메시지 |
|------|-----------|--------|
| size가 50 초과 | `400 Bad Request` | 최대 조회 가능 수는 50개입니다. |
| 유효하지 않은 status 값 | `400 Bad Request` | 유효하지 않은 상태값입니다. |

---

## API 목록 요약

| 메서드 | 경로 | 설명 | 인증 |
|--------|------|------|------|
| POST | `/api/auth/join` | 회원가입 | X |
| POST | `/api/auth/login` | 로그인 | X |
| POST | `/api/auth/refresh` | 토큰 재발급 | X (Cookie) |
| POST | `/api/auth/email-verification/request` | 이메일 인증 코드 발송 | X |
| POST | `/api/auth/email-verification/confirm` | 이메일 인증 코드 확인 | X |
| POST | `/api/auth/password-reset/request` | 비밀번호 재설정 이메일 발송 | X |
| POST | `/api/auth/password-reset/verify` | 비밀번호 재설정 코드 확인 | X |
| POST | `/api/auth/password-reset/confirm` | 비밀번호 재설정 확정 | X |
| PATCH | `/api/users/password` | 비밀번호 변경 | O |
| DELETE | `/api/users` | 회원 탈퇴 | O |
| GET | `/api/bookmarks/folders` | 북마크 폴더 목록 조회 | O |
| POST | `/api/bookmarks/folders` | 북마크 폴더 생성 | O |
| PATCH | `/api/bookmarks/folders/{folderId}` | 북마크 폴더 수정 | O |
| DELETE | `/api/bookmarks/folders/{folderId}` | 북마크 폴더 삭제 | O |
| GET | `/api/bookmarks` | 북마크 목록 조회 | O |
| POST | `/api/bookmarks` | 북마크 등록 | O |
| DELETE | `/api/bookmarks/{bookmarkId}` | 북마크 해제 | O |
| PATCH | `/api/bookmarks/{bookmarkId}/folder` | 북마크 폴더 이동 | O |
| DELETE | `/api/bookmarks/{bookmarkId}/folder` | 북마크 폴더에서 제거 | O |
| GET | `/api/keywords` | 내 키워드 목록 조회 | O |
| GET | `/api/keywords/trending` | 트렌딩 키워드 조회 | X |
| POST | `/api/keywords` | 키워드 추가 | O |
| PATCH | `/api/keywords/{keywordId}/toggle` | 키워드 알림 토글 | O |
| DELETE | `/api/keywords/{keywordId}` | 키워드 삭제 | O |
| GET | `/api/sources/my` | 내 소스 목록 조회 | O |
| GET | `/api/sources/my/search` | 내 소스 검색 | O |
| POST | `/api/sources` | 소스 추가 (구독) | O |
| DELETE | `/api/sources/my/{userSourceId}` | 소스 구독 해제 | O |
| PATCH | `/api/sources/my/{userSourceId}/receive-feed` | 소스 피드 수신 토글 | O |
| GET | `/api/sources/recommended` | 추천 소스 목록 조회 | O |
| GET | `/api/feed` | 개인화 피드 조회 | O |
| GET | `/api/notifications/subscribe` | SSE 구독 (실시간 알림 연결) | O |
| GET | `/api/notifications` | 알림 목록 조회 | O |
| GET | `/api/payment-methods/customer-key` | 고객 키 발급 | O |
| POST | `/api/payment-methods/register` | 결제 수단 등록 | O |
| GET | `/api/payment-methods` | 결제 수단 목록 조회 | O |
| DELETE | `/api/payment-methods/{methodId}` | 결제 수단 삭제 | O |
| PATCH | `/api/payment-methods/{methodId}/default` | 기본 결제 수단 변경 | O |
| POST | `/api/subscriptions/start` | 구독 시작 | O |
| GET | `/api/subscriptions/me` | 내 구독 상태 조회 | O |
| POST | `/api/subscriptions/cancel` | 구독 해지 | O |
| POST | `/api/subscriptions/resume` | 구독 재개 | O |
| POST | `/api/subscriptions/refund` | 구독 취소 (1일 이내 환불) | O |
| GET | `/api/payment-history` | 결제 이력 목록 조회 | O |
