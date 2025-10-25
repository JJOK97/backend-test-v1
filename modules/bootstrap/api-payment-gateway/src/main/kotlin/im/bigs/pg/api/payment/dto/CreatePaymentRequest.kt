package im.bigs.pg.api.payment.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import java.math.BigDecimal

data class CreatePaymentRequest(
    @Schema(description = "제휴사 ID", example = "1")
    val partnerId: Long,
    @field:Min(1)
    @Schema(description = "결제 금액", example = "10000")
    val amount: BigDecimal,
    @Schema(description = "카드 BIN", example = "123456", nullable = true)
    val cardBin: String? = null,
    @Schema(description = "카드 마지막 4자리", example = "4242", nullable = true)
    val cardLast4: String? = null,
    @Schema(description = "상품명", example = "샘플", nullable = true)
    val productName: String? = null,
)
