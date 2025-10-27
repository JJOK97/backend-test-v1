package im.bigs.pg.external.pg

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * AES-256-GCM 암호화 유틸리티.
 *
 * API 키를 SHA-256으로 해싱하여 256비트 AES 키를 생성하고,
 * 주어진 IV로 GCM 암호화를 수행합니다.
 *
 * @property apiKey 암호화에 사용할 API 키 (SHA-256 해싱되어 AES 키로 변환됨)
 * @property ivBase64Url Base64 URL 인코딩된 초기화 벡터 (12바이트 권장)
 */
class AesGcmEncryption(
    private val apiKey: String,
    private val ivBase64Url: String,
) {
    /**
     * 평문을 AES-256-GCM으로 암호화하여 Base64 URL 인코딩 문자열로 반환합니다.
     *
     * @param plainText 암호화할 평문 텍스트
     * @return Base64 URL 인코딩된 암호문
     */
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")

        val keyBytes = MessageDigest.getInstance("SHA-256")
            .digest(apiKey.toByteArray(Charsets.UTF_8))
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val iv = Base64.getUrlDecoder().decode(ivBase64Url)

        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        return Base64.getUrlEncoder().withoutPadding().encodeToString(encrypted)
    }
}