package im.bigs.pg.external.pg

import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.pg.PgType
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * DummyPay PG: Failover 테스트를 위한 더미 PG.
 *
 * Failover 시나리오를 테스트하기 위해 추가된 3번째 PG 구현체입니다.
 *
 * - 실제 네트워크 호출 없이 항상 성공 응답
 * - 승인번호 형식: "DUMMY-{timestamp}"
 */
@Component
class DummyPayClient : PgClientOutPort {
    override val type: PgType = PgType.DUMMY_PAY

    override fun supports(partnerId: Long): Boolean = false // DB 기반 매핑에서 관리

    override fun approve(request: PgApproveRequest): PgApproveResult {
        val timestamp = System.currentTimeMillis()
        return PgApproveResult(
            approvalCode = "DUMMY-$timestamp",
            approvedAt = LocalDateTime.now(ZoneOffset.UTC),
            status = PaymentStatus.APPROVED,
        )
    }
}
