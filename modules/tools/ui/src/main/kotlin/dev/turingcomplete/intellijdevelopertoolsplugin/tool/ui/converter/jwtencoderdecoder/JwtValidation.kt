package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.jwtencoderdecoder

import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.util.ExceptionUtil
import dev.turingcomplete.intellijdevelopertoolsplugin.common.OkHttpClientUtils.applyIntelliJProxySettings
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.decodeBase64String
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.SENSITIVE
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.ObjectMapperService
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import java.security.Key
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.codec.binary.Base32
import org.jose4j.jwk.JsonWebKey
import org.jose4j.jwk.JsonWebKeySet
import org.jose4j.jws.JsonWebSignature
import org.jose4j.keys.HmacKey

internal class JwtValidation(configuration: DeveloperToolConfiguration) {

  private val keySourceName =
    configuration.register("validationKeySource", ValidationKeySource.SECRET.name, CONFIGURATION)
  val keySource =
    ValueProperty(
      runCatching { ValidationKeySource.valueOf(keySourceName.get()) }
        .getOrDefault(ValidationKeySource.SECRET)
    )
  val secret =
    configuration.register("validationSecret", "", SENSITIVE, JwtEncoderDecoder.EXAMPLE_SECRET)
  val publicKey = configuration.register("validationPublicKey", "", SENSITIVE)
  val jwksUrl = configuration.register("validationJwksUrl", "", INPUT)
  val jwksJson = configuration.register("validationJwksJson", "", INPUT)
  val secretEncodingMode =
    configuration.register(
      "validationSecretKeyEncodingMode",
      SecretKeyEncodingMode.RAW,
      CONFIGURATION,
    )
  val strictKeyValidation =
    configuration.register(
      "validationStrictKeyValidation",
      JwtEncoderDecoder.SIGNING_KEY_VALIDATION_DEFAULT,
      CONFIGURATION,
    )

  val tokenMetadata =
    ValueProperty(UiToolsBundle.message("jwt-encoder-decoder.validation.token-metadata.default"))
  val resultState = ValueProperty(ValidationResultState.NONE)
  val validResultMessage = ValueProperty("")
  val invalidResultMessage = ValueProperty("")
  val fetchingJwks = ValueProperty(false)

  init {
    keySource.afterChangeConsumeEvent(null) { event ->
      if (event.valueChanged()) {
        keySourceName.set(event.newValue.name)
      }
    }
  }

  fun fetchJwks(project: Project?, onFetchSuccess: () -> Unit = {}) {
    val url = jwksUrl.get().trim()
    if (url.isBlank()) {
      setInvalidResult(UiToolsBundle.message("jwt-encoder-decoder.validation.fetch-jwks.enter-url"))
      return
    }
    if (fetchingJwks.get()) {
      return
    }

    fetchingJwks.set(true)

    object :
        Task.Backgroundable(
          project,
          UiToolsBundle.message("jwt-encoder-decoder.validation.fetch-jwks.in-progress-title"),
          true,
        ) {
        private lateinit var fetchedJwks: String

        override fun run(indicator: ProgressIndicator) {
          indicator.text =
            UiToolsBundle.message("jwt-encoder-decoder.validation.fetch-jwks.in-progress")

          val httpClient = OkHttpClient.Builder().applyIntelliJProxySettings(url).build()
          val request = Request.Builder().url(url).build()
          httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
              val statusMessage = response.message.ifBlank { response.code.toString() }
              error("HTTP ${response.code}: $statusMessage")
            }
            fetchedJwks = response.body.string()
          }
        }

        override fun onSuccess() {
          jwksJson.set(fetchedJwks)
          onFetchSuccess()
        }

        override fun onThrowable(error: Throwable) {
          setInvalidResult(
            UiToolsBundle.message(
              "jwt-encoder-decoder.validation.fetch-jwks.failed",
              rootCauseMessage(error),
            )
          )
        }

        override fun onFinished() {
          fetchingJwks.set(false)
        }
      }
      .queue()
  }

  fun validate(encodedJwt: String) {
    if (encodedJwt.isBlank()) {
      tokenMetadata.set(
        UiToolsBundle.message("jwt-encoder-decoder.validation.token-metadata.default")
      )
      setNeutralResult()
      return
    }

    try {
      val jwtParts = encodedJwt.split('.', limit = 3)
      if (jwtParts.size < 2) {
        tokenMetadata.set(
          UiToolsBundle.message("jwt-encoder-decoder.validation.token-metadata.unknown")
        )
        setInvalidResult(
          UiToolsBundle.message("jwt-encoder-decoder.validation.invalid-compact-jwt.header-payload")
        )
        return
      }

      val headerNode =
        ObjectMapperService.instance.jsonMapper().readTree(jwtParts[0].decodeBase64String())
      val algorithm =
        headerNode.get("alg")?.asText()?.let { SignatureAlgorithm.findByJwtHeaderValue(it) }
          ?: run {
            tokenMetadata.set(
              UiToolsBundle.message("jwt-encoder-decoder.validation.token-metadata.unknown")
            )
            setInvalidResult(
              UiToolsBundle.message(
                "jwt-encoder-decoder.validation.missing-or-unsupported-alg-header"
              )
            )
            return
          }
      val keyId = headerNode.get("kid")?.asText()?.takeIf { it.isNotBlank() }
      tokenMetadata.set(
        buildString {
          append(
            UiToolsBundle.message(
              "jwt-encoder-decoder.validation.token-metadata.algorithm",
              algorithm.jwtHeaderValue,
            )
          )
          if (keyId != null) {
            append(
              UiToolsBundle.message(
                "jwt-encoder-decoder.validation.token-metadata.kid-suffix",
                keyId,
              )
            )
          }
        }
      )

      if (algorithm == SignatureAlgorithm.NONE) {
        if (jwtParts.getOrElse(2) { "" }.isEmpty()) {
          setValidResult(
            UiToolsBundle.message("jwt-encoder-decoder.validation.valid-unsecured-jwt")
          )
        } else {
          setInvalidResult(
            UiToolsBundle.message("jwt-encoder-decoder.validation.invalid-jwt.alg-none-signature")
          )
        }
        return
      }

      if (jwtParts.size < 3) {
        setInvalidResult(
          UiToolsBundle.message("jwt-encoder-decoder.validation.invalid-compact-jwt.signature")
        )
        return
      }

      val outcome =
        when (keySource.get()) {
          ValidationKeySource.SECRET -> validateWithSecret(encodedJwt, algorithm)
          ValidationKeySource.PUBLIC_KEY -> validateWithPublicKey(encodedJwt, algorithm)
          ValidationKeySource.JWKS -> validateWithJwks(encodedJwt, algorithm, keyId)
        }
      if (outcome.valid) {
        setValidResult(outcome.message)
      } else {
        setInvalidResult(outcome.message)
      }
    } catch (e: Exception) {
      tokenMetadata.set(
        UiToolsBundle.message("jwt-encoder-decoder.validation.token-metadata.unknown")
      )
      setInvalidResult(
        UiToolsBundle.message("jwt-encoder-decoder.validation.failed", rootCauseMessage(e))
      )
    }
  }

  private fun validateWithSecret(
    encodedJwt: String,
    algorithm: SignatureAlgorithm,
  ): ValidationOutcome {
    if (algorithm.kind != SignatureAlgorithmKind.HMAC) {
      return ValidationOutcome(
        UiToolsBundle.message(
          "jwt-encoder-decoder.validation.key-source-mismatch.public-key-or-jwks",
          algorithm.jwtHeaderValue,
        ),
        false,
      )
    }

    val signingKey =
      HmacKey(
        when (secretEncodingMode.get()) {
          SecretKeyEncodingMode.RAW -> secret.get().encodeToByteArray()
          SecretKeyEncodingMode.BASE32 -> Base32().decode(secret.get())
          SecretKeyEncodingMode.BASE64 -> Base64.getDecoder().decode(secret.get())
        }
      )
    return if (verifySignature(encodedJwt, signingKey)) {
      ValidationOutcome(
        UiToolsBundle.message("jwt-encoder-decoder.validation.signature.valid"),
        true,
      )
    } else {
      ValidationOutcome(
        UiToolsBundle.message("jwt-encoder-decoder.validation.signature.invalid"),
        false,
      )
    }
  }

  private fun validateWithPublicKey(
    encodedJwt: String,
    algorithm: SignatureAlgorithm,
  ): ValidationOutcome {
    if (algorithm.kind == SignatureAlgorithmKind.HMAC) {
      return ValidationOutcome(
        UiToolsBundle.message(
          "jwt-encoder-decoder.validation.key-source-mismatch.shared-secret",
          algorithm.jwtHeaderValue,
        ),
        false,
      )
    }

    val keyInput = publicKey.get().trim()
    if (keyInput.isBlank()) {
      return ValidationOutcome(
        UiToolsBundle.message("jwt-encoder-decoder.validation.provide-public-key-or-jwk"),
        false,
      )
    }

    val verificationKey =
      try {
        readVerificationKey(keyInput, algorithm)
      } catch (e: Exception) {
        return ValidationOutcome(
          UiToolsBundle.message(
            "jwt-encoder-decoder.validation.read-public-key-or-jwk.failed",
            rootCauseMessage(e),
          ),
          false,
        )
      }

    return if (verifySignature(encodedJwt, verificationKey)) {
      ValidationOutcome(
        UiToolsBundle.message("jwt-encoder-decoder.validation.signature.valid"),
        true,
      )
    } else {
      ValidationOutcome(
        UiToolsBundle.message("jwt-encoder-decoder.validation.signature.invalid"),
        false,
      )
    }
  }

  private fun validateWithJwks(
    encodedJwt: String,
    algorithm: SignatureAlgorithm,
    keyId: String?,
  ): ValidationOutcome {
    val jwksValue = jwksJson.get().trim()
    if (jwksValue.isBlank()) {
      return ValidationOutcome(
        UiToolsBundle.message("jwt-encoder-decoder.validation.provide-jwks-json"),
        false,
      )
    }

    val jwks =
      try {
        JsonWebKeySet(jwksValue)
      } catch (e: Exception) {
        return ValidationOutcome(
          UiToolsBundle.message(
            "jwt-encoder-decoder.validation.parse-jwks.failed",
            rootCauseMessage(e),
          ),
          false,
        )
      }

    val candidateKeys =
      jwks.jsonWebKeys.filter {
        (keyId == null || keyId == it.keyId) && isJwkCompatibleWithAlgorithm(it, algorithm)
      }

    if (candidateKeys.isEmpty()) {
      return if (keyId != null) {
        ValidationOutcome(
          UiToolsBundle.message("jwt-encoder-decoder.validation.no-compatible-key-with-kid", keyId),
          false,
        )
      } else {
        ValidationOutcome(
          UiToolsBundle.message("jwt-encoder-decoder.validation.no-compatible-key"),
          false,
        )
      }
    }

    candidateKeys.forEach { jsonWebKey ->
      try {
        if (verifySignature(encodedJwt, jsonWebKey.key)) {
          return if (jsonWebKey.keyId != null) {
            ValidationOutcome(
              UiToolsBundle.message(
                "jwt-encoder-decoder.validation.signature.valid-using-key",
                jsonWebKey.keyId,
              ),
              true,
            )
          } else {
            ValidationOutcome(
              UiToolsBundle.message("jwt-encoder-decoder.validation.signature.valid"),
              true,
            )
          }
        }
      } catch (_: Exception) {
        // Ignore incompatible keys from the JWKS and continue with the next candidate.
      }
    }

    return ValidationOutcome(
      UiToolsBundle.message("jwt-encoder-decoder.validation.signature.invalid"),
      false,
    )
  }

  private fun verifySignature(encodedJwt: String, key: Key): Boolean =
    JsonWebSignature()
      .apply {
        setCompactSerialization(encodedJwt)
        setKey(key)
        isDoKeyValidation = strictKeyValidation.get()
      }
      .verifySignature()

  private fun readVerificationKey(keyInput: String, algorithm: SignatureAlgorithm): Key =
    if (keyInput.startsWith("{")) {
      JsonWebKey.Factory.newJwk(keyInput).key
    } else {
      algorithm.kind.keyFactory!!.generatePublic(X509EncodedKeySpec(toRawKey(keyInput)))
    }

  private fun isJwkCompatibleWithAlgorithm(
    jsonWebKey: JsonWebKey,
    algorithm: SignatureAlgorithm,
  ): Boolean {
    val algorithmMatches =
      jsonWebKey.algorithm == null || jsonWebKey.algorithm == algorithm.jwtHeaderValue
    val useMatches = jsonWebKey.use == null || jsonWebKey.use == "sig"
    val keyTypeMatches =
      when (algorithm.kind) {
        SignatureAlgorithmKind.NONE -> false
        SignatureAlgorithmKind.HMAC -> jsonWebKey.keyType == "oct"
        SignatureAlgorithmKind.RSA -> jsonWebKey.keyType == "RSA"
        SignatureAlgorithmKind.ECDSA -> jsonWebKey.keyType == "EC"
      }
    return algorithmMatches && useMatches && keyTypeMatches
  }

  private fun toRawKey(keyInput: String): ByteArray =
    Base64.getDecoder().decode(keyInput.replace(JwtEncoderDecoder.rawKeyRegex, ""))

  private fun rootCauseMessage(exception: Throwable): String {
    val rootCause = ExceptionUtil.getRootCause(exception)
    return rootCause.message ?: rootCause::class.simpleName ?: "unknown"
  }

  private fun setNeutralResult() {
    resultState.set(ValidationResultState.NONE)
    validResultMessage.set("")
    invalidResultMessage.set("")
  }

  private fun setValidResult(message: String) {
    resultState.set(ValidationResultState.VALID)
    validResultMessage.set("<icon src='AllIcons.General.InspectionsOK'>&nbsp;$message")
    invalidResultMessage.set("")
  }

  private fun setInvalidResult(message: String) {
    resultState.set(ValidationResultState.INVALID)
    validResultMessage.set("")
    invalidResultMessage.set("<icon src='AllIcons.General.Error'>&nbsp;$message")
  }

  private data class ValidationOutcome(val message: String, val valid: Boolean)
}
