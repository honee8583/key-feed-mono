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

### 오프셋 페이지네이션 응답 (`data` 필드)

```json
{
  "content": [ ],
  "page": 0,
  "size": 10,
  "totalElements": 100,
  "totalPages": 10,
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
  "message": "저장에 성공하였습니다.",
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
  "message": "토큰발급에 성공했습니다.",
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
  "message": "이메일 전송에 성공하였습니다.",
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
  "message": "비밀번호 재설정을 위한 인증 이메일이 발송되었습니다.",
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
  "message": "비밀번호가 성공적으로 변경되었습니다.",
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
  "message": "저장에 성공하였습니다.",
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
  "message": "수정에 성공하였습니다.",
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
  "message": "삭제에 성공하였습니다.",
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
  "message": "조회에 성공하였습니다.",
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
  "message": "저장에 성공하였습니다.",
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
  "message": "조회에 성공하였습니다.",
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
  "message": "수정에 성공하였습니다.",
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
  "message": "수정에 성공하였습니다.",
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
  "message": "삭제에 성공하였습니다.",
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
  "message": "조회에 성공하였습니다.",
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
  "message": "조회에 성공하였습니다.",
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
  "message": "저장에 성공하였습니다.",
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
  "message": "수정에 성공하였습니다.",
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
  "message": "삭제에 성공하였습니다.",
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
  "message": "조회에 성공하였습니다.",
  "data": [
    {
      "sourceId": 10,
      "userSourceId": 3,
      "userDefinedName": "Spring 공식 블로그",
      "url": "https://spring.io/blog.atom",
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
  "message": "조회에 성공하였습니다.",
  "data": [
    {
      "sourceId": 10,
      "userSourceId": 3,
      "userDefinedName": "Spring 공식 블로그",
      "url": "https://spring.io/blog.atom",
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
  "message": "저장에 성공하였습니다.",
  "data": {
    "sourceId": 10,
    "userSourceId": 3,
    "userDefinedName": "Spring 공식 블로그",
    "url": "https://spring.io/blog.atom",
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
  "message": "삭제에 성공하였습니다.",
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
  "message": "수정에 성공하였습니다.",
  "data": {
    "sourceId": 10,
    "userSourceId": 3,
    "userDefinedName": "Spring 공식 블로그",
    "url": "https://spring.io/blog.atom",
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
  "message": "조회에 성공하였습니다.",
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

> Base Path: `/api/feed` — **인증 필요** (`Authorization: Bearer <token>`)

---

### 6-1. 개인화 피드 조회 (커서 기반)

```
GET /api/feed
```

구독 중인 소스의 최신 콘텐츠를 커서 기반 페이지네이션으로 반환합니다.

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `lastId` | Long | X | - | 이전 페이지 마지막 항목의 `publishedAt` epoch millis (커서) |
| `size` | Integer | X | 10 | 페이지 크기 |
| `keyword` | String | X | - | 키워드 필터링 |

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "조회에 성공하였습니다.",
  "data": {
    "content": [
      {
        "contentId": "123",
        "title": "Spring Boot 3.5 Released",
        "summary": "Spring Boot 3.5 brings...",
        "sourceName": "Spring Blog",
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

### 6-2. 개인화 피드 조회 (오프셋 기반)

```
GET /api/feed/offset
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `page` | int | X | 0 | 페이지 번호 (0부터 시작) |
| `size` | Integer | X | 10 | 페이지 크기 |
| `keyword` | String | X | - | 키워드 필터링 |

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "조회에 성공하였습니다.",
  "data": {
    "content": [
      {
        "contentId": "123",
        "title": "Spring Boot 3.5 Released",
        "summary": "Spring Boot 3.5 brings...",
        "sourceName": "Spring Blog",
        "originalUrl": "https://spring.io/blog/2025/...",
        "thumbnailUrl": "https://example.com/thumb.jpg",
        "publishedAt": "2025-03-01T12:00:00",
        "bookmarkId": 42
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 100,
    "totalPages": 10,
    "hasNext": true
  }
}
```

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

토스 SDK 호출 전 프론트엔드에서 요청합니다.

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "고객 키가 발급되었습니다.",
  "data": "<customer_key>"
}
```

---

### 8-2. 결제 수단 등록

```
POST /api/payment-methods/register
```

**Request Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `authKey` | String | O | 토스 SDK에서 발급받은 인증 키 |

```json
{
  "authKey": "auth_key_from_toss_sdk"
}
```

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "결제 수단이 등록되었습니다.",
  "data": {
    "methodId": 1,
    "methodType": "CARD",
    "providerName": "신한카드",
    "displayNumber": "1234",
    "isDefault": true,
    "createdAt": "2026-03-01T12:00:00"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `methodId` | Long | 결제 수단 ID |
| `methodType` | String | 결제 수단 유형 (`CARD` 등) |
| `providerName` | String | 카드사/제공사 이름 |
| `displayNumber` | String | 카드 번호 뒷자리 |
| `isDefault` | Boolean | 기본 결제 수단 여부 |
| `createdAt` | String | 등록 일시 |

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
      "providerName": "신한카드",
      "displayNumber": "1234",
      "isDefault": true,
      "createdAt": "2026-03-01T12:00:00"
    }
  ]
}
```

---

### 8-4. 결제 수단 삭제

```
DELETE /api/payment-methods/{methodId}
```

**Path Variable**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `methodId` | Long | 삭제할 결제 수단 ID |

**Response** `204 No Content`

---

### 8-5. 기본 결제 수단 변경

```
PATCH /api/payment-methods/{methodId}/default
```

**Path Variable**

| 파라미터 | 타입 | 설명 |
|---------|------|------|
| `methodId` | Long | 기본으로 설정할 결제 수단 ID |

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "기본 결제 수단이 변경되었습니다.",
  "data": null
}
```

---

## 9. 구독 (Subscriptions)

> Base Path: `/api/subscriptions` — **인증 필요** (`Authorization: Bearer <token>`)

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

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "구독이 시작되었습니다.",
  "data": {
    "subscriptionId": 10,
    "status": "ACTIVE",
    "price": 9900,
    "nextBillingAt": "2026-05-01T00:00:00",
    "expiredAt": null
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `subscriptionId` | Long | 구독 ID |
| `status` | String | 구독 상태 (`ACTIVE` \| `CANCELED` \| `EXPIRED` 등) |
| `price` | int | 구독 금액 |
| `nextBillingAt` | String | 다음 결제 예정일 |
| `expiredAt` | String | 구독 만료일 (해지 예약 시 존재) |

---

### 9-2. 내 구독 조회

```
GET /api/subscriptions/me
```

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "구독 상태 조회에 성공했습니다.",
  "data": {
    "subscriptionId": 10,
    "status": "ACTIVE",
    "price": 9900,
    "startedAt": "2026-04-01T00:00:00",
    "nextBillingAt": "2026-05-01T00:00:00",
    "expiredAt": null,
    "canceledAt": null,
    "retryCount": 0,
    "providerName": "신한카드",
    "displayNumber": "1234"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `subscriptionId` | Long | 구독 ID |
| `status` | String | 구독 상태 |
| `price` | Integer | 구독 금액 |
| `startedAt` | String | 구독 시작일 |
| `nextBillingAt` | String | 다음 결제 예정일 |
| `expiredAt` | String | 구독 만료일 |
| `canceledAt` | String | 해지 신청일 |
| `retryCount` | Integer | 결제 재시도 횟수 |
| `providerName` | String | 결제 수단 카드사 |
| `displayNumber` | String | 결제 수단 카드 번호 뒷자리 |

> 구독이 없는 경우 `data.status`가 `"NONE"`으로 반환됩니다.

---

### 9-3. 구독 해지 (만료일까지 서비스 유지)

```
POST /api/subscriptions/cancel
```

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "구독이 해지되었습니다.",
  "data": {
    "status": "CANCEL_RESERVED",
    "expiredAt": "2026-05-01T00:00:00",
    "canceledAt": "2026-04-21T10:00:00"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `status` | String | 변경된 구독 상태 |
| `expiredAt` | String | 서비스 이용 가능 마지막 일시 |
| `canceledAt` | String | 해지 신청 일시 |

---

### 9-4. 구독 재개

```
POST /api/subscriptions/resume
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

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "구독이 재개되었습니다.",
  "data": {
    "subscriptionId": 10,
    "status": "ACTIVE",
    "nextBillingAt": "2026-05-01T00:00:00"
  }
}
```

---

### 9-5. 구독 즉시 취소 (결제일 1일 이내 환불)

```
POST /api/subscriptions/refund
```

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "구독이 취소되었습니다. 환불이 처리됩니다.",
  "data": {
    "subscriptionId": 10,
    "status": "REFUNDED",
    "canceledAt": "2026-04-21T10:00:00"
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `subscriptionId` | Long | 구독 ID |
| `status` | String | 변경된 구독 상태 |
| `canceledAt` | String | 취소 처리 일시 |

---

## 10. 결제 내역 (Payment History)

> Base Path: `/api/payment-history` — **인증 필요** (`Authorization: Bearer <token>`)

---

### 10-1. 결제 내역 조회

```
GET /api/payment-history
```

**Query Parameters**

| 파라미터 | 타입 | 필수 | 기본값 | 설명 |
|---------|------|------|--------|------|
| `cursorId` | Long | X | - | 이전 페이지 마지막 결제 내역 ID (커서) |
| `size` | int | X | 10 | 페이지 크기 |
| `status` | String | X | - | 결제 상태 필터 (`APPROVED`, `FAILED` 등) |

**Response** `200 OK`

```json
{
  "status": 200,
  "message": "결제 이력 조회에 성공했습니다.",
  "data": {
    "content": [
      {
        "paymentId": 1,
        "orderId": "order_abc123",
        "orderName": "Key-Feed 월간 구독",
        "amount": 9900,
        "status": "APPROVED",
        "failReason": null,
        "approvedAt": "2026-04-01T00:00:00",
        "createdAt": "2026-04-01T00:00:00",
        "paymentMethod": {
          "providerName": "신한카드",
          "displayNumber": "1234",
          "methodType": "CARD"
        }
      }
    ],
    "nextCursorId": null,
    "hasNext": false
  }
}
```

| 필드 | 타입 | 설명 |
|------|------|------|
| `paymentId` | Long | 결제 내역 ID |
| `orderId` | String | 주문 ID |
| `orderName` | String | 주문명 |
| `amount` | int | 결제 금액 |
| `status` | String | 결제 상태 (`APPROVED` \| `FAILED` 등) |
| `failReason` | String | 실패 사유 (실패 시에만 존재) |
| `approvedAt` | String | 결제 승인 일시 |
| `createdAt` | String | 결제 내역 생성 일시 |
| `paymentMethod.providerName` | String | 카드사 이름 |
| `paymentMethod.displayNumber` | String | 카드 번호 뒷자리 |
| `paymentMethod.methodType` | String | 결제 수단 유형 |

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
| GET | `/api/feed` | 개인화 피드 조회 (커서 기반) | O |
| GET | `/api/feed/offset` | 개인화 피드 조회 (오프셋 기반) | O |
| GET | `/api/notifications/subscribe` | SSE 구독 (실시간 알림 연결) | O |
| GET | `/api/notifications` | 알림 목록 조회 | O |
| GET | `/api/payment-methods/customer-key` | 고객 키 발급 | O |
| POST | `/api/payment-methods/register` | 결제 수단 등록 | O |
| GET | `/api/payment-methods` | 결제 수단 목록 조회 | O |
| DELETE | `/api/payment-methods/{methodId}` | 결제 수단 삭제 | O |
| PATCH | `/api/payment-methods/{methodId}/default` | 기본 결제 수단 변경 | O |
| POST | `/api/subscriptions/start` | 구독 시작 | O |
| GET | `/api/subscriptions/me` | 내 구독 조회 | O |
| POST | `/api/subscriptions/cancel` | 구독 해지 | O |
| POST | `/api/subscriptions/resume` | 구독 재개 | O |
| POST | `/api/subscriptions/refund` | 구독 즉시 취소 (환불) | O |
| GET | `/api/payment-history` | 결제 내역 조회 | O |
