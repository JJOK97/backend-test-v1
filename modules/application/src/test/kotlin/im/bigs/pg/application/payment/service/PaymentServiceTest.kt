package im.bigs.pg.application.payment.service

import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.service.PgClientSelector
import im.bigs.pg.domain.partner.FeePolicy
import im.bigs.pg.domain.partner.Partner
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.DisplayName
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import io.mockk.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class PaymentServiceTest {
    private val partnerRepo = mockk<PartnerOutPort>()
    private val feeRepo = mockk<FeePolicyOutPort>()
    private val paymentRepo = mockk<PaymentOutPort>()
    private val pgClientSelector = mockk<PgClientSelector>()

    @Test
    @DisplayName("결제 시 수수료 정책을 적용하고 저장해야 한다")
    fun `결제 시 수수료 정책을 적용하고 저장해야 한다`() {
        val service = PaymentService(partnerRepo, feeRepo, paymentRepo, pgClientSelector)
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", true)
        every { pgClientSelector.approveWithFailover(1L, any()) } returns PgApproveResult(
            "APPROVAL-123",
            LocalDateTime.of(2024, 1, 1, 0, 0),
            PaymentStatus.APPROVED
        )
        every { feeRepo.findEffectivePolicy(1L, any()) } returns FeePolicy(
            id = 10L, partnerId = 1L, effectiveFrom = LocalDateTime.ofInstant(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC),
            percentage = BigDecimal("0.0300"), fixedFee = BigDecimal("100")
        )
        val savedSlot = slot<Payment>()
        every { paymentRepo.save(capture(savedSlot)) } answers { savedSlot.captured.copy(id = 99L) }

        val cmd = PaymentCommand(partnerId = 1L, amount = BigDecimal("10000"), cardLast4 = "4242")
        val res = service.pay(cmd)

        assertEquals(99L, res.id)
        assertEquals(BigDecimal("400"), res.feeAmount)
        assertEquals(BigDecimal("9600"), res.netAmount)
        assertEquals(PaymentStatus.APPROVED, res.status)
    }

    @Test
    @DisplayName("제휴사가 존재하지 않으면 예외가 발생해야 한다")
    fun `제휴사가 존재하지 않으면 예외가 발생해야 한다`() {
        val service = PaymentService(partnerRepo, feeRepo, paymentRepo, pgClientSelector)
        every { partnerRepo.findById(999L) } returns null

        val cmd = PaymentCommand(partnerId = 999L, amount = BigDecimal("10000"), cardLast4 = "4242")

        assertFailsWith<IllegalArgumentException> {
            service.pay(cmd)
        }
    }

    @Test
    @DisplayName("제휴사가 비활성화된 경우 예외가 발생해야 한다")
    fun `제휴사가 비활성화된 경우 예외가 발생해야 한다`() {
        val service = PaymentService(partnerRepo, feeRepo, paymentRepo, pgClientSelector)
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", active = false)

        val cmd = PaymentCommand(partnerId = 1L, amount = BigDecimal("10000"), cardLast4 = "4242")

        assertFailsWith<IllegalArgumentException> {
            service.pay(cmd)
        }
    }

    @Test
    @DisplayName("수수료 정책이 없는 경우 예외가 발생해야 한다")
    fun `수수료 정책이 없는 경우 예외가 발생해야 한다`() {
        val service = PaymentService(partnerRepo, feeRepo, paymentRepo, pgClientSelector)
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", true)
        every { pgClientSelector.approveWithFailover(1L, any()) } returns PgApproveResult(
            "APPROVAL-123",
            LocalDateTime.of(2024, 1, 1, 0, 0),
            PaymentStatus.APPROVED
        )
        every { feeRepo.findEffectivePolicy(1L, any()) } returns null

        val cmd = PaymentCommand(partnerId = 1L, amount = BigDecimal("10000"), cardLast4 = "4242")

        assertFailsWith<IllegalStateException> {
            service.pay(cmd)
        }
    }

    @Test
    @DisplayName("퍼센트 수수료만 적용 시 계산이 정확해야 한다")
    fun `퍼센트 수수료 적용만 시 계산이 정확해야 한다`() {
        val service = PaymentService(partnerRepo, feeRepo, paymentRepo, pgClientSelector)
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test", true)
        every { pgClientSelector.approveWithFailover(1L, any()) } returns PgApproveResult(
            "APPROVAL-123",
            LocalDateTime.of(2024, 1, 1, 0, 0),
            PaymentStatus.APPROVED
        )
        every { feeRepo.findEffectivePolicy(1L, any()) } returns FeePolicy(
            id = 10L, partnerId = 1L, effectiveFrom = LocalDateTime.ofInstant(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC),
            percentage = BigDecimal("0.0235"), fixedFee = null
        )
        val savedSlot = slot<Payment>()
        every { paymentRepo.save(capture(savedSlot)) } answers { savedSlot.captured.copy(id = 99L) }

        val cmd = PaymentCommand(partnerId = 1L, amount = BigDecimal("10000"), cardLast4 = "4242")
        val res = service.pay(cmd)

        assertEquals(BigDecimal("235"), res.feeAmount)
        assertEquals(BigDecimal("9765"), res.netAmount)
    }

    @Test
    @DisplayName("결제 생성 전체 흐름이 정상 동작해야 한다.")
    fun `결제 생성 전체 흐름이 정상 동작해야 한다`() {
        val service = PaymentService(partnerRepo, feeRepo, paymentRepo, pgClientSelector)
        every { partnerRepo.findById(1L) } returns Partner(1L, "TEST", "Test Partner", true)
        every { pgClientSelector.approveWithFailover(1L, any()) } returns PgApproveResult(
            "APPROVAL-123",
            LocalDateTime.of(2024, 1, 1, 0, 0),
            PaymentStatus.APPROVED
        )
        every { feeRepo.findEffectivePolicy(1L, any()) } returns FeePolicy(
            id = 10L, partnerId = 1L,
            effectiveFrom = LocalDateTime.ofInstant(Instant.parse("2020-01-01T00:00:00Z"), ZoneOffset.UTC),
            percentage = BigDecimal("0.0300"), fixedFee = BigDecimal("100")
        )
        val savedSlot = slot<Payment>()
        every { paymentRepo.save(capture(savedSlot)) } answers { savedSlot.captured.copy(id = 99L) }

        val cmd = PaymentCommand(
            partnerId = 1L,
            amount = BigDecimal("10000"),
            cardBin = "123456",
            cardLast4 = "4242",
            productName = "테스트 상품"
        )
        val res = service.pay(cmd)

        // help.md 필수 요구사항 검증: 8개 필드
        assertNotNull(res.id, "결제 ID가 생성되어야 함")
        assertEquals(BigDecimal("10000"), res.amount, "1. 금액")
        assertEquals(BigDecimal("0.0300"), res.appliedFeeRate, "2. 적용 수수료율")
        assertEquals(BigDecimal("400"), res.feeAmount, "3. 수수료")
        assertEquals(BigDecimal("9600"), res.netAmount, "4. 정산금")
        assertEquals("4242", res.cardLast4, "5. 카드 식별(마스킹)")
        assertEquals("APPROVAL-123", res.approvalCode, "6. 승인번호")
        assertNotNull(res.approvedAt, "7. 승인시각")
        assertEquals(PaymentStatus.APPROVED, res.status, "8. 상태")

        // 저장 호출 검증
        verify { paymentRepo.save(any()) }
    }
}
