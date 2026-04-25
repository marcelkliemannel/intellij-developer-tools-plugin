package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.jwtencoderdecoder

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.util.ExceptionUtil
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.decodeBase64String
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.SENSITIVE
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsApplicationSettings.Companion.generalSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.ObjectMapperService
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import java.security.Key
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64
import org.apache.commons.codec.binary.Base32
import org.jose4j.jws.AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256
import org.jose4j.jws.AlgorithmIdentifiers.ECDSA_USING_P384_CURVE_AND_SHA384
import org.jose4j.jws.AlgorithmIdentifiers.ECDSA_USING_P521_CURVE_AND_SHA512
import org.jose4j.jws.AlgorithmIdentifiers.HMAC_SHA256
import org.jose4j.jws.AlgorithmIdentifiers.HMAC_SHA384
import org.jose4j.jws.AlgorithmIdentifiers.HMAC_SHA512
import org.jose4j.jws.AlgorithmIdentifiers.RSA_USING_SHA256
import org.jose4j.jws.AlgorithmIdentifiers.RSA_USING_SHA384
import org.jose4j.jws.AlgorithmIdentifiers.RSA_USING_SHA512
import org.jose4j.jws.JsonWebSignature
import org.jose4j.keys.HmacKey

internal enum class ChangeOrigin {

  ENCODED,
  HEADER_OR_PAYLOAD,
  SIGNATURE_CONFIGURATION,
}

internal enum class SignatureAlgorithmKind(val keyFactory: KeyFactory?) {

  NONE(null),
  HMAC(null),
  RSA(KeyFactory.getInstance("RSA")),
  ECDSA(KeyFactory.getInstance("EC")),
}

internal enum class SignatureAlgorithm(
  val jwtHeaderValue: String,
  val kind: SignatureAlgorithmKind,
  @Suppress("unused") // May be used for JWK validation
  val algorithmIdentifiers: String,
) {

  NONE("none", SignatureAlgorithmKind.NONE, "none"),
  HMAC256("HS256", SignatureAlgorithmKind.HMAC, HMAC_SHA256),
  HMAC384("HS384", SignatureAlgorithmKind.HMAC, HMAC_SHA384),
  HMAC512("HS512", SignatureAlgorithmKind.HMAC, HMAC_SHA512),
  RSA256("RS256", SignatureAlgorithmKind.RSA, RSA_USING_SHA256),
  RSA384("RS384", SignatureAlgorithmKind.RSA, RSA_USING_SHA384),
  RSA512("RS512", SignatureAlgorithmKind.RSA, RSA_USING_SHA512),
  ECDSA256("ES256", SignatureAlgorithmKind.ECDSA, ECDSA_USING_P256_CURVE_AND_SHA256),
  ECDSA384("ES384", SignatureAlgorithmKind.ECDSA, ECDSA_USING_P384_CURVE_AND_SHA384),
  ECDSA512("ES512", SignatureAlgorithmKind.ECDSA, ECDSA_USING_P521_CURVE_AND_SHA512);

  override fun toString(): String =
    if (this == NONE) {
      UiToolsBundle.message("jwt-encoder-decoder.signature-algorithm.none")
    } else {
      UiToolsBundle.message("jwt-encoder-decoder.signature-algorithm.named", name, jwtHeaderValue)
    }

  companion object {

    fun findByJwtHeaderValue(jwtHeaderValue: String): SignatureAlgorithm? =
      entries.firstOrNull { it.jwtHeaderValue == jwtHeaderValue }
  }
}

internal enum class SecretKeyEncodingMode(val title: String) {

  RAW(UiToolsBundle.message("jwt-encoder-decoder.secret-key-encoding-mode.raw")),
  BASE32(UiToolsBundle.message("jwt-encoder-decoder.secret-key-encoding-mode.base32")),
  BASE64(UiToolsBundle.message("jwt-encoder-decoder.secret-key-encoding-mode.base64")),
}

internal enum class ValidationKeySource(private val title: String) {

  SECRET(UiToolsBundle.message("jwt-encoder-decoder.validation-key-source.secret")),
  PUBLIC_KEY(UiToolsBundle.message("jwt-encoder-decoder.validation-key-source.public-key")),
  JWKS(UiToolsBundle.message("jwt-encoder-decoder.validation-key-source.jwks"));

  override fun toString(): String = title
}

internal enum class ValidationResultState {

  NONE,
  VALID,
  INVALID,
}

internal enum class StandardClaim(
  val fieldName: String,
  val title: String,
  val description: String,
) {

  TYP(
    fieldName = "typ",
    title = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.typ.title"),
    description = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.typ.description"),
  ),
  ISSUER(
    fieldName = "iss",
    title = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.iss.title"),
    description = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.iss.description"),
  ),
  SUBJECT(
    fieldName = "sub",
    title = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.sub.title"),
    description = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.sub.description"),
  ),
  AUDIENCE(
    fieldName = "aud",
    title = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.aud.title"),
    description = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.aud.description"),
  ),
  EXPIRATION_TIME(
    fieldName = "exp",
    title = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.exp.title"),
    description = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.exp.description"),
  ),
  NOT_BEFORE_TIME(
    fieldName = "nbf",
    title = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.nbf.title"),
    description = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.nbf.description"),
  ),
  ISSUED_AT_TIME(
    fieldName = "iat",
    title = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.iat.title"),
    description = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.iat.description"),
  ),
  JWT_ID(
    fieldName = "jti",
    title = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.jti.title"),
    description = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.jti.description"),
  ),
  ALG(
    fieldName = "alg",
    title = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.alg.title"),
    description = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.alg.description"),
  ),
  AZP(
    fieldName = "azp",
    title = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.azp.title"),
    description = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.azp.description"),
  ),
  SID(
    fieldName = "sid",
    title = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.sid.title"),
    description = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.sid.description"),
  ),
  NONCE(
    fieldName = "nonce",
    title = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.nonce.title"),
    description = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.nonce.description"),
  ),
  AT_HASH(
    fieldName = "at_hash",
    title = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.at-hash.title"),
    description = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.at-hash.description"),
  ),
  C_HASH(
    fieldName = "c_hash",
    title = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.c-hash.title"),
    description = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.c-hash.description"),
  ),
  ACT(
    fieldName = "act",
    title = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.act.title"),
    description = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.act.description"),
  ),
  AUTH_TIME(
    fieldName = "auth_time",
    title = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.auth-time.title"),
    description = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.auth-time.description"),
  ),
  SCOPE(
    fieldName = "scope",
    title = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.scope.title"),
    description = UiToolsBundle.message("jwt-encoder-decoder.standard-claim.scope.description"),
  );

  override fun toString(): String =
    UiToolsBundle.message(
      "jwt-encoder-decoder.standard-claim.tooltip",
      title,
      fieldName,
      description,
    )

  companion object {

    fun findByFieldName(fieldName: String): StandardClaim? =
      entries.firstOrNull { it.fieldName == fieldName }
  }
}

