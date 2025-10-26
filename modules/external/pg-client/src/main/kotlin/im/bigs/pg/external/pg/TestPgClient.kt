package im.bigs.pg.external.pg

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import im.bigs.pg.application.pg.port.out.PgApproveRequest
import im.bigs.pg.application.pg.port.out.PgApproveResult
import im.bigs.pg.application.pg.port.out.PgClientOutPort
import im.bigs.pg.domain.payment.PaymentStatus
import im.bigs.pg.domain.pg.PgType
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Test PG API 클라이언트 구현체.
 *
 * AES-256-GCM 암호화를 사용하여 카드 결제 승인을 요청합니다.
 * - 현재 기존 MOCK PG API에 설정을 참조하여 짝수 제휴사 ID를 제공하도록 하였습니다. 
 *   추가 과제 진행 시 수정 계획입니다.
 * - 카드 정보는 암호화된 JSON 형식으로 전송됩니다
 *
 * @property apiKey Test PG API 인증 키
 * @property iv AES-GCM 암호화에 사용할 초기화 벡터
 * @property baseUrl Test PG API 베이스 URL
 * @property restTemplate HTTP 요청을 위한 RestTemplate
 * @property objectMapper JSON 직렬화/역직렬화를 위한 ObjectMapper
 */
@Component
class TestPgClient(
    @Value("\${testpg.api-key}") private val apiKey: String,
    @Value("\${testpg.iv}") private val iv: String,
    @Value("\${testpg.base-url}") private val baseUrl: String,
    private val restTemplate: RestTemplate,
    private val objectMapper: ObjectMapper,
) : PgClientOutPort {

    override val type: PgType = PgType.TEST_PG

    private val encryption = AesGcmEncryption(apiKey, iv)

    /**
     * 해당 제휴사 ID를 지원하는지 확인합니다.
     *
     * @param partnerId 제휴사 ID
     * @return 짝수 제휴사 ID인 경우 true, 홀수인 경우 false
     */
    override fun supports(partnerId: Long): Boolean = partnerId % 2L == 0L

    /**
     * 카드 결제 승인을 요청합니다.
     *
     * 카드 정보를 AES-GCM으로 암호화하여 Test PG API에 전송하고,
     * 응답받은 승인 결과를 파싱하여 반환합니다.
     *
     * 현재 TEST PG API가 고정된 cardNumber만을 받기에, 양식에 맞춰 하드코딩 하였습니다.
     *
     * @param request 결제 승인 요청 정보
     * @return 승인 결과 (승인번호, 승인시각, 상태)
     * @throws RuntimeException Test PG API 응답이 null인 경우
     */
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