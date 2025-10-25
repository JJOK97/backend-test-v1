package im.bigs.pg.external.pg

import java.security.MessageDigest
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class AesGcmEncryption(
    private val apiKey: String,
    private val ivBase64Url: String,
) {
    private val cipher = Cipher.getInstance("AES/GCM/NoPadding")

    fun encrypt(plainText: String): String {
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