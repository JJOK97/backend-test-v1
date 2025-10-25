package im.bigs.pg.external.pg

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Test PG API 클라이언트
 * AES-256-GCM 암호화를 통해 카드 결제 승인을 요청합니다.
 */
@Component
class TestPgClient(
    @Value("\${testpg.api-key}") private val apiKey: String,
    @Value("\${testpg.iv}") private val iv: String,
    @Value("\${testpg.base-url}") private val baseUrl: String,
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
) : PgClientOutPort {

    private val encryption = AesGcmEncryption(apiKey, iv)

    override fun supports(partnerId: Long): Boolean = partnerId % 2L == 0L

    override fun approve(request: PgApproveRequest): PgApproveResult {
        val plainJson = objectMapper.writeValueAsString(
            mapOf(
                "cardNumber" to "1111111111111111",
                "birthDate" to "19900101",
                "expiry" to "1227",
                "password" to "12",
                "amount" to request.amount.toInt(),
            ),
        )

        val encryptedData = encryption.encrypt(plainJson)

        val headers = HttpHeaders().apply {
            contentType = MediaType.APPLICATION_JSON
            set("API-KEY", apiKey)
        }

        val requestBody = mapOf("enc" to encryptedData)
        val httpEntity = HttpEntity(requestBody, headers)

        val response = restTemplate.postForObject(
            "$baseUrl/api/v1/pay/credit-card",
            httpEntity,
            String::class.java,
        ) ?: throw RuntimeException("TestPG API 응답이 null입니다")

        val responseMap: Map<String, Any> = objectMapper.readValue(response)

        return PgApproveResult(
            approvalCode = responseMap["approvalCode"] as String,
            approvedAt = LocalDateTime.parse(
                responseMap["approvedAt"] as String,
                DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            ),
            status = PaymentStatus.valueOf(responseMap["status"] as String),
        )
    }
}