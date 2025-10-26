package im.bigs.pg.application.pg.port.out

import im.bigs.pg.domain.pg.PgType

/**
 * 외부 결제사(PG) 승인 연동 포트.
 *
 * @property type PG 타입 (MOCK, TEST_PG, DUMMY_PAY 등)
 */
interface PgClientOutPort {
    val type: PgType
    fun supports(partnerId: Long): Boolean
    fun approve(request: PgApproveRequest): PgApproveResult
}
