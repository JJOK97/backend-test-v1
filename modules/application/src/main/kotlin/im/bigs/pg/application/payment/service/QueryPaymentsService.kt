package im.bigs.pg.application.payment.service

import im.bigs.pg.application.payment.port.`in`.QueryFilter
import im.bigs.pg.application.payment.port.`in`.QueryPaymentsUseCase
import im.bigs.pg.application.payment.port.`in`.QueryResult
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.payment.port.out.PaymentQuery
import im.bigs.pg.application.payment.port.out.PaymentSummaryFilter
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.payment.PaymentSummary
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Base64

/**
 * 결제 이력 조회 유스케이스 구현체.
 *
 * 커서 기반 페이지네이션과 통계 집계를 제공합니다.
 * - 정렬 키: (createdAt DESC, id DESC)
 * - 커서 인코딩: Base64 URL-safe ("createdAtMillis:id")
 * - 통계: 필터와 동일한 집합을 대상으로 계산 (커서/페이징 제외)
 *
 * @property paymentOutPort 결제 영속성 포트
 */
@Service
class QueryPaymentsService(
    private val paymentOutPort: PaymentOutPort,
) : QueryPaymentsUseCase {
    /**
     * 필터를 기반으로 결제 내역을 조회합니다.
     *
     * @param filter 파트너/상태/기간/커서/페이지 크기
     * @return 조회 결과(목록/통계/커서)
     */
    override fun query(filter: QueryFilter): QueryResult {
        // 1. 커서 디코딩
        val (cursorCreatedAt, cursorId) = decodeCursor(filter.cursor)

        // 2. 페이징 조회 (커서 + 필터)
        val query = PaymentQuery(
            partnerId = filter.partnerId,
            status = filter.status?.let { PaymentStatus.valueOf(it) },
            from = filter.from,
            to = filter.to,
            cursorCreatedAt = cursorCreatedAt,
            cursorId = cursorId,
            limit = filter.limit
        )
        val page = paymentOutPort.findBy(query)

        // 3. 통계 조회 (필터만, 커서 제외)
        val summaryFilter = PaymentSummaryFilter(
            partnerId = filter.partnerId,
            status = filter.status?.let { PaymentStatus.valueOf(it) },
            from = filter.from,
            to = filter.to
        )
        val projection = paymentOutPort.summary(summaryFilter)

        // 4. 결과 조합
        return QueryResult(
            items = page.items,
            summary = PaymentSummary(
                count = projection.count,
                totalAmount = projection.totalAmount,
                totalNetAmount = projection.totalNetAmount
            ),
            nextCursor = encodeCursor(page.nextCursorCreatedAt, page.nextCursorId),
            hasNext = page.hasNext
        )
    }

    /**
     * 다음 페이지 이동을 위한 커서 인코딩.
     *
     * 커서 형식: Base64(createdAtMillis:id)
     * 예: "1705314600000:99" → "MTcwNTMxNDYwMDAwMDo5OQ"
     *
     * @param createdAt 마지막 항목의 생성 시각
     * @param id 마지막 항목의 ID
     * @return Base64 인코딩된 커서, null이면 다음 페이지 없음
     */
    private fun encodeCursor(createdAt: LocalDateTime?, id: Long?): String? {
        if (createdAt == null || id == null) return null
        val raw = "${createdAt.toInstant(ZoneOffset.UTC).toEpochMilli()}:$id"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.toByteArray())
    }

    /**
     * 요청으로 전달된 커서 복원.
     *
     * @param cursor Base64 인코딩된 커서 문자열
     * @return (createdAt, id) 쌍, 유효하지 않으면 (null, null)
     */
    private fun decodeCursor(cursor: String?): Pair<LocalDateTime?, Long?> {
        if (cursor.isNullOrBlank()) return null to null
        return try {
            val raw = String(Base64.getUrlDecoder().decode(cursor))
            val parts = raw.split(":")
            val ts = parts[0].toLong()
            val id = parts[1].toLong()
            LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneOffset.UTC) to id
        } catch (e: Exception) {
            null to null
        }
    }
}
