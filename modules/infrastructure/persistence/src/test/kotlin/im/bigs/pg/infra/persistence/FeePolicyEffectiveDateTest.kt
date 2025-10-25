package im.bigs.pg.infra.persistence

import im.bigs.pg.infra.persistence.config.JpaConfig
import im.bigs.pg.infra.persistence.partner.adapter.FeePolicyPersistenceAdapter
import im.bigs.pg.infra.persistence.partner.entity.FeePolicyEntity
import im.bigs.pg.infra.persistence.partner.repository.FeePolicyJpaRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.ContextConfiguration
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * effective_from 기준 정책 선택 로직 통합 테스트.
 *
 * FeePolicyPersistenceAdapter의 findEffectivePolicy 메서드를 검증합니다.
 * - 여러 정책 중 가장 최근 정책 선택 (OrderByEffectiveFromDesc)
 * - 미래 정책 제외 (EffectiveFromLessThanEqual)
 * - 경계 조건 (정확히 effective_from과 동일한 시점)
 */
@DataJpaTest
@ContextConfiguration(classes = [JpaConfig::class, FeePolicyPersistenceAdapter::class])
class FeePolicyEffectiveDateTest @Autowired constructor(
    val adapter: FeePolicyPersistenceAdapter,
    val repository: FeePolicyJpaRepository,
) {

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
    }

    @Test
    @DisplayName("여러 정책 중 effective_from 기준 최신 정책을 선택해야 한다")
    fun `여러 정책 중 effective_from 기준 최신 정책을 선택해야 한다`() {
        // Given: 제휴사 2에 3개의 정책이 존재
        val partnerId = 2L

        // 2020-01-01: 3% + 100원 (가장 오래된 정책)
        repository.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = Instant.parse("2020-01-01T00:00:00Z"),
                percentage = BigDecimal("0.0300"),
                fixedFee = BigDecimal("100"),
            ),
        )

        // 2023-01-01: 2.5% + 50원 (중간 정책 - 현재 시점 기준 최신)
        repository.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = Instant.parse("2023-01-01T00:00:00Z"),
                percentage = BigDecimal("0.0250"),
                fixedFee = BigDecimal("50"),
            ),
        )

        // 2025-11-01: 2% + 0원 (미래 정책 - 아직 적용 안 됨)
        repository.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = Instant.parse("2025-11-01T00:00:00Z"),
                percentage = BigDecimal("0.0200"),
                fixedFee = BigDecimal.ZERO,
            ),
        )

        // When: 2024-12-01 시점의 유효한 정책 조회
        val queryTime = LocalDateTime.of(2024, 12, 1, 0, 0, 0)
        val policy = adapter.findEffectivePolicy(partnerId, queryTime)

        // Then: OrderByEffectiveFromDesc 정렬이 제대로 동작하여 2023-01-01 정책 선택
        // (2020보다 2023이 더 최신이므로, 정렬 후 첫 번째 = 2023)
        assertNotNull(policy)
        assertEquals(BigDecimal("0.0250"), policy.percentage)
        assertEquals(BigDecimal("50"), policy.fixedFee)
        assertEquals(
            LocalDateTime.ofInstant(Instant.parse("2023-01-01T00:00:00Z"), ZoneOffset.UTC),
            policy.effectiveFrom,
        )
    }

    @Test
    @DisplayName("미래 정책은 제외하고 현재 시점 기준 최신 정책을 선택해야 한다")
    fun `미래 정책은 제외하고 현재 시점 기준 최신 정책을 선택해야 한다`() {
        // Given: 제휴사 2에 과거 1개, 미래 1개 정책 존재
        val partnerId = 2L

        // 2020-01-01: 3% + 100원 (과거 정책)
        repository.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = Instant.parse("2020-01-01T00:00:00Z"),
                percentage = BigDecimal("0.0300"),
                fixedFee = BigDecimal("100"),
            ),
        )

        // 2026-01-01: 1% + 0원 (미래 정책)
        repository.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = Instant.parse("2026-01-01T00:00:00Z"),
                percentage = BigDecimal("0.0100"),
                fixedFee = BigDecimal.ZERO,
            ),
        )

        // When: 2024-12-01 시점의 유효한 정책 조회
        val queryTime = LocalDateTime.of(2024, 12, 1, 0, 0, 0)
        val policy = adapter.findEffectivePolicy(partnerId, queryTime)

        // Then: EffectiveFromLessThanEqual 필터링이 제대로 동작하여 2020-01-01 정책 선택
        // (2026은 미래이므로 필터링으로 제외됨)
        assertNotNull(policy)
        assertEquals(BigDecimal("0.0300"), policy.percentage)
        assertEquals(BigDecimal("100"), policy.fixedFee)
    }

    @Test
    @DisplayName("모든 정책이 미래인 경우 null을 반환해야 한다")
    fun `모든 정책이 미래인 경우 null을 반환해야 한다`() {
        // Given: 제휴사 2에 미래 정책만 존재
        val partnerId = 2L

        repository.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = Instant.parse("2026-01-01T00:00:00Z"),
                percentage = BigDecimal("0.0200"),
                fixedFee = BigDecimal.ZERO,
            ),
        )

        // When: 2024-12-01 시점의 유효한 정책 조회
        val queryTime = LocalDateTime.of(2024, 12, 1, 0, 0, 0)
        val policy = adapter.findEffectivePolicy(partnerId, queryTime)

        // Then: null 반환
        assertNull(policy)
    }

    @Test
    @DisplayName("정책이 없는 제휴사는 null을 반환해야 한다")
    fun `정책이 없는 제휴사는 null을 반환해야 한다`() {
        // Given: 정책이 전혀 없는 제휴사
        val partnerId = 999L

        // When: 정책 조회
        val queryTime = LocalDateTime.of(2024, 12, 1, 0, 0, 0)
        val policy = adapter.findEffectivePolicy(partnerId, queryTime)

        // Then: null 반환
        assertNull(policy)
    }

    @Test
    @DisplayName("정확히 effective_from과 동일한 시점에도 정책이 적용되어야 한다")
    fun `정확히 effective_from과 동일한 시점에도 정책이 적용되어야 한다`() {
        // Given: 2024-01-01부터 적용되는 정책
        val partnerId = 4L

        repository.save(
            FeePolicyEntity(
                partnerId = partnerId,
                effectiveFrom = Instant.parse("2024-01-01T00:00:00Z"),
                percentage = BigDecimal("0.0250"),
                fixedFee = BigDecimal("50"),
            ),
        )

        // When: 정확히 2024-01-01 00:00:00 시점에 조회
        val queryTime = LocalDateTime.of(2024, 1, 1, 0, 0, 0)
        val policy = adapter.findEffectivePolicy(partnerId, queryTime)

        // Then: 정책이 적용되어야 함 (effective_from <= queryTime)
        assertNotNull(policy)
        assertEquals(BigDecimal("0.0250"), policy.percentage)
    }
}