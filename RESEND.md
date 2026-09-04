# Resend 로 `noreply@example.com` 발송하기

이 앱은 `MAIL_FROM` 에 넣은 주소를 그대로 From 헤더에 넣는다. **코드는 아무 주소나 받는다.**
막는 쪽은 메일 서버다.

Gmail SMTP 는 발신 계정이 소유를 증명한 주소가 아니면 From 을 계정 주소로 조용히 덮어쓴다.
그런데 Gmail 의 "다른 주소에서 메일 보내기"는 그 주소로 **확인 코드**를 보내 소유를 증명받는다.
메일함이 없는 `noreply@` 는 코드를 받을 수 없으니 이 경로로는 등록할 수 없다.

**Resend 는 메일함이 아니라 DNS 로 도메인 소유를 증명한다.** 그래서 `noreply@example.com` 처럼
받는 메일함이 아예 없는 주소도 발신 주소로 쓸 수 있다. DKIM 도 `example.com` 키로 서명되니
스팸 판정도 Gmail 을 우회해 쓰는 것보다 낫다.

준비물은 **example.com 의 DNS 레코드를 편집할 수 있는 권한** 하나뿐이다.

| 항목 | 값 |
| --- | --- |
| 무료 한도 | 3,000통/월, 100통/일 |
| SMTP 호스트 | `smtp.resend.com` |
| 포트 | `587` (STARTTLS) |
| SMTP 사용자 이름 | `resend` (고정 문자열) |
| SMTP 비밀번호 | API 키 (`re_...`) |

이 앱의 **코드는 건드리지 않는다.** 환경 변수만 바꾸면 된다.

---

## 1. 도메인 등록

1. <https://resend.com> 가입 후 대시보드 접속
2. **Domains → Add Domain**
3. Domain 에 `example.com` 입력
   - `send.example.com` 같은 서브도메인으로 등록해도 된다. 서브도메인으로 하면
     루트 도메인의 기존 메일 설정(SPF 등)과 섞이지 않아 더 안전하다.
     대신 발신 주소도 `noreply@send.example.com` 가 된다.
4. **Region** 을 고른다. 가까운 리전이 좋다(예: `ap-northeast-1` 도쿄).
   한 번 정하면 DNS 레코드 값이 그 리전으로 고정된다.

## 2. DNS 레코드 등록

도메인을 추가하면 등록해야 할 레코드가 화면에 나온다. **화면에 나온 값 그대로** DNS 에 넣는다.
리전과 도메인에 따라 값이 달라지므로 아래는 형태를 보여주는 예시다.

| 이름 | 타입 | 값 | 용도 |
| --- | --- | --- | --- |
| `send.example.com` | MX | `feedback-smtp.ap-northeast-1.amazonses.com` (우선순위 `10`) | 반송 처리 |
| `send.example.com` | TXT | `v=spf1 include:amazonses.com ~all` | SPF |
| `resend._domainkey.example.com` | TXT | `p=MIGfMA0GCSqGSIb3DQEB...` | DKIM |

주의할 점 몇 가지.

- DNS 관리 화면에 따라 이름을 **호스트만** 넣어야 한다(`send.example.com` 대신 `send`).
  전체 도메인을 넣으면 `send.example.com.example.com` 가 되어버린다. 등록 후 실제 값을 확인한다.
- 값이 긴 DKIM TXT 는 복사할 때 **줄바꿈이나 공백이 섞이지 않게** 한다.
- Cloudflare 를 쓴다면 이 레코드들은 **프록시(주황 구름)를 끈다.** DNS only 여야 한다.

등록 후 Resend 대시보드에서 **Verify DNS Records** 를 누른다.
상태가 **Verified** 로 바뀌면 끝이다(보통 몇 분, DNS 전파가 느리면 더 걸린다).

## 3. DMARC 레코드 (권장)

없어도 발송은 되지만, 넣어 두면 수신 서버가 더 신뢰한다.

| 이름 | 타입 | 값 |
| --- | --- | --- |
| `_dmarc.example.com` | TXT | `v=DMARC1; p=none; rua=mailto:dmarc@example.com` |

`p=none` 으로 며칠 리포트를 받아 보고 문제가 없으면 `p=quarantine` → `p=reject` 로 올린다.
`rua` 주소는 **실제로 받을 수 있는 주소**여야 한다.

> 루트 도메인에 이미 SPF TXT 가 있다면 **새로 만들지 않는다.** SPF 레코드는 도메인당 하나여야 한다.
> 2번에서 서브도메인(`send.example.com`)에 SPF 를 넣으므로 보통 충돌하지 않는다.

## 4. API 키 발급

**API Keys → Create API Key**

- Name: `supports`
- Permission: **Sending access** (발송만 필요하다)
- Domain: `example.com` 로 제한해 두면 키가 새어도 피해가 작다

`re_` 로 시작하는 키가 **한 번만** 보인다. 이게 SMTP 비밀번호다.

## 5. 환경 변수

```bash
export MAIL_HOST=smtp.resend.com
export MAIL_PORT=587
export MAIL_USERNAME=resend               # 고정 문자열. 계정 이메일이 아니다.
export MAIL_PASSWORD=re_xxxxxxxxxxxx      # 4번에서 받은 API 키
export MAIL_FROM=noreply@example.com         # 인증한 도메인의 주소
export MAIL_FROM_NAME=의견 접수
export MAIL_REPLY_TO=support@example.com     # 회신 받을 실제 주소 (선택)
```

> ⚠️ **`MAIL_FROM` 은 반드시 설정한다.** 비워 두면 앱이 `MAIL_USERNAME` 을 발신 주소로 쓰는데,
> Resend 에서는 그 값이 `resend` 라는 이메일 주소가 아닌 문자열이라 발송에 실패한다.
> (Gmail 을 쓸 때는 `MAIL_USERNAME` 이 곧 메일 주소라 비워 둬도 동작했다.)

> `noreply@` 로 보내면 받는 사람이 회신해도 아무도 못 읽는다.
> 회신을 받을 생각이면 `MAIL_REPLY_TO` 에 실제 있는 주소를 넣는다.

인증한 도메인의 주소라면 `MAIL_FROM` 을 아무거나 써도 된다.
`noreply@`, `support@`, `hello@` 모두 메일함을 따로 만들 필요가 없다.

## 6. 배포 설정 (GitHub Actions)

`.github/workflows/actions.yml` 은 Resend 기준으로 잡혀 있다.
고정값(호스트/포트/사용자 이름)은 워크플로에 그대로 적혀 있고, 나머지만 시크릿으로 받는다.

저장소 **Settings → Secrets and variables → Actions** 에 아래를 등록한다.

| 시크릿 | 값 | 필수 |
| --- | --- | --- |
| `APP_MAIL_PASSWORD` | Resend API 키 (`re_...`) | 필수 |
| `APP_MAIL_FROM` | `noreply@example.com` | 필수 |
| `APP_MAIL_FROM_NAME` | `의견 접수` | 선택 |
| `APP_MAIL_REPLY_TO` | `support@example.com` | 선택 |

> 기존에 Gmail 용으로 쓰던 `APP_MAIL_HOST`, `APP_MAIL_PORT`, `APP_MAIL_USERNAME` 시크릿은
> 더 이상 참조하지 않는다. 지워도 되고 남겨 둬도 무해하다.

## 7. TLS

메일은 기본적으로 **TLS 로 암호화되어 나간다.** 연결 방식은 `MAIL_TLS` 하나로 고른다.

| `MAIL_TLS` | 포트 | 동작 |
| --- | --- | --- |
| `starttls` (기본) | 587 | 평문으로 붙은 뒤 TLS 로 승격. 승격에 실패하면 **발송을 포기한다** |
| `ssl` | 465 | 처음부터 TLS 로 연결(SMTPS) |
| `none` | - | 암호화 없음. **로컬 더미 SMTP 전용** |

이 값 하나에서 JavaMail 속성 세 개가 함께 결정된다.

| `MAIL_TLS` | `ssl.enable` | `starttls.enable` | `starttls.required` |
| --- | --- | --- | --- |
| `starttls` | `false` | `true` | `true` |
| `ssl` | `true` | `false` | `false` |
| `none` | `false` | `false` | `false` |

**셋을 따로 받지 않는 이유**가 있다. `ssl.enable` 과 `starttls.enable` 이 동시에 켜진 모순된 조합,
그리고 더 위험하게는 `starttls.enable` 만 켜고 `required` 를 끈 조합이 만들어지기 때문이다.
후자는 서버가 STARTTLS 를 지원하지 않을 때 예외 없이 **조용히 평문으로 보내버린다.**
`required=true` 면 그 경우 예외가 나고 메일이 나가지 않는다. 그래서 `MailConfig` 가 모드에서
세 값을 함께 파생시킨다.

나머지 TLS 속성은 `application.yml` 에 고정돼 있다.

| 설정 | 값 | 의미 |
| --- | --- | --- |
| `mail.smtp.ssl.protocols` | `TLSv1.2 TLSv1.3` | TLS 1.0/1.1, SSLv3 은 쓰지 않는다 |
| `mail.smtp.ssl.checkserveridentity` | `true` | 인증서의 호스트명까지 검증한다 |
| `mail.smtp.auth` | `${MAIL_AUTH:true}` | SMTP 인증. 로컬 더미 SMTP 에서만 `false` |

기동할 때 어떤 모드로 잡혔는지 로그로 남는다.

```
SMTP TLS mode: STARTTLS (smtp.resend.com:587)
```

`none` 이면 경고로 남는다.

```
SMTP TLS is disabled (app.mail.tls=NONE). Mail will be sent in plain text - local use only.
```

`checkserveridentity` 는 인증서가 유효한지뿐 아니라 **접속한 호스트명과 일치하는지**까지 본다.
이게 없으면 유효한 아무 인증서나 들이대는 중간자 공격을 막지 못한다.
Angus Mail 2.x 는 기본값이 `true` 지만, 값이 바뀌어도 영향받지 않도록 명시해 뒀다.

> ⚠️ 인증서 오류가 난다고 `mail.smtp.ssl.trust=*` 를 넣지 않는다.
> 그건 검증을 통째로 끄는 설정이라 TLS 를 쓰는 의미가 없어진다.

### 로컬 더미 SMTP (Mailpit 등)

Mailpit, MailHog, smtp4dev 같은 로컬 더미 SMTP 는 **STARTTLS 도 AUTH 도 지원하지 않는다.**
그대로 붙이면 이 에러가 난다.

```
jakarta.mail.MessagingException: STARTTLS is required but host does not support STARTTLS
```

로컬에서만 둘 다 끈다.

```bash
export MAIL_HOST=localhost
export MAIL_PORT=1025
export MAIL_TLS=none                 # 암호화 끄기
export MAIL_AUTH=false               # SMTP 인증 끄기
export MAIL_USERNAME=dev@example.com # 비워두면 발송 자체를 건너뛴다(아래 주의)
export MAIL_FROM=noreply@example.com
```

Mailpit 이면 <http://localhost:8025> 에서 받은 메일을 확인한다.

> ⚠️ **`MAIL_USERNAME` 은 인증을 꺼도 채워야 한다.** `MailService` 가 이 값이 비어 있으면
> "메일 설정이 없다"고 보고 발송을 통째로 건너뛰기 때문이다(로그: `Mail is not configured,
> skipping done mail.`). 인증을 끄면 이 값은 서버로 전송되지 않으니 아무 값이나 넣어도 된다.
> `MAIL_PASSWORD` 는 비워 둬도 된다.

> ⚠️ `MAIL_TLS=none` 은 **로컬 전용**이다. 운영에 이 값이 들어가면 메일이 평문으로 나간다.
> 배포 워크플로에는 이 변수를 넣지 않았으므로 기본값(`starttls`)이 그대로 적용된다.
> 기동 로그의 경고로도 확인할 수 있다.

### 465 포트로 붙어야 한다면

방화벽에서 587 이 막힌 경우 등에 쓴다. STARTTLS 승격 없이 **처음부터 TLS** 로 연결한다.

```bash
export MAIL_PORT=465
export MAIL_TLS=ssl
```

> 포트와 모드는 함께 바꾼다. `ssl` 인데 587 로 두면 평문으로 여는 서버에 TLS 로 말을 걸어서
> `Unsupported or unrecognized SSL message` 로 실패한다. 반대로 `starttls` 인데 465 면 응답이 없다.

### 확인

Resend 의 587 이 어떤 TLS 로 응답하는지는 이렇게 직접 볼 수 있다.

```bash
openssl s_client -connect smtp.resend.com:587 -starttls smtp \
  -verify_hostname smtp.resend.com -brief </dev/null
```

```
Protocol version: TLSv1.3
Peer certificate: CN=*.resend.com
Verification: OK
Verified peername: *.resend.com
```

`Verification: OK` 와 `Verified peername` 이 나오면 위 설정 그대로 붙는다.

애플리케이션 쪽에서 핸드셰이크까지 보고 싶으면 JVM 옵션에
`-Djavax.net.debug=ssl:handshake` 를 주고 기동한다(로그가 매우 길어지니 확인 후 뺀다).

## 8. 동작 확인

관리자 화면에서 이메일이 있는 의견 하나를 **"처리완료" + 메일 발송 체크** 로 처리한다.

1. 앱 로그에서 발송 성공과 발신 주소를 확인한다.

   ```
   Done mail sent: from=noreply@example.com, to=..., receiptNo=...
   ```

   > 이 로그는 **앱이 넣은 값**이다. 서버가 덮어썼는지는 여기서 알 수 없다.

2. **받은 메일**에서 실제 결과를 본다. Gmail 이면 메일 우측 **⋮ → 원본 보기**.

   ```
   SPF:    PASS  (example.com)
   DKIM:   PASS  (example.com)
   DMARC:  PASS
   ```

   세 줄이 모두 `PASS` 이고 괄호 안 도메인이 `example.com` 면 제대로 된 것이다.

3. Resend 대시보드의 **Emails** 탭에서 발송/전달/반송 기록을 볼 수 있다.
   앱 로그에는 성공으로 찍혔는데 도착하지 않았다면 여기부터 확인한다.

4. 스팸 점수까지 보고 싶으면 <https://www.mail-tester.com> 에 표시된 주소로 한 통 보낸다.

## 문제 해결

| 증상 | 원인 | 조치 |
| --- | --- | --- |
| SMTP `535` 인증 실패 | `MAIL_USERNAME` 에 계정 이메일을 넣음 | 고정 문자열 `resend` 를 넣는다 |
| `The from address is not verified` | 도메인 인증 전이거나 다른 도메인 주소 | 2번 Verified 상태 확인 |
| 발송은 되는데 도착하지 않음 | 반송/스팸 처리 | Resend **Emails** 탭에서 상태 확인 |
| 스팸함으로 감 | DMARC 미설정, 본문 링크 문제 | 3번 DMARC 등록 후 mail-tester 확인 |
| 도메인이 계속 `Pending` | DNS 이름에 도메인이 중복 입력됨 | `send.example.com.example.com` 가 됐는지 확인 |
| 〃 | Cloudflare 프록시 켜짐 | 해당 레코드를 DNS only 로 |
| 연결 타임아웃 | 서버에서 587 아웃바운드 차단 | 방화벽/보안그룹 개방, 또는 7번 465 포트 |
| `STARTTLS is required but host does not support STARTTLS` | 로컬 더미 SMTP 에 붙음 | 7번 `MAIL_TLS=none` |
| `Unsupported or unrecognized SSL message` | 평문 서버에 `MAIL_TLS=ssl` 로 붙음 | 포트와 모드를 맞춘다 |
| `No authentication mechanisms supported by both server and client` | 서버가 AUTH 미지원 | 7번 `MAIL_AUTH=false` |
| `Could not convert socket to TLS` | 서버가 STARTTLS 미지원 / 중간에 TLS 를 가로채는 장비 | 7번 참고. `ssl.trust=*` 로 덮지 않는다 |
| `Mail is not configured, skipping done mail.` | `MAIL_USERNAME` 이 비어 있음 | 5번 환경 변수 확인 |
| 발송 실패인데 원인이 안 보임 | `MAIL_FROM` 미설정 | 5번 경고 참고 |

## 한도

무료 플랜은 **3,000통/월, 100통/일** 이다. 의견 처리 완료 메일 용도로는 넉넉하다.
넘어설 것 같으면 대시보드에서 사용량을 보고 유료 플랜으로 올린다.

---

