package im.bigs.pg.external.pg

import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.pg.PgType
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * DummyPayClient 단위 테스트.
 */
class DummyPayClientTest {
    private val client = DummyPayClient()

    @Test
    @DisplayName("PG 타입은 DUMMY_PAY여야 한다")
    fun `PG 타입은 DUMMY_PAY여야 한다`() {
        assertEquals(PgType.DUMMY_PAY, client.type)
    }

    @Test
    @DisplayName("결제 승인 시 항상 성공 응답을 반환해야 한다")
    fun `결제 승인 시 항상 성공 응답을 반환해야 한다`() {
        // Given
        val request = PgApproveRequest(
            partnerId = 1L,
            amount = BigDecimal("10000"),
            cardBin = "123456",
            cardLast4 = "4242",
            productName = "테스트 상품",
        )

        // When
        val result = client.approve(request)

        // Then
        assertNotNull(result.approvalCode)
        assertTrue(result.approvalCode.startsWith("DUMMY-"))
        assertEquals(PaymentStatus.APPROVED, result.status)
        assertNotNull(result.approvedAt)
    }

    @Test
    @DisplayName("승인번호는 DUMMY- 접두사와 타임스탬프 형식이어야 한다")
    fun `승인번호는 DUMMY- 접두사와 타임스탬프 형식이어야 한다`() {
        // Given
        val request = PgApproveRequest(
            partnerId = 1L,
            amount = BigDecimal("10000"),
            cardBin = "123456",
            cardLast4 = "4242",
            productName = "테스트 상품",
        )

        // When
        val result1 = client.approve(request)
        Thread.sleep(10) // 타임스탬프 차이를 위해
        val result2 = client.approve(request)

        // Then: 각 승인번호가 고유해야 함
        assertTrue(result1.approvalCode.startsWith("DUMMY-"))
        assertTrue(result2.approvalCode.startsWith("DUMMY-"))
        assertTrue(result1.approvalCode != result2.approvalCode)
    }
}