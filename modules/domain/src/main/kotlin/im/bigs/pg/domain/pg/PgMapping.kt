package im.bigs.pg.domain.pg

import java.time.LocalDateTime

/**
 * Partner-PG 매핑 도메인 모델.
 *
 * 제휴사(Partner)가 사용할 PG의 우선순위와 활성화 여부를 관리합니다.
 *
 * @property id 매핑 식별자
 * @property partnerId 제휴사 ID
 * @property pgType PG 타입
 * @property priority 우선순위 (1이 최우선, 숫자가 작을수록 높은 우선순위)
 * @property isActive 활성화 여부
 * @property createdAt 생성 시각
 * @property updatedAt 수정 시각
 */
data class PgMapping(
    val id: Long? = null,
    val partnerId: Long,
    val pgType: PgType,
    val priority: Int,
    val isActive: Boolean = true,
    val createdAt: LocalDateTime? = null,
    val updatedAt: LocalDateTime? = null,
)