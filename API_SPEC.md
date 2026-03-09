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
| `contentId` | String | O | 북마크할 콘텐츠 ID (Elasticsearch Document ID) |
| `folderId` | Long | X | 담을 폴더 ID (미지정 시 미분류) |

```json
{
  "contentId": "es_doc_id_abc123",
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
          "contentId": "es_doc_id_abc123",
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

- **응답**:

```json
{
  "status": 200,
  "message": "조회되었습니다.",
  "data": {
    "content": [
      {
        "contentId": "es_document_id",
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
