package im.bigs.pg.infra.persistence

import im.bigs.pg.domain.pg.PgType
import im.bigs.pg.infra.persistence.config.JpaConfig
import im.bigs.pg.infra.persistence.pg.adapter.PgMappingPersistenceAdapter
import im.bigs.pg.infra.persistence.pg.entity.PgMappingEntity
import im.bigs.pg.infra.persistence.pg.repository.PgMappingJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PgMapping 리포지토리 통합 테스트.
 *
 * - 우선순위 기반 정렬 검증
 * - 활성화 필터링 검증
 * - Failover를 위한 복수 PG 매핑 조회 검증
 */
@DataJpaTest
@ContextConfiguration(classes = [JpaConfig::class, PgMappingPersistenceAdapter::class])
class PgMappingRepositoryTest @Autowired constructor(
    val adapter: PgMappingPersistenceAdapter,
    val repository: PgMappingJpaRepository,
) : MariaDbTestBase() {

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }

    @Test
    @DisplayName("제휴사의 활성화된 PG 매핑을 우선순위 순으로 조회해야 한다")
    fun `제휴사의 활성화된 PG 매핑을 우선순위 순으로 조회해야 한다`() {
        // Given: Partner 1에 3개의 PG 매핑 (우선순위: 2, 1, 3)
        val partnerId = 1L

        repository.save(
            PgMappingEntity(
                partnerId = partnerId,
                pgType = PgType.TEST_PG,
                priority = 2,
                isActive = true,
            ),
        )

        repository.save(
            PgMappingEntity(
                partnerId = partnerId,
                pgType = PgType.MOCK,
                priority = 1,
                isActive = true,
            ),
        )

        repository.save(
            PgMappingEntity(
                partnerId = partnerId,
                pgType = PgType.DUMMY_PAY,
                priority = 3,
                isActive = true,
            ),
        )

        // When: 우선순위 순으로 조회
        val mappings = adapter.findActiveByPartnerIdOrderByPriority(partnerId)

        // Then: 1, 2, 3 순서로 정렬되어야 함
        assertEquals(3, mappings.size)
        assertEquals(PgType.MOCK, mappings[0].pgType) // priority 1
        assertEquals(PgType.TEST_PG, mappings[1].pgType) // priority 2
        assertEquals(PgType.DUMMY_PAY, mappings[2].pgType) // priority 3
    }

    @Test
    @DisplayName("비활성화된 PG 매핑은 조회되지 않아야 한다")
    fun `비활성화된 PG 매핑은 조회되지 않아야 한다`() {
        // Given: Partner 1에 활성화 1개, 비활성화 1개
        val partnerId = 1L

        repository.save(
            PgMappingEntity(
                partnerId = partnerId,
                pgType = PgType.MOCK,
                priority = 1,
                isActive = true,
            ),
        )

        repository.save(
            PgMappingEntity(
                partnerId = partnerId,
                pgType = PgType.TEST_PG,
                priority = 2,
                isActive = false, // 비활성화
            ),
        )

        // When: 활성화된 매핑만 조회
        val mappings = adapter.findActiveByPartnerIdOrderByPriority(partnerId)

        // Then: 활성화된 1개만 조회되어야 함
        assertEquals(1, mappings.size)
        assertEquals(PgType.MOCK, mappings[0].pgType)
    }

    @Test
    @DisplayName("매핑이 없는 제휴사는 빈 리스트를 반환해야 한다")
    fun `매핑이 없는 제휴사는 빈 리스트를 반환해야 한다`() {
        // Given: 매핑이 전혀 없는 제휴사
        val partnerId = 999L

        // When: 조회
        val mappings = adapter.findActiveByPartnerIdOrderByPriority(partnerId)

        // Then: 빈 리스트 반환
        assertTrue(mappings.isEmpty())
    }

    @Test
    @DisplayName("다른 제휴사의 매핑은 조회되지 않아야 한다")
    fun `다른 제휴사의 매핑은 조회되지 않아야 한다`() {
        // Given: Partner 1과 Partner 2에 각각 매핑 존재
        repository.save(
            PgMappingEntity(
                partnerId = 1L,
                pgType = PgType.MOCK,
                priority = 1,
                isActive = true,
            ),
        )

        repository.save(
            PgMappingEntity(
                partnerId = 2L,
                pgType = PgType.TEST_PG,
                priority = 1,
                isActive = true,
            ),
        )

        // When: Partner 1의 매핑만 조회
        val mappings = adapter.findActiveByPartnerIdOrderByPriority(1L)

        // Then: Partner 1의 매핑만 조회되어야 함
        assertEquals(1, mappings.size)
        assertEquals(PgType.MOCK, mappings[0].pgType)
    }
}
