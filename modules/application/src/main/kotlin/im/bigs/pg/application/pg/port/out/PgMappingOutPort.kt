package im.bigs.pg.application.pg.port.out

import im.bigs.pg.domain.pg.PgMapping

/**
 * Partner-PG 매핑 조회용 출력 포트.
 */
interface PgMappingOutPort {
    /**
     * 제휴사의 활성화된 PG 매핑 목록을 우선순위 순으로 조회합니다.
     *
     * @param partnerId 제휴사 ID
     * @return 우선순위 순으로 정렬된 PG 매핑 목록
     */
    fun findActiveByPartnerIdOrderByPriority(partnerId: Long): List<PgMapping>
}
