package im.bigs.pg.api.config

import im.bigs.pg.domain.pg.PgType
import im.bigs.pg.infra.persistence.partner.entity.FeePolicyEntity
import im.bigs.pg.infra.persistence.partner.entity.PartnerEntity
import im.bigs.pg.infra.persistence.partner.repository.FeePolicyJpaRepository
import im.bigs.pg.infra.persistence.partner.repository.PartnerJpaRepository
import im.bigs.pg.infra.persistence.pg.entity.PgMappingEntity
import im.bigs.pg.infra.persistence.pg.repository.PgMappingJpaRepository
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.math.BigDecimal
import java.time.Instant

/**
 * 로컬/데모 환경에서 빠른 실행을 위한 간단한 시드 데이터.
 * - 운영 환경에서는 제거하거나 마이그레이션 도구로 대체합니다.
 */
@Configuration
class DataInitializer {
    @Bean
    fun seed(
        partnerRepo: PartnerJpaRepository,
        feeRepo: FeePolicyJpaRepository,
        pgMappingRepo: PgMappingJpaRepository,
    ) = CommandLineRunner {
        if (partnerRepo.count() == 0L) {
            val p1 = partnerRepo.save(PartnerEntity(code = "MOCK1", name = "Mock Partner 1", active = true))
            val p2 = partnerRepo.save(PartnerEntity(code = "TESTPAY1", name = "TestPay Partner 1", active = true))

            feeRepo.save(
                FeePolicyEntity(
                    partnerId = p1.id!!,
                    effectiveFrom = Instant.parse("2020-01-01T00:00:00Z"),
                    percentage = BigDecimal("0.0235"),
                    fixedFee = BigDecimal.ZERO,
                ),
            )
            feeRepo.save(
                FeePolicyEntity(
                    partnerId = p2.id!!,
                    effectiveFrom = Instant.parse("2020-01-01T00:00:00Z"),
                    percentage = BigDecimal("0.0300"),
                    fixedFee = BigDecimal("100"),
                ),
            )

            // PG 매핑 시드 데이터
            // Partner 1: MOCK(1순위), TEST_PG(2순위)
            pgMappingRepo.save(
                PgMappingEntity(
                    partnerId = p1.id!!,
                    pgType = PgType.MOCK,
                    priority = 1,
                    isActive = true,
                ),
            )
            pgMappingRepo.save(
                PgMappingEntity(
                    partnerId = p1.id!!,
                    pgType = PgType.TEST_PG,
                    priority = 2,
                    isActive = true,
                ),
            )

            // Partner 2: TEST_PG(1순위), DUMMY_PAY(2순위)
            pgMappingRepo.save(
                PgMappingEntity(
                    partnerId = p2.id!!,
                    pgType = PgType.TEST_PG,
                    priority = 1,
                    isActive = true,
                ),
            )
            pgMappingRepo.save(
                PgMappingEntity(
                    partnerId = p2.id!!,
                    pgType = PgType.DUMMY_PAY,
                    priority = 2,
                    isActive = true,
                ),
            )
        }
    }
}
