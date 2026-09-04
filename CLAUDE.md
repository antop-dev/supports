# CLAUDE.md

이 파일은 Claude Code가 이 저장소에서 작업할 때 참고하는 가이드입니다.

## 프로젝트 개요

- Kotlin + Spring Boot 기반 웹 애플리케이션
- 빌드 도구: Gradle (Kotlin DSL, `build.gradle.kts`)
- Java 17 / Kotlin 2.x / Spring Boot 3.x

## 주요 명령어

```bash
# 클린 빌드
./gradlew clean build
```

## 아키텍처 및 패키지 구조

레이어드 아키텍처를 따른다.

```
src/main/kotlin/ai/antop/supports/
├── controller/    # REST 컨트롤러 (@RestController) — 요청/응답 처리만 담당
├── service/       # 비즈니스 로직 (@Service, @Transactional)
├── repository/    # 데이터 접근 (Spring Data JPA Repository)
├── domain/        # 엔티티, 도메인 모델
├── dto/           # 요청/응답 DTO (data class)
├── config/        # 설정 클래스 (@Configuration)
└── exception/     # 커스텀 예외, @RestControllerAdvice 전역 예외 처리
```

- 의존 방향: `controller → service → repository`. 역방향 의존 금지.
- 컨트롤러에서 엔티티를 직접 반환하지 않는다. 반드시 DTO로 변환해서 반환한다.
- 비즈니스 로직은 서비스 계층에만 둔다. 컨트롤러/리포지토리에 넣지 않는다.

## Kotlin 코딩 컨벤션

- [Kotlin 공식 코딩 컨벤션](https://kotlinlang.org/docs/coding-conventions.html)을 따른다.
- 들여쓰기 4칸, 클래스는 PascalCase, 함수/프로퍼티는 camelCase.
- `var`보다 `val`을 우선 사용. 불변성을 기본으로 한다.
- 널 안전성: `!!` 사용 금지. `?.`, `?:`, `requireNotNull`, `checkNotNull`을 사용한다.
- DTO는 `data class`로 정의한다.
- 빈 주입은 생성자 주입만 사용한다 (`@Autowired` 필드 주입 금지).
- 확장 함수는 적절히 활용하되, 남용하지 않는다 (예: 엔티티 → DTO 매핑).
- 컬렉션 처리는 `map`, `filter` 등 함수형 스타일을 우선한다.
- JPA 엔티티는 `class`로 정의하고 `data class`를 사용하지 않는다 (equals/hashCode 문제).

## Spring Boot 컨벤션

- `@Transactional(readOnly = true)`를 서비스 클래스 기본으로 하고, 쓰기 메서드에만 `@Transactional`을 붙인다.
- 설정 값은 `@ConfigurationProperties` + `data class`로 바인딩한다. `@Value` 남용 금지.
- 예외 처리는 `@RestControllerAdvice`에서 전역으로 처리하고, 일관된 에러 응답 형식을 사용한다.
- 요청 검증은 `spring-boot-starter-validation`(`@Valid`, `@field:NotBlank` 등)을 사용한다.
- API 경로는 복수형 명사 사용: `/api/v1/users`, `/api/v1/orders/{orderId}`.
- 프로파일별 설정: `application.yml` + `application-{profile}.yml` (local, dev, prod).
- 로깅은 SLF4J 사용. `println` 금지.
    
## 테스트

- 테스트는 하지 않는다. 개발자가 수동으로 테스트한다.

## 작업 규칙

- 코드 수정 후 `./gradlew build`가 통과하는지 확인한다.
- 코드 스타일은 `ktlint`을 따른다.
- DB 스키마 변경은 마이그레이션 도구(Flyway/Liquibase)로만 한다. `ddl-auto=update` 사용 금지.
- 시크릿(비밀번호, API 키)을 코드나 설정 파일에 하드코딩하지 않는다. 환경 변수를 사용한다.
- Git 관련 작업은 하지 않는다. 개발자가 수동으로 한다.
