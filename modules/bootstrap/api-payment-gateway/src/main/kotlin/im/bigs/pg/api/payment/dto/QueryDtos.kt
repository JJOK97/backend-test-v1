package im.bigs.pg.api.payment.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

data class QueryResponse(
    @Schema(description = "결제 내역 목록")
    val items: List<PaymentResponse>,
    @Schema(description = "통계 정보")
    val summary: Summary,
    @Schema(description = "다음 페이지 커서", nullable = true)
    val nextCursor: String?,
    @Schema(description = "다음 페이지 존재 여부")
    val hasNext: Boolean,
)

data class Summary(
    @Schema(description = "총 건수", example = "35")
    val count: Long,
    @Schema(description = "총 결제 금액", example = "35000")
    val totalAmount: BigDecimal,
    @Schema(description = "총 정산 금액", example = "33950")
    val totalNetAmount: BigDecimal,
)