internal class Jwt(
  configuration: DeveloperToolConfiguration,
  val encoded: ValueProperty<String>,
  val header: ValueProperty<String>,
  val payload: ValueProperty<String>,
) {

  val encodedErrorHolder = ErrorHolder()
  val headerErrorHolder = ErrorHolder()
  val payloadErrorHolder = ErrorHolder()
  val signatureErrorHolder =
    ErrorHolder(addErrorIconToMessage = true, surroundMessageWithHtml = false)

  val signature = Signature(configuration, signatureErrorHolder)

  fun decodeJwt() {
    clearErrorHolders()

    val jwtParts = encoded.get().split('.', limit = 3)
    val numOfJwtParts = jwtParts.size

    if (numOfJwtParts >= 1) {
      val handleError: (Exception) -> Unit = { error ->
        header.set(jwtParts[0])
        headerErrorHolder.add(error)
      }
      parseAsJson(text = jwtParts[0], textIsBase64 = true, handleError) {
        parseHeader(it)
        header.set(ObjectMapperService.instance.prettyPrintJson(it))
      }
    } else {
      header.set("")
    }

    if (numOfJwtParts >= 2) {
      val handleError: (Exception) -> Unit = { error ->
        payload.set(jwtParts[1])
        payloadErrorHolder.add(error)
      }
      parseAsJson(text = jwtParts[1], textIsBase64 = true, handleError) {
        payload.set(ObjectMapperService.instance.prettyPrintJson(it))
      }
    } else {
      payload.set("")
    }
  }

  fun encodeJwt() {
    clearErrorHolders()

    val jsonMapper = ObjectMapperService.instance.jsonMapper()

    val headerJson =
      try {
        val headerJson = jsonMapper.readTree(header.get())
        parseHeader(headerJson)
        headerJson
      } catch (e: Exception) {
        headerErrorHolder.add(e)
        null
      }

    val payloadJson =
      try {
        jsonMapper.readTree(payload.get())
      } catch (e: Exception) {
        payloadErrorHolder.add(e)
        null
      }

    if (headerErrorHolder.isSet() || payloadErrorHolder.isSet()) {
      encoded.set("")
      signatureErrorHolder.add(
        UiToolsBundle.message(
          "jwt-encoder-decoder.encode.signature-unavailable.header-payload-errors"
        )
      )
    } else {
      val encodedHeader =
        JwtEncoderDecoder.urlEncoder
          .encode(jsonMapper.writeValueAsString(headerJson!!).encodeToByteArray())
          .decodeToString()
      val encodedPayload =
        JwtEncoderDecoder.urlEncoder
          .encode(jsonMapper.writeValueAsString(payloadJson!!).encodeToByteArray())
          .decodeToString()
      val encodedSignature = signature.compute(encodedHeader, encodedPayload)
      if (encodedSignature == null) {
        encoded.set("")
        signatureErrorHolder.addIfNoErrors(
          UiToolsBundle.message(
            "jwt-encoder-decoder.encode.signature-unavailable.configuration-errors"
          )
        )
      } else {
        encoded.set("${encodedHeader}.${encodedPayload}.$encodedSignature")
      }
    }
  }

  fun setAlgorithmInHeader() {
    parseAsJson(text = header.get(), textIsBase64 = false, { headerErrorHolder.add(it) }) {
      headerNode ->
      if (headerNode is ObjectNode) {
        headerNode.put("alg", signature.algorithm.get().jwtHeaderValue)
        header.set(ObjectMapperService.instance.prettyPrintJson(headerNode))
      }
    }
  }

  private fun clearErrorHolders() {
    encodedErrorHolder.clear()
    headerErrorHolder.clear()
    payloadErrorHolder.clear()
    signatureErrorHolder.clear()
  }

  private fun parseHeader(headerNode: JsonNode) {
    if (headerNode.has("alg")) {
      val algFieldValue = headerNode.get("alg").asText()
      val algorithm = SignatureAlgorithm.findByJwtHeaderValue(algFieldValue)
      if (algorithm != null) {
        signature.algorithm.set(algorithm)
      } else {
        headerErrorHolder.add(
          UiToolsBundle.message("jwt-encoder-decoder.header.unsupported-algorithm", algFieldValue)
        )
      }
    } else {
      headerErrorHolder.add(UiToolsBundle.message("jwt-encoder-decoder.header.missing-algorithm"))
    }
  }

  private fun parseAsJson(
    text: String,
    textIsBase64: Boolean,
    handleError: (Exception) -> Unit,
    handleResult: (JsonNode) -> Unit,
  ) {
    try {
      val actualText = if (textIsBase64) text.decodeBase64String() else text
      val jsonNode = ObjectMapperService.instance.jsonMapper().readTree(actualText)
      handleResult(jsonNode)
    } catch (e: Exception) {
      handleError(e)
    }
  }
}

internal class Signature(
  configuration: DeveloperToolConfiguration,
  private val signatureErrorHolder: ErrorHolder,
) {

  val algorithm = configuration.register("algorithm", JwtEncoderDecoder.defaultSignatureAlgorithm)
  val strictSigningKeyValidation =
    configuration.register(
      "signingKeyValidation",
      JwtEncoderDecoder.SIGNING_KEY_VALIDATION_DEFAULT,
      CONFIGURATION,
    )
  val secret = configuration.register("secret", "", SENSITIVE, JwtEncoderDecoder.EXAMPLE_SECRET)
  val privateKey =
    configuration.registerWithExampleProvider("privateKey", "", SENSITIVE) {
      if (algorithm.get().kind == SignatureAlgorithmKind.RSA) {
        JwtEncoderDecoder.exampleRsaPrivateKey
      } else {
        JwtEncoderDecoder.exampleEcPrivateKey
      }
    }
  val secretEncodingMode =
    configuration.register("secretKeyEncodingMode", SecretKeyEncodingMode.RAW, CONFIGURATION)

  val privateKeyErrorHolder = ErrorHolder()

  init {
    handleAlgorithmChange()
    algorithm.afterChangeConsumeEvent(null) { event ->
      if (event.valueChanged()) {
        handleAlgorithmChange()
      }
    }
  }

  fun compute(encodedHeader: String, encodedPayload: String): String? {
    privateKeyErrorHolder.clear()

    return try {
      if (algorithm.get() == SignatureAlgorithm.NONE) {
        return ""
      }
      val signingKey = createSigningKey() ?: return null
      ExtendedJsonWebSignature()
        .apply {
          setEncodedHeader(encodedHeader)
          setEncodedPayload(encodedPayload)
          setKey(signingKey)
          isDoKeyValidation = strictSigningKeyValidation.get()
          sign()
        }
        .encodedSignature
    } catch (e: Exception) {
      signatureErrorHolder.add(
        UiToolsBundle.message("jwt-encoder-decoder.signature.compute-failed"),
        ExceptionUtil.getRootCause(e),
      )
      null
    }
  }

  private fun createSigningKey(): Key? {
    val signatureAlgorithm = algorithm.get()
    return when (signatureAlgorithm.kind) {
      SignatureAlgorithmKind.NONE -> null
      SignatureAlgorithmKind.HMAC ->
        HmacKey(
          when (secretEncodingMode.get()) {
            SecretKeyEncodingMode.RAW -> secret.get().encodeToByteArray()
            SecretKeyEncodingMode.BASE32 -> Base32().decode(secret.get())
            SecretKeyEncodingMode.BASE64 -> Base64.getDecoder().decode(secret.get())
          }
        )

      SignatureAlgorithmKind.RSA,
      SignatureAlgorithmKind.ECDSA ->
        readPrivateKey(signatureAlgorithm.kind.keyFactory!!) ?: return null
    }
  }

  private fun readPrivateKey(keyFactory: KeyFactory) =
    try {
      val privateKeyValue = privateKey.get()
      if (privateKeyValue.isBlank()) {
        privateKeyErrorHolder.add(
          UiToolsBundle.message("jwt-encoder-decoder.signature.private-key-required")
        )
        null
      } else {
        keyFactory.generatePrivate(PKCS8EncodedKeySpec(toRawKey(privateKey.get())))
      }
    } catch (e: Exception) {
      privateKeyErrorHolder.add(e)
      null
    }

  private fun toRawKey(keyInput: String): ByteArray =
    Base64.getDecoder().decode(keyInput.replace(JwtEncoderDecoder.rawKeyRegex, ""))

  private fun handleAlgorithmChange() {
    if (generalSettings.loadExamples.get()) {
      loadExampleSecrets()
    }
  }

  private fun loadExampleSecrets() {
    val privateKeyValue = privateKey.get()
    when (algorithm.get().kind) {
      SignatureAlgorithmKind.NONE -> {}
      SignatureAlgorithmKind.HMAC -> {
        if (secret.get().isBlank()) {
          secret.set(JwtEncoderDecoder.EXAMPLE_SECRET)
        }
      }

      SignatureAlgorithmKind.RSA -> {
        if (privateKeyValue.isBlank() || privateKeyValue == JwtEncoderDecoder.exampleEcPrivateKey) {
          privateKey.set(JwtEncoderDecoder.exampleRsaPrivateKey)
        }
      }

      SignatureAlgorithmKind.ECDSA -> {
        if (
          privateKeyValue.isBlank() || privateKeyValue == JwtEncoderDecoder.exampleRsaPrivateKey
        ) {
          privateKey.set(JwtEncoderDecoder.exampleEcPrivateKey)
        }
      }
    }
  }
}

internal class ExtendedJsonWebSignature : JsonWebSignature() {

  @Suppress("RedundantVisibilityModifier")
  public override fun setEncodedHeader(encodedHeader: String?) {
    super.setEncodedHeader(encodedHeader)
  }
}
