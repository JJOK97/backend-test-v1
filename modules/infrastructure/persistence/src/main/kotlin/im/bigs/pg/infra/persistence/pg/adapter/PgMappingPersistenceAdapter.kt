package im.bigs.pg.infra.persistence.pg.adapter

import im.bigs.pg.application.pg.port.out.PgMappingOutPort
import im.bigs.pg.domain.pg.PgMapping
import im.bigs.pg.infra.persistence.pg.entity.PgMappingEntity
import im.bigs.pg.infra.persistence.pg.repository.PgMappingJpaRepository
import org.springframework.stereotype.Component

@Component
class PgMappingPersistenceAdapter(
    private val repository: PgMappingJpaRepository,
) : PgMappingOutPort {
    override fun findActiveByPartnerIdOrderByPriority(partnerId: Long): List<PgMapping> {
        return repository.findByPartnerIdAndIsActiveTrueOrderByPriorityAsc(partnerId)
            .map { it.toDomain() }
    }

    private fun PgMappingEntity.toDomain() = PgMapping(
        id = this.id,
        partnerId = this.partnerId,
        pgType = this.pgType,
        priority = this.priority,
        isActive = this.isActive,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt,
    )
}