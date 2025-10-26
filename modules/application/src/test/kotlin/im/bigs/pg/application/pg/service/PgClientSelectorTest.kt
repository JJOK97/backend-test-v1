package im.bigs.pg.application.pg.service

import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.application.pg.port.out.PgMappingOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.pg.PgMapping
import im.bigs.pg.domain.pg.PgType
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.math.BigDecimal
import java.time.LocalDateTime
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * PgClientSelector 단위 테스트.
 *
 * - 우선순위 기반 PG 선택 검증
 * - Failover 시나리오 검증
 * - 예외 상황 처리 검증
 */
class PgClientSelectorTest {
    private val pgMappingRepository = mockk<PgMappingOutPort>()
    private val mockPgClient = mockk<PgClientOutPort>()
    private val testPgClient = mockk<PgClientOutPort>()
    private val dummyPgClient = mockk<PgClientOutPort>()

    private val pgClients = listOf(mockPgClient, testPgClient, dummyPgClient)
    private val selector = PgClientSelector(pgMappingRepository, pgClients)

    private val dummyRequest = PgApproveRequest(
        partnerId = 1L,
        amount = BigDecimal("10000"),
        cardBin = "123456",
        cardLast4 = "4242",
        productName = "테스트",
    )

    private val successResult = PgApproveResult(
        approvalCode = "SUCCESS123",
        approvedAt = LocalDateTime.now(),
        status = PaymentStatus.APPROVED,
    )

    @Test
    @DisplayName("1순위 PG가 성공하면 해당 PG의 결과를 반환해야 한다")
    fun `1순위 PG가 성공하면 해당 PG의 결과를 반환해야 한다`() {
        // Given: Partner 1에 MOCK(1순위), TEST_PG(2순위) 매핑
        every { pgMappingRepository.findActiveByPartnerIdOrderByPriority(1L) } returns listOf(
            PgMapping(id = 1, partnerId = 1L, pgType = PgType.MOCK, priority = 1, isActive = true),
            PgMapping(id = 2, partnerId = 1L, pgType = PgType.TEST_PG, priority = 2, isActive = true),
        )

        every { mockPgClient.type } returns PgType.MOCK
        every { testPgClient.type } returns PgType.TEST_PG
        every { mockPgClient.approve(any()) } returns successResult

        // When: 승인 시도
        val result = selector.approveWithFailover(1L, dummyRequest)

        // Then: MOCK PG의 결과 반환
        assertEquals(successResult, result)
        verify(exactly = 1) { mockPgClient.approve(any()) }
        verify(exactly = 0) { testPgClient.approve(any()) } // 2순위는 호출 안 됨
    }

    @Test
    @DisplayName("1순위 PG 실패 시 2순위 PG로 Failover해야 한다")
    fun `1순위 PG 실패 시 2순위 PG로 Failover해야 한다`() {
        // Given: Partner 1에 MOCK(1순위), TEST_PG(2순위) 매핑
        every { pgMappingRepository.findActiveByPartnerIdOrderByPriority(1L) } returns listOf(
            PgMapping(id = 1, partnerId = 1L, pgType = PgType.MOCK, priority = 1, isActive = true),
            PgMapping(id = 2, partnerId = 1L, pgType = PgType.TEST_PG, priority = 2, isActive = true),
        )

        every { mockPgClient.type } returns PgType.MOCK
        every { testPgClient.type } returns PgType.TEST_PG

        // MOCK PG 실패
        every { mockPgClient.approve(any()) } throws RuntimeException("MOCK PG 장애")
        // TEST_PG 성공
        every { testPgClient.approve(any()) } returns successResult

        // When: 승인 시도
        val result = selector.approveWithFailover(1L, dummyRequest)

        // Then: TEST_PG의 결과 반환 (Failover 성공)
        assertEquals(successResult, result)
        verify(exactly = 1) { mockPgClient.approve(any()) } // 1순위 시도
        verify(exactly = 1) { testPgClient.approve(any()) } // Failover
    }

