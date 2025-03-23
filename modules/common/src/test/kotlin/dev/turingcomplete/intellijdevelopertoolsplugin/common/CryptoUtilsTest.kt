package dev.turingcomplete.intellijdevelopertoolsplugin.common

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatCode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.security.spec.PKCS8EncodedKeySpec

class CryptoUtilsTest {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  @ParameterizedTest
  @MethodSource("validPrivateKeys")
  fun `parsing of valid PEM formatted key`(validPemKey: String) {
    val result = CryptoUtils.toRkcs8Key(validPemKey)
    assertThat(result).isNotNull
    assertThat(result).isInstanceOf(PKCS8EncodedKeySpec::class.java)
  }

  @ParameterizedTest
  @MethodSource("invalidPrivateKeys")
  fun `parsing of invalid PEM formatted keys`(invalidPemKey: String) {
    assertThatCode {
      CryptoUtils.toRkcs8Key(invalidPemKey)
    }.doesNotThrowAnyException()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    @JvmStatic
    fun validPrivateKeys(): Collection<Arguments> =
      listOf(
        """
            -----BEGIN PRIVATE KEY-----
            MIIBVwIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEAzPj2X1dyZzDmqT/W
            ZcdQF9i9vL+UN65wX5i2+Zm3B4TFjQtqYSbdTkCGhPrTfKZ1mH6fHnCqJce5Y4zD
            UQIDAQABAkAcQ0hOM2j1dLD+Rl4nQUcxdLKHykHpeNkKccJcMqRm7C9P0hxPjTvV
            cxMEk5u1bJJNV9IpTey4PPZ0ddOmFGAiAiEA8z3ibztuj9HbW1vJZTZUB3W3uyhH
            6uv3g9jPH0FcV00CIQDNzFqJk2ql+0N+2/tHkD3A0P3AUKPd0QPoRBDeyQ==
            -----END PRIVATE KEY-----
        """.trimIndent(),
        """
            -----BEGIN PRIVATE KEY-----
            MIIBVwIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEAzPj2X1dyZzDmqT/W
            
            ZcdQF9i9vL+UN65wX5i2+Zm3B4TFjQtqYSbdTkCGhPrTfKZ1mH6fHnCqJce5Y4zD
            
            UQIDAQABAkAcQ0hOM2j1dLD+Rl4nQUcxdLKHykHpeNkKccJcMqRm7C9P0hxPjTvV
            -----END PRIVATE KEY-----
        """.trimIndent(),
        "MIIBVwIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEAzPj2X1dyZzDmqT/W",
        "MIIBVwIBADANBgkqhkiG9w0BAQEFAASCAT8wggE7AgEAAkEAzPj2X1dyZzDmqT/WZcdQF9i9vL+UN65wX5i2+Zm3B4TFjQtqYSbdTkCGhPrTfKZ1mH6fHnCqJce5Y4zDUQIDAQABAkAcQ0hOM2j1dLD+Rl4nQUcxdLKHykHpeNkKccJcMqRm7C9P0hxPjTvV"
      ).map { Arguments.of(it) }

    @JvmStatic
    fun invalidPrivateKeys(): Collection<Arguments> =
      listOf(
        """
            -----BEGIN PRIVATE KEY-----
            InvalidBase64Content
            -----END PRIVATE KEY-----
        """.trimIndent(),
        "InvalidBase64Content",
        ""
      ).map { Arguments.of(it) }
  }
}
