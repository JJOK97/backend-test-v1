package im.bigs.pg.infra.persistence.pg.repository

import im.bigs.pg.domain.pg.PgType
import im.bigs.pg.infra.persistence.pg.entity.PgMappingEntity
import org.springframework.data.jpa.repository.JpaRepository

interface PgMappingJpaRepository : JpaRepository<PgMappingEntity, Long> {
    /**
     * 제휴사 ID로 활성화된 PG 매핑 목록을 우선순위 오름차순으로 조회합니다.
     *
     * @param partnerId 제휴사 ID
     * @param isActive 활성화 여부
     * @return 우선순위 순으로 정렬된 PG 매핑 목록
     */
    fun findByPartnerIdAndIsActiveTrueOrderByPriorityAsc(partnerId: Long): List<PgMappingEntity>

    /**
     * 제휴사 ID와 PG 타입으로 매핑을 조회합니다.
     *
     * @param partnerId 제휴사 ID
     * @param pgType PG 타입
     * @return 매핑 엔티티 (없으면 null)
     */
    fun findByPartnerIdAndPgType(partnerId: Long, pgType: PgType): PgMappingEntity?
}