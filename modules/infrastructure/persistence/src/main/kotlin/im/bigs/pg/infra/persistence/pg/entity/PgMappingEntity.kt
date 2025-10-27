package im.bigs.pg.infra.persistence.pg.entity

import im.bigs.pg.domain.pg.PgType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "partner_pg_mapping")
class PgMappingEntity(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "partner_id", nullable = false)
    val partnerId: Long,

    @Enumerated(EnumType.STRING)
    @Column(name = "pg_type", nullable = false, length = 50)
    val pgType: PgType,

    @Column(name = "priority", nullable = false)
    val priority: Int,

    @Column(name = "is_active", nullable = false)
    val isActive: Boolean = true,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at", nullable = false)
    val updatedAt: LocalDateTime = LocalDateTime.now(),
)
