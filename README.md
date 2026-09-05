# 의견 접수 (supports)

의견을 받아 접수/처리하는 웹 애플리케이션.

## 기술 스택

- Kotlin 2.2 (JDK 17), Spring Boot 4
- Thymeleaf, Spring Data JPA, SQLite3
- Flyway (`spring-boot-flyway` 모듈로 자동 설정)
- ULID(PK), BCrypt(단방향 암호화), 비동기 메일(SMTP)
- 사용자 페이지: Material Design 3 + Material Symbols
- 관리자 페이지: Flowbite(Tailwind) + Font Awesome
- 자체 구현 숫자 6자리 캡차(Java2D)
- 국제화(ko 기본 / en / ja / zh), 모든 시각 UTC 저장 → 브라우저 시간대로 표시

## 실행

```bash
./gradlew bootRun
```

프로파일은 설정 파일에 고정하지 않고 실행할 때 정한다.
`bootRun` 은 `local` 로 뜨고(`build.gradle.kts`), 배포는 CI/CD 가 지정한다.

```bash
java -jar app.jar --spring.profiles.active=prd
```

기본 접속: <http://localhost:20002/supports/>  (포트 `20002`, context-path `/supports`)

| 화면 | 주소 |
| --- | --- |
| 사용자 | `/supports/` |
| 관리자 | `/supports/admin` |

SQLite 파일과 첨부 파일은 기본적으로 `./data` 아래에 만들어진다.
스키마는 기동할 때 Flyway 가 적용하므로 따로 준비할 것이 없다.

## 관리자 계정

설정 파일로 만들지 않고 `admin_users` 테이블에 직접 넣는다.
`password_hash` 에는 Spring Security 형식의 접두사가 필요하다(`{bcrypt}$2a$...`).
계정이 하나 생기면 그 뒤로는 관리자 화면의 "계정 관리"에서 추가/삭제할 수 있다.

```sql
-- BCrypt 해시 예: htpasswd -bnBC 10 "" '비밀번호' | tr -d ':\n'
INSERT INTO admin_users (id, username, password_hash, created_at)
VALUES ('01ADMIN000000000000000000', 'admin', '{bcrypt}$2y$10$...', strftime('%s','now') * 1000);
```

## 로컬 개발

**캡차 끄기.** 자동화 테스트처럼 이미지 캡차를 읽을 수 없을 때만 쓴다.

```bash
APP_CAPTCHA_ENABLED=false ./gradlew bootRun
```

**메일 확인.** 로컬 더미 SMTP(Mailpit 등)로 받아 본다.
더미 SMTP 는 STARTTLS 도 인증도 지원하지 않으므로 둘 다 꺼야 한다.

```bash
MAIL_HOST=localhost MAIL_PORT=1025 MAIL_TLS=none MAIL_AUTH=false \
MAIL_USERNAME=dev@example.com MAIL_FROM=noreply@example.com \
./gradlew bootRun
```

Mailpit 이면 <http://localhost:8025> 에서 받은 메일을 확인한다.
`MAIL_USERNAME` 은 인증을 꺼도 채워야 한다. 비어 있으면 발송 자체를 건너뛴다.

## 환경 변수 (운영)

| 변수 | 설명 | 기본값 |
| --- | --- | --- |
| `CONTEXT_PATH` | 컨텍스트 경로 | `/supports` |
| `DB_PATH` | SQLite 파일 경로 | `./data/supports.db` |
| `UPLOAD_DIR` | 첨부 파일 저장 경로 | `./data/uploads` |
| `BASE_URL` | 메일·슬랙에 넣을 절대 URL (예: `https://example.com`) | 요청 기준 자동 |
| `COOKIE_SECURE` | 세션 쿠키에 Secure 플래그. HTTPS 면 `true` | `false` |
| `SLACK_WEBHOOK_URL` | 의견 접수 알림용 Incoming Webhook | - (비면 알림 안 함) |
| `MAIL_HOST` / `MAIL_PORT` | SMTP 서버. 호스트가 비면 메일을 보내지 않는다 | - / `587` |
| `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP 로그인 계정 / 비밀번호 | - |
| `MAIL_FROM` / `MAIL_FROM_NAME` | 받는 사람에게 보이는 발신 주소 / 표시 이름 | SMTP 계정 주소 |
| `MAIL_REPLY_TO` | 회신 주소 | 발신 주소 |
| `MAIL_TLS` | SMTP 암호화: `starttls`(587) / `ssl`(465) / `none`(로컬 더미 전용) | `starttls` |
| `MAIL_AUTH` | SMTP 인증 사용 여부 | `true` |
| `APP_CAPTCHA_ENABLED` | 캡차 검증 사용 여부 | `true` |

`MAIL_HOST` 나 `MAIL_USERNAME` 이 없으면 처리 완료 메일 발송은 조용히 건너뛴다.
처리 완료 메일은 접수자가 접수할 때 "이메일로 답변 받기"를 선택한 경우에만 나간다.

SMTP 설정, `noreply@도메인` 발송, TLS 는 [RESEND.md](RESEND.md) 참고.

## 빌드

```bash
./gradlew clean build   # ktlint 검사 포함
./gradlew ktlintFormat  # 코드 스타일 자동 정리
```

## 배포

`main` 에 푸시하면 GitHub Actions 가 빌드해서 SSH 로 올리고 재기동한다
(`.github/workflows/actions.yml`). 필요한 저장소 시크릿은 다음과 같다.

| 시크릿 | 용도 |
| --- | --- |
| `SSH_HOST` / `SSH_PORT` / `SSH_USERNAME` / `SSH_PASSWORD` | 배포 서버 접속 |
| `SSH_DEPLOY_DIR` | 배포 디렉터리 |
| `APP_DB_PATH` / `APP_UPLOAD_DIR` | `DB_PATH` / `UPLOAD_DIR` |
| `APP_BASE_URL` | `BASE_URL` |
| `APP_MAIL_PASSWORD` | SMTP 비밀번호(Resend API 키) |
| `APP_MAIL_FROM` / `APP_MAIL_FROM_NAME` / `APP_MAIL_REPLY_TO` | 발신 주소 |
| `APP_SLACK_WEBHOOK_URL` | 의견 접수 알림 |
| `SLACK_WEBHOOK_URL` | 배포 결과 알림 |

SMTP 호스트·포트·계정은 고정값이라 워크플로에 직접 적혀 있다.

## 구조

레이어드 아키텍처(`controller → service → repository`). DB 스키마 변경은
`src/main/resources/db/migration` 의 Flyway 마이그레이션으로만 한다.

## SEO / GEO

검색 엔진과 생성형 AI 검색에 **진입 화면만** 노출한다. 사용자가 남긴 의견 내용은 색인하지 않는다.

| 화면 | 색인 |
| --- | --- |
| `/`, `/feedbacks/new`, `/feedbacks/lookup` | 허용 |
| 개별 의견, 접수 완료, 비밀글 확인, 첨부 파일, 오류 화면 | 차단 |
| 관리 화면(`/admin/**`) | 차단 (`meta` + `X-Robots-Tag` 응답 헤더) |

색인 허용 목록은 `web/SeoPaths.kt` 한 곳에만 있다. robots.txt·sitemap.xml·페이지의
`<meta name="robots">` 가 모두 이 값을 본다. 목록에 없으면 **기본이 noindex** 다.

`robots.txt` / `sitemap.xml` / `llms.txt` 는 `SeoController` 가 만들어 내려준다.
도메인이 배포마다 달라 정적 파일로 두지 않고 `BASE_URL` 기준으로 절대 주소를 채운다.

### 앞단(nginx) 설정

크롤러는 **도메인 루트**의 `/robots.txt` 만 읽는다. 이 앱은 context-path(`/supports`)
아래에서 돌기 때문에 앞단에서 연결해 줘야 한다.

```nginx
location = /robots.txt  { proxy_pass http://127.0.0.1:20002/supports/robots.txt; }
location = /sitemap.xml { proxy_pass http://127.0.0.1:20002/supports/sitemap.xml; }
location = /llms.txt    { proxy_pass http://127.0.0.1:20002/supports/llms.txt; }
```

이 설정이 없으면 robots.txt 가 적용되지 않는다. 다만 페이지마다 `meta` 태그로도 막고
있으므로 사용자 의견이 색인되지는 않는다.

`BASE_URL` 을 설정하지 않으면 sitemap 의 주소가 요청 기준으로 만들어져 내부 주소가
그대로 나갈 수 있다. 운영에서는 반드시 지정한다.

## 라이선스

이 프로젝트는 [MIT 라이선스](LICENSE)를 따른다.

`src/main/resources/static/vendor/` 에는 Quill, Grid.js, Flowbite, FilePond,
Tailwind CSS, Font Awesome, Google Fonts 의 배포본을 동봉했다. 각 자산의 라이선스는
[vendor/LICENSES.md](src/main/resources/static/vendor/LICENSES.md) 를 참고한다.
