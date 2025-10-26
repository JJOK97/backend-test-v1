package im.bigs.pg.infra.persistence

import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.MariaDBContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * MariaDB Testcontainers 기반 통합 테스트를 위한 추상 베이스 클래스
 *
 * 이 클래스를 상속받는 테스트는 독립적인 MariaDB 컨테이너 환경에서 실행됩니다.
 * 테스트마다 격리된 DB 환경을 보장하여 로컬 DB나 다른 테스트에 영향을 주지 않습니다.
 *
 * ### 주요 기능
 *    - MariaDB 11.2 컨테이너 자동 시작/종료
 *    - 테스트용 독립 데이터베이스 생성
 *    - Spring Boot 프로퍼티 동적 주입
 *    - 테스트 실행 시 스키마 자동 생성 및 종료 시 삭제
 *
 * @see Testcontainers
 * @see MariaDBContainer
 */
@Testcontainers
abstract class MariaDbTestBase {
    companion object {
        /**
         * 모든 테스트가 공유하는 MariaDB 컨테이너
         *
         * 같은 테스트 실행 세션 내에서 컨테이너를 재사용하여 시작 시간을 절약합니다.
         * 각 테스트는 create-drop으로 독립적인 스키마 상태를 보장받습니다.
         */
        @Container
        @JvmStatic
        val mariadb = MariaDBContainer("mariadb:11.2").apply {
            withDatabaseName("payment_gateway_test")
            withUsername("test")
            withPassword("test")
        }

        /**
         * Spring Boot 테스트 환경에 MariaDB 컨테이너 연결 정보를 동적으로 주입
         *
         * @param registry Spring의 동적 프로퍼티 레지스트리
         */
        @DynamicPropertySource
        @JvmStatic
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", mariadb::getJdbcUrl)
            registry.add("spring.datasource.username", mariadb::getUsername)
            registry.add("spring.datasource.password", mariadb::getPassword)
            registry.add("spring.jpa.hibernate.ddl-auto") { "create-drop" }
        }
    }
}