    @Test
    @DisplayName("모든 PG가 실패하면 예외를 발생시켜야 한다")
    fun `모든 PG가 실패하면 예외를 발생시켜야 한다`() {
        // Given: Partner 1에 MOCK, TEST_PG 매핑
        every { pgMappingRepository.findActiveByPartnerIdOrderByPriority(1L) } returns listOf(
            PgMapping(id = 1, partnerId = 1L, pgType = PgType.MOCK, priority = 1, isActive = true),
            PgMapping(id = 2, partnerId = 1L, pgType = PgType.TEST_PG, priority = 2, isActive = true),
        )

        every { mockPgClient.type } returns PgType.MOCK
        every { testPgClient.type } returns PgType.TEST_PG

        // 모든 PG 실패
        every { mockPgClient.approve(any()) } throws RuntimeException("MOCK 장애")
        every { testPgClient.approve(any()) } throws RuntimeException("TEST_PG 장애")

        // When & Then: 예외 발생
        val exception = assertThrows<IllegalStateException> {
            selector.approveWithFailover(1L, dummyRequest)
        }

        assertTrue(exception.message!!.contains("모든 PG 승인 실패"))
        verify(exactly = 1) { mockPgClient.approve(any()) }
        verify(exactly = 1) { testPgClient.approve(any()) }
    }

    @Test
    @DisplayName("PG 매핑이 없으면 예외를 발생시켜야 한다")
    fun `PG 매핑이 없으면 예외를 발생시켜야 한다`() {
        // Given: 매핑이 없는 제휴사
        every { pgMappingRepository.findActiveByPartnerIdOrderByPriority(999L) } returns emptyList()

        // When & Then: 예외 발생
        val exception = assertThrows<IllegalStateException> {
            selector.approveWithFailover(999L, dummyRequest)
        }

        assertTrue(exception.message!!.contains("PG 매핑이 없습니다"))
    }

    @Test
    @DisplayName("3단계 Failover 시나리오: 1순위, 2순위 실패 후 3순위 성공")
    fun `3단계 Failover 시나리오`() {
        // Given: 3개의 PG 매핑
        every { pgMappingRepository.findActiveByPartnerIdOrderByPriority(1L) } returns listOf(
            PgMapping(id = 1, partnerId = 1L, pgType = PgType.MOCK, priority = 1, isActive = true),
            PgMapping(id = 2, partnerId = 1L, pgType = PgType.TEST_PG, priority = 2, isActive = true),
            PgMapping(id = 3, partnerId = 1L, pgType = PgType.DUMMY_PAY, priority = 3, isActive = true),
        )

        every { mockPgClient.type } returns PgType.MOCK
        every { testPgClient.type } returns PgType.TEST_PG
        every { dummyPgClient.type } returns PgType.DUMMY_PAY

        // 1순위, 2순위 실패
        every { mockPgClient.approve(any()) } throws RuntimeException("MOCK 장애")
        every { testPgClient.approve(any()) } throws RuntimeException("TEST_PG 장애")
        // 3순위 성공
        every { dummyPgClient.approve(any()) } returns successResult

        // When: 승인 시도
        val result = selector.approveWithFailover(1L, dummyRequest)

        // Then: DUMMY_PAY 성공
        assertEquals(successResult, result)
        verify(exactly = 1) { mockPgClient.approve(any()) }
        verify(exactly = 1) { testPgClient.approve(any()) }
        verify(exactly = 1) { dummyPgClient.approve(any()) }
    }

    @Test
    @DisplayName("selectPgClient는 1순위 PG를 반환해야 한다")
    fun `selectPgClient는 1순위 PG를 반환해야 한다`() {
        // Given: Partner 1에 MOCK(1순위) 매핑
        every { pgMappingRepository.findActiveByPartnerIdOrderByPriority(1L) } returns listOf(
            PgMapping(id = 1, partnerId = 1L, pgType = PgType.MOCK, priority = 1, isActive = true),
        )

        every { mockPgClient.type } returns PgType.MOCK

        // When: PG 선택
        val client = selector.selectPgClient(1L)

        // Then: MOCK PG 반환
        assertEquals(mockPgClient, client)
    }
}