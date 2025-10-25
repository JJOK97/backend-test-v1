package im.bigs.pg.application.payment.service

import im.bigs.pg.application.partner.port.out.FeePolicyOutPort
import im.bigs.pg.application.partner.port.out.PartnerOutPort
import im.bigs.pg.application.payment.port.`in`.PaymentCommand
import im.bigs.pg.application.payment.port.`in`.PaymentUseCase
import im.bigs.pg.application.payment.port.out.PaymentOutPort
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.calculation.FeeCalculator
import im.bigs.pg.domain.payment.Payment
import im.bigs.pg.domain.payment.PaymentStatus
import org.springframework.stereotype.Service

/**
 * 결제 생성 유스케이스 구현체.
 * - 입력(REST 등) → 도메인/외부PG/영속성 포트를 순차적으로 호출하는 흐름을 담당합니다.
 * - 수수료 정책 조회 및 적용(계산)은 도메인 유틸리티를 통해 수행합니다.
 */
@Service
class PaymentService(
    private val partnerRepository: PartnerOutPort,
    private val feePolicyRepository: FeePolicyOutPort,
    private val paymentRepository: PaymentOutPort,
    private val pgClients: List<PgClientOutPort>,
) : PaymentUseCase {
    /**
     * 결제 승인 요청을 처리하고 제휴사별 수수료 정책을 적용하여 결제를 생성합니다.
     *
     * 처리 흐름 :
     * 1. 제휴사 정보 조회 및 활성화 상태 검증
     * 2. 제휴사에 맞는 PG 클라이언트 선택
     * 3. PG 승인 요청 수행
     * 4. 제휴사별 수수료 정책 조회
     * 5. FeeCalculator로 수수료 및 정산금 계산
     * 6. 결제 정보 저장
     *
     * @param command 결제 요청 정보 (제휴사 ID, 금액, 카드 정보 등)
     * @return 저장된 결제 정보 (수수료, 정산금, 승인번호 포함)
     * @throws IllegalArgumentException 제휴사를 찾을 수 없거나 비활성화된 경우
     * @throws IllegalStateException PG 클라이언트 또는 수수료 정책을 찾을 수 없는 경우
     */
    override fun pay(command: PaymentCommand): Payment {
        val partner = partnerRepository.findById(command.partnerId)
            ?: throw IllegalArgumentException("제휴사를 찾을 수 없습니다: ${command.partnerId}")
        require(partner.active) { "비활성화된 제휴사입니다: ${partner.id}" }

        val pgClient = pgClients.firstOrNull { it.supports(partner.id) }
            ?: throw IllegalStateException("제휴사 ${partner.id}에 대한 PG 클라이언트를 찾을 수 없습니다")

        val approve = pgClient.approve(
            PgApproveRequest(
                partnerId = partner.id,
                amount = command.amount,
                cardBin = command.cardBin,
                cardLast4 = command.cardLast4,
                productName = command.productName,
            ),
        )

        val policy = feePolicyRepository.findEffectivePolicy(partner.id)
            ?: throw IllegalStateException("제휴사 ${partner.id}의 수수료 정책을 찾을 수 없습니다")

        val (fee, net) = FeeCalculator.calculateFee(
            command.amount,
            policy.percentage,
            policy.fixedFee ?: java.math.BigDecimal.ZERO,
        )
        val payment = Payment(
            partnerId = partner.id,
            amount = command.amount,
            appliedFeeRate = policy.percentage,
            feeAmount = fee,
            netAmount = net,
            cardBin = command.cardBin,
            cardLast4 = command.cardLast4,
            approvalCode = approve.approvalCode,
            approvedAt = approve.approvedAt,
            status = PaymentStatus.APPROVED,
        )

        return paymentRepository.save(payment)
    }
}
