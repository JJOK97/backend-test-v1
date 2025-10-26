package im.bigs.pg.infra.persistence

import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.service.PgClientSelector
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.pg.PgType
import im.bigs.pg.external.pg.DummyPayClient
import im.bigs.pg.external.pg.MockPgClient
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
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * PgClientSelector Failover 동작 통합 테스트.
 *
 * 실제 DB + MockPgClient, DummyPayClient를 사용하여 다음을 검증:
 * 1. 우선순위대로 PG가 선택되는지
 * 2. Failover 동작이 올바른지
 * 3. 매핑이 없을 때 예외가 발생하는지
 */
@DataJpaTest
@ContextConfiguration(
    classes = [
        JpaConfig::class,
        PgMappingPersistenceAdapter::class,
        PgClientSelector::class,
        MockPgClient::class,
        DummyPayClient::class,
    ],
)
class PgClientSelectorFailoverTest @Autowired constructor(
    val pgMappingRepository: PgMappingJpaRepository,
    val pgClientSelector: PgClientSelector,
) : MariaDbTestBase() {

    @BeforeEach
    fun setUp() {
        pgMappingRepository.deleteAll()
    }

    @Test
    @DisplayName("우선순위 1순위 PG로 결제 승인이 성공해야 한다")
    fun `우선순위 1순위 PG로 결제 승인이 성공해야 한다`() {
        // Given: Partner 1에 MOCK(1순위), DUMMY_PAY(2순위) 매핑
        val partnerId = 1L
        pgMappingRepository.save(
            PgMappingEntity(partnerId = partnerId, pgType = PgType.MOCK, priority = 1, isActive = true),
        )
        pgMappingRepository.save(
            PgMappingEntity(partnerId = partnerId, pgType = PgType.DUMMY_PAY, priority = 2, isActive = true),
        )

        val request = PgApproveRequest(
            partnerId = partnerId,
            amount = BigDecimal("10000"),
            cardBin = "123456",
            cardLast4 = "4242",
            productName = "테스트 상품",
        )

        // When: 결제 승인 시도
        val result = pgClientSelector.approveWithFailover(partnerId, request)

        // Then: MOCK PG(1순위)로 승인 성공
        assertEquals(PaymentStatus.APPROVED, result.status)
        assertTrue(result.approvalCode.matches(Regex("\\d{8}"))) // MMdd#### 형식
    }

    @Test
    @DisplayName("우선순위대로 여러 PG가 정렬되어 조회되어야 한다")
    fun `우선순위대로 여러 PG가 정렬되어 조회되어야 한다`() {
        // Given: Partner 1에 2개의 PG 매핑 (역순으로 저장)
        val partnerId = 1L
        pgMappingRepository.save(
            PgMappingEntity(partnerId = partnerId, pgType = PgType.DUMMY_PAY, priority = 2, isActive = true),
        )
        pgMappingRepository.save(
            PgMappingEntity(partnerId = partnerId, pgType = PgType.MOCK, priority = 1, isActive = true),
        )

        val request = PgApproveRequest(
            partnerId = partnerId,
            amount = BigDecimal("10000"),
            cardBin = "123456",
            cardLast4 = "4242",
            productName = "테스트 상품",
        )

        // When: 결제 승인 시도
        val result = pgClientSelector.approveWithFailover(partnerId, request)

        // Then: 1순위 MOCK PG로 승인 성공
        assertEquals(PaymentStatus.APPROVED, result.status)
        assertTrue(result.approvalCode.matches(Regex("\\d{8}"))) // MMdd#### 형식
    }

    @Test
    @DisplayName("PG 매핑이 없는 제휴사는 예외가 발생해야 한다")
    fun `PG 매핑이 없는 제휴사는 예외가 발생해야 한다`() {
        // Given: 매핑이 없는 제휴사
        val partnerId = 999L

        val request = PgApproveRequest(
            partnerId = partnerId,
            amount = BigDecimal("10000"),
            cardBin = "123456",
            cardLast4 = "4242",
            productName = "테스트 상품",
        )

        // When & Then: 예외 발생
        val exception = assertFailsWith<IllegalStateException> {
            pgClientSelector.approveWithFailover(partnerId, request)
        }
        assertEquals("제휴사 $partnerId 에 대한 PG 매핑이 없습니다", exception.message)
    }

    @Test
    @DisplayName("비활성화된 PG 매핑은 사용되지 않아야 한다")
    fun `비활성화된 PG 매핑은 사용되지 않아야 한다`() {
        // Given: Partner 1에 비활성화된 MOCK(1순위), 활성화된 DUMMY_PAY(2순위)
        val partnerId = 1L
        pgMappingRepository.save(
            PgMappingEntity(partnerId = partnerId, pgType = PgType.MOCK, priority = 1, isActive = false),
        )
        pgMappingRepository.save(
            PgMappingEntity(partnerId = partnerId, pgType = PgType.DUMMY_PAY, priority = 2, isActive = true),
        )

        val request = PgApproveRequest(
            partnerId = partnerId,
            amount = BigDecimal("10000"),
            cardBin = "123456",
            cardLast4 = "4242",
            productName = "테스트 상품",
        )

        // When: 결제 승인 시도
        val result = pgClientSelector.approveWithFailover(partnerId, request)

        // Then: 비활성화된 1순위는 건너뛰고, 활성화된 2순위 DUMMY_PAY로 승인
        assertEquals(PaymentStatus.APPROVED, result.status)
        assertTrue(result.approvalCode.startsWith("DUMMY-"))
    }
}