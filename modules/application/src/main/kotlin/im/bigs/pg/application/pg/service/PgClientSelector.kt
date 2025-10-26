package im.bigs.pg.application.pg.service

import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.application.pg.port.out.PgMappingOutPort
import org.springframework.stereotype.Service

/**
 * PG 선택 및 Failover 관리 서비스.
 *
 * 제휴사별 PG 매핑 정보를 기반으로 우선순위에 따라 PG를 선택하고,
 * 실패 시 자동으로 다음 순위 PG로 Failover를 수행합니다.
 *
 * ### 동작 방식
 * 1. DB에서 제휴사의 활성화된 PG 매핑 목록을 우선순위 순으로 조회
 * 2. 1순위 PG로 결제 승인 시도
 * 3. 실패 시 2순위, 3순위 PG로 자동 Failover
 * 4. 모든 PG 실패 시 예외 발생
 *
 * @property pgMappingRepository PG 매핑 조회 포트
 * @property pgClients 등록된 모든 PG 클라이언트 목록
 */
@Service
class PgClientSelector(
    private val pgMappingRepository: PgMappingOutPort,
    private val pgClients: List<PgClientOutPort>,
) {

    /**
     * 제휴사의 PG 매핑에 따라 결제 승인을 시도합니다.
     * 실패 시 자동으로 다음 우선순위 PG로 Failover를 수행합니다.
     *
     * @param partnerId 제휴사 ID
     * @param request 결제 승인 요청
     * @return 승인 결과
     * @throws IllegalStateException 사용 가능한 PG가 없거나 모든 PG가 실패한 경우
     */
    fun approveWithFailover(partnerId: Long, request: PgApproveRequest): PgApproveResult {
        // 1. DB에서 제휴사의 PG 매핑 목록 조회 (우선순위 순)
        val mappings = pgMappingRepository.findActiveByPartnerIdOrderByPriority(partnerId)

        if (mappings.isEmpty()) {
            throw IllegalStateException("제휴사 $partnerId 에 대한 PG 매핑이 없습니다")
        }

        // 2. 우선순위대로 PG 선택 및 승인 시도
        val failedPgs = mutableListOf<String>()

        for (mapping in mappings) {
            val pgClient = pgClients.find { it.type == mapping.pgType }

            if (pgClient == null) {
                failedPgs.add("${mapping.pgType}(구현체 없음)")
                continue
            }

            try {
                val result = pgClient.approve(request)
                return result
            } catch (e: Exception) {
                failedPgs.add("${mapping.pgType}(${e.message})")
                // 다음 PG로 계속 진행
            }
        }

        // 3. 모든 PG 실패
        throw IllegalStateException(
            "제휴사 $partnerId 의 모든 PG 승인 실패. 시도한 PG: $failedPgs",
        )
    }

    /**
     * 특정 PG 타입의 클라이언트를 조회합니다.
     *
     * @param partnerId 제휴사 ID (로깅용)
     * @return PG 클라이언트
     * @throws IllegalStateException 사용 가능한 PG가 없는 경우
     */
    fun selectPgClient(partnerId: Long): PgClientOutPort {
        val mappings = pgMappingRepository.findActiveByPartnerIdOrderByPriority(partnerId)

        if (mappings.isEmpty()) {
            throw IllegalStateException("제휴사 $partnerId 에 대한 PG 매핑이 없습니다")
        }

        val firstMapping = mappings.first()
        val pgClient = pgClients.find { it.type == firstMapping.pgType }
            ?: throw IllegalStateException("PG 타입 ${firstMapping.pgType} 에 해당하는 클라이언트가 없습니다")

        return pgClient
    }
}
