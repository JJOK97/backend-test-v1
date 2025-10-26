package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.port.out.PaymentPage
import im.bigs.pg.application.payment.port.out.PaymentQuery
import im.bigs.pg.application.payment.port.out.PaymentSummaryFilter
import im.bigs.pg.application.payment.port.out.PaymentSummaryProjection
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class QueryPaymentsServiceTest {
    private val paymentOutPort = mockk<PaymentOutPort>()
    private val service = QueryPaymentsService(paymentOutPort)

    @Test
    @DisplayName("필터 없이 전체 결제 목록을 조회해야 한다")
    fun `필터 없이 전체 결제 목록을 조회해야 한다`() {
        // given
        val filter = QueryFilter(
            partnerId = null,
            status = null,
            from = null,
            to = null,
            cursor = null,
            limit = 10
        )

        val payments = listOf(
            createPayment(1L, BigDecimal("10000"), PaymentStatus.APPROVED),
            createPayment(2L, BigDecimal("20000"), PaymentStatus.APPROVED)
        )

        val querySlot = slot<PaymentQuery>()
        every { paymentOutPort.findBy(capture(querySlot)) } returns PaymentPage(
            items = payments,
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null
        )

        val summaryFilterSlot = slot<PaymentSummaryFilter>()
        every { paymentOutPort.summary(capture(summaryFilterSlot)) } returns PaymentSummaryProjection(
            count = 2,
            totalAmount = BigDecimal("30000"),
            totalNetAmount = BigDecimal("29200")
        )

        // when
        val result = service.query(filter)

        // then
        assertEquals(2, result.items.size)
        assertEquals(2, result.summary.count)
        assertEquals(BigDecimal("30000"), result.summary.totalAmount)
        assertEquals(BigDecimal("29200"), result.summary.totalNetAmount)
        assertNull(result.nextCursor)
        assertEquals(false, result.hasNext)

        // 쿼리에는 커서가 null로 전달되어야 함
        assertEquals(null, querySlot.captured.cursorCreatedAt)
        assertEquals(null, querySlot.captured.cursorId)

        // 통계 필터에는 커서가 포함되지 않아야 함
        verify { paymentOutPort.summary(any()) }
    }

    @Test
    @DisplayName("partnerId 필터를 적용하여 조회해야 한다")
    fun `partnerId 필터를 적용하여 조회해야 한다`() {
        // given
        val filter = QueryFilter(
            partnerId = 1L,
            status = null,
            from = null,
            to = null,
            cursor = null,
            limit = 10
        )

        val querySlot = slot<PaymentQuery>()
        every { paymentOutPort.findBy(capture(querySlot)) } returns PaymentPage(
            items = listOf(createPayment(1L, BigDecimal("10000"), PaymentStatus.APPROVED)),
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null
        )

        val summaryFilterSlot = slot<PaymentSummaryFilter>()
        every { paymentOutPort.summary(capture(summaryFilterSlot)) } returns PaymentSummaryProjection(
            count = 1,
            totalAmount = BigDecimal("10000"),
            totalNetAmount = BigDecimal("9600")
        )

        // when
        service.query(filter)

        // then
        assertEquals(1L, querySlot.captured.partnerId)
        assertEquals(1L, summaryFilterSlot.captured.partnerId)
    }

    @Test
    @DisplayName("status 필터를 적용하여 조회해야 한다")
    fun `status 필터를 적용하여 조회해야 한다`() {
        // given
        val filter = QueryFilter(
            partnerId = null,
            status = "APPROVED",
            from = null,
            to = null,
            cursor = null,
            limit = 10
        )

        val querySlot = slot<PaymentQuery>()
        every { paymentOutPort.findBy(capture(querySlot)) } returns PaymentPage(
            items = listOf(createPayment(1L, BigDecimal("10000"), PaymentStatus.APPROVED)),
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null
        )

        val summaryFilterSlot = slot<PaymentSummaryFilter>()
        every { paymentOutPort.summary(capture(summaryFilterSlot)) } returns PaymentSummaryProjection(
            count = 1,
            totalAmount = BigDecimal("10000"),
            totalNetAmount = BigDecimal("9600")
        )

        // when
        service.query(filter)

        // then
        assertEquals(PaymentStatus.APPROVED, querySlot.captured.status)
        assertEquals(PaymentStatus.APPROVED, summaryFilterSlot.captured.status)
    }

    @Test
    @DisplayName("날짜 범위 필터를 적용하여 조회해야 한다")
    fun `날짜 범위 필터를 적용하여 조회해야 한다`() {
        // given
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val to = LocalDateTime.of(2024, 12, 31, 23, 59)

        val filter = QueryFilter(
            partnerId = null,
            status = null,
            from = from,
            to = to,
            cursor = null,
            limit = 10
        )

        val querySlot = slot<PaymentQuery>()
        every { paymentOutPort.findBy(capture(querySlot)) } returns PaymentPage(
            items = listOf(createPayment(1L, BigDecimal("10000"), PaymentStatus.APPROVED)),
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null
        )

        val summaryFilterSlot = slot<PaymentSummaryFilter>()
        every { paymentOutPort.summary(capture(summaryFilterSlot)) } returns PaymentSummaryProjection(
            count = 1,
            totalAmount = BigDecimal("10000"),
            totalNetAmount = BigDecimal("9600")
        )

        // when
        service.query(filter)

        // then
        assertEquals(from, querySlot.captured.from)
        assertEquals(to, querySlot.captured.to)
        assertEquals(from, summaryFilterSlot.captured.from)
        assertEquals(to, summaryFilterSlot.captured.to)
    }

    @Test
    @DisplayName("커서 기반 페이지네이션이 정상 동작해야 한다")
    fun `커서 기반 페이지네이션이 정상 동작해야 한다`() {
        // given
        val cursorCreatedAt = LocalDateTime.of(2024, 1, 1, 12, 0)
        val cursorId = 100L
        val cursor = encodeCursor(cursorCreatedAt, cursorId)

        val filter = QueryFilter(
            partnerId = null,
            status = null,
            from = null,
            to = null,
            cursor = cursor,
            limit = 10
        )

        val nextCreatedAt = LocalDateTime.of(2024, 1, 1, 11, 0)
        val nextId = 90L

        val querySlot = slot<PaymentQuery>()
        every { paymentOutPort.findBy(capture(querySlot)) } returns PaymentPage(
            items = listOf(createPayment(90L, BigDecimal("10000"), PaymentStatus.APPROVED)),
            hasNext = true,
            nextCursorCreatedAt = nextCreatedAt,
            nextCursorId = nextId
        )

        every { paymentOutPort.summary(any()) } returns PaymentSummaryProjection(
            count = 1,
            totalAmount = BigDecimal("10000"),
            totalNetAmount = BigDecimal("9600")
        )

        // when
        val result = service.query(filter)

        // then
        assertEquals(cursorCreatedAt, querySlot.captured.cursorCreatedAt)
        assertEquals(cursorId, querySlot.captured.cursorId)
        assertEquals(true, result.hasNext)
        assertNotNull(result.nextCursor)

        // nextCursor 디코딩 검증
        val decoded = decodeCursor(result.nextCursor!!)
        assertEquals(nextCreatedAt, decoded.first)
        assertEquals(nextId, decoded.second)
    }

    @Test
    @DisplayName("통계는 필터와 동일한 조건으로 집계되고 커서는 제외되어야 한다")
    fun `통계는 필터와 동일한 조건으로 집계되고 커서는 제외되어야 한다`() {
        // given
        val cursor = encodeCursor(LocalDateTime.of(2024, 1, 1, 12, 0), 100L)
        val from = LocalDateTime.of(2024, 1, 1, 0, 0)
        val to = LocalDateTime.of(2024, 12, 31, 23, 59)

        val filter = QueryFilter(
            partnerId = 1L,
            status = "APPROVED",
            from = from,
            to = to,
            cursor = cursor,  // 커서는 통계에서 제외되어야 함
            limit = 10
        )

        every { paymentOutPort.findBy(any()) } returns PaymentPage(
            items = listOf(createPayment(1L, BigDecimal("10000"), PaymentStatus.APPROVED)),
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null
        )

        val summaryFilterSlot = slot<PaymentSummaryFilter>()
        every { paymentOutPort.summary(capture(summaryFilterSlot)) } returns PaymentSummaryProjection(
            count = 5,
            totalAmount = BigDecimal("50000"),
            totalNetAmount = BigDecimal("48000")
        )

        // when
        val result = service.query(filter)

        // then
        val summaryFilter = summaryFilterSlot.captured
        assertEquals(1L, summaryFilter.partnerId, "partnerId는 필터와 동일해야 함")
        assertEquals(PaymentStatus.APPROVED, summaryFilter.status, "status는 필터와 동일해야 함")
        assertEquals(from, summaryFilter.from, "from은 필터와 동일해야 함")
        assertEquals(to, summaryFilter.to, "to는 필터와 동일해야 함")

        // 통계는 커서와 무관하게 전체 집합을 대상으로 해야 함
        assertEquals(5, result.summary.count)
        assertEquals(BigDecimal("50000"), result.summary.totalAmount)
        assertEquals(BigDecimal("48000"), result.summary.totalNetAmount)
    }

    @Test
    @DisplayName("잘못된 커서는 무시되고 null로 처리되어야 한다")
    fun `잘못된 커서는 무시되고 null로 처리되어야 한다`() {
        // given
        val filter = QueryFilter(
            partnerId = null,
            status = null,
            from = null,
            to = null,
            cursor = "invalid-cursor",
            limit = 10
        )

        val querySlot = slot<PaymentQuery>()
        every { paymentOutPort.findBy(capture(querySlot)) } returns PaymentPage(
            items = emptyList(),
            hasNext = false,
            nextCursorCreatedAt = null,
            nextCursorId = null
        )

        every { paymentOutPort.summary(any()) } returns PaymentSummaryProjection(
            count = 0,
            totalAmount = BigDecimal.ZERO,
            totalNetAmount = BigDecimal.ZERO
        )

        // when
        service.query(filter)

        // then
        assertNull(querySlot.captured.cursorCreatedAt)
        assertNull(querySlot.captured.cursorId)
    }

    private fun createPayment(
        id: Long,
        amount: BigDecimal,
        status: PaymentStatus
    ): Payment {
        return Payment(
            id = id,
            partnerId = 1L,
            amount = amount,
            appliedFeeRate = BigDecimal("0.03"),
            feeAmount = amount.multiply(BigDecimal("0.03")).setScale(0, BigDecimal.ROUND_HALF_UP),
            netAmount = amount.subtract(amount.multiply(BigDecimal("0.03")).setScale(0, BigDecimal.ROUND_HALF_UP)),
            cardLast4 = "4242",
            approvalCode = "APPROVAL-123",
            approvedAt = LocalDateTime.ofInstant(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC),
            status = status,
            createdAt = LocalDateTime.ofInstant(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC),
            updatedAt = LocalDateTime.ofInstant(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC)
        )
    }

    private fun encodeCursor(createdAt: LocalDateTime, id: Long): String {
        val raw = "${createdAt.toInstant(ZoneOffset.UTC).toEpochMilli()}:$id"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
    }

    private fun decodeCursor(cursor: String): Pair<LocalDateTime, Long> {
        val raw = String(Base64.getUrlDecoder().decode(cursor))
        val parts = raw.split(":")
        val ts = parts[0].toLong()
        val id = parts[1].toLong()
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC) to id
    }
}
