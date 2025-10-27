# 빅스페이먼츠 백엔드 사전 과제

### 지원자 정보
- 이름: 옥진석
- 이메일: jsock414@gmail.com
- 연락처: 010-4014-2405

---

### 프로젝트 개요

나노바나나 페이먼츠의 결제 도메인 서버입니다.  
헥사고널 기반 멀티모듈 프로젝트로, 제휴사별 수수료 정책을 적용한 결제 생성 및 커서 기반 페이지네이션을 활용한 결제 조회 기능을 제공합니다.

---

### 개발 목표

**1. 필수 과제**  
- [x] 제휴사별 수수료 정책
  - 하드코드 제거 및 `FeePolicyRepository` 기반 정책 조회
  - 정책에 대한 단위/통합 테스트

- [x] 결제 생성 API (`POST /api/v1/payments`)
  - TestPG 연동 및 승인 처리
  - 제휴사별 수수료 정책(`partner_fee_policy`) 조회 및 적용
  - `effective_from` 기준 최신 정책 자동 선택
  - 수수료/정산금 계산 단위 테스트
  
- [x] 결제 조회 API + 통계 (`GET /api/v1/payments`)
  - 커서 기반 페이지네이션 (`createdAt desc, id desc`)
  - 필터: `partnerId`, `status`, `from`, `to`, `limit`
  - 통계 집계: 필터와 동일한 집합 대상 (`count`, `totalAmount`, `totalNetAmount`)
  - 페이지네이션 단위 테스트

<br>

**2. 선택 과제**

- [x] API 문서화 및 logger 설정
   - Swagger UI에서 전체 API 명세를 확인할 수 있습니다.  
   - http://localhost:8080/swagger-ui.html  
   - `build` 실행 시 logger를 확인할 수 있습니다.
   
- [x] 외부 DB로 전환  
   - MariaDB와 flyway로 마이그레이션 진행

- [x] 추가 제휴사 연동  
   - PG Client 추가에 대한 전략 수립

<br>

**3. 기타**

- [] 멀티 모듈과 헥사고널에 대한 학습
- [] Kotlin + Spring과 Java + Spring 비교 학습
- [] GCM 및 IV 학습

---

### 실행 방법

**기술 스택**
   - Language : Kotlin 1.9.25
   - Framework : Spring Boot 3.4.4
   - JVM : Java 21
   - Database : MariaDB 11.2
   - Migration : Flyway
   - ORM : JPA/Hibernate
   - Build : Gradle 8.14 (Kotlin DSL)
   - Container : Docker & Docker Compose
   - Library : Spring Data JPA, Jackson, SpringDoc OpenAPI
   - Logging : SLF4J
   - Test : JUnit 5, Mockk, Testcontainers

<br>

**빌드 및 실행**
```bash
# ktlint 포멧 실행
./gradlew ktlintFormat

# 빌드 및 테스트
./gradlew build

# 도커 데스크탑 등 실행
# MariaDB 컨테이너 시작
docker-compose up -d mariadb

# 애플리케이션 실행
./gradlew :modules:bootstrap:api-payment-gateway:bootRun
```

<br>

**테스트**

1. 도메인 계층
```bash
- 수수료 계산 로직 검증
./gradlew :modules:domain:test --tests FeeCalculatorTest
```

2. 애플리케이션 계층
```bash
- PaymentServiceTest: 결제 생성, 수수료 정책 적용, 예외 처리
./gradlew :modules:application:test --tests PaymentServiceTest

- QueryPaymentsServiceTest: 필터 조회, 커서 페이지네이션, 통계 집계
./gradlew :modules:application:test --tests QueryPaymentsServiceTest
```

3. 인프라 계층
```bash
- FeePolicyEffectiveDateTest: `effective_from` 기준 최신 정책 선택
./gradlew :modules:infrastructure:persistence:test --tests FeePolicyEffectiveDateTest

- PaymentRepositoryPagingTest: 커서 기반 페이징 및 통계 일관성
./gradlew :modules:infrastructure:persistence:test --tests PaymentRepositoryPagingTest

- Multi-PG Failover
- 우선순위 기반 PG 선택
- Failover 동작 검증
- 비활성화된 PG 스킵
- 매핑 없는 제휴사 예외 처리

./gradlew :modules:infrastructure:persistence:test --tests PgClientSelectorFailoverTest
```
---

### 참고

   - 개발에 필요한 도메인 학습 및 트러블 슈팅은 [wiki](https://github.com/JJOK97/backend-test-v1/wiki)에 정리되어 있습니다.  
   - 기존의 README.md는 help.md로 변경되었습니다.

---

**감사합니다.**
