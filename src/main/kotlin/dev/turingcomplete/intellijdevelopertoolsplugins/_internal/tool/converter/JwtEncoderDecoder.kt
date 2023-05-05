package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter

import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.icons.AllIcons
import com.intellij.json.JsonLanguage
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.TextRange
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.rows
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.ui.layout.ComboBoxPredicate
import com.intellij.ui.layout.not
import com.intellij.util.Alarm
import com.intellij.util.io.decodeBase64
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.DeveloperToolsPluginService
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor.EditorMode.INPUT_OUTPUT
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.setValidationResultBorder
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.toPrettyStringWithDefaultObjectMapper
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.ChangeOrigin.ENCODED
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.ChangeOrigin.HEADER_PAYLOAD
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.ChangeOrigin.SIGNATURE_CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.SignatureAlgorithm.ECDSA256
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.SignatureAlgorithm.ECDSA384
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.SignatureAlgorithm.ECDSA512
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.SignatureAlgorithm.HMAC256
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.SignatureAlgorithm.HMAC384
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.SignatureAlgorithm.HMAC512
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.SignatureAlgorithm.RSA256
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.SignatureAlgorithm.RSA384
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.SignatureAlgorithm.RSA512
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.SignatureAlgorithm.values
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.SignatureAlgorithmKind.ECDSA
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.SignatureAlgorithmKind.HMAC
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.SignatureAlgorithmKind.RSA
import dev.turingcomplete.intellijdevelopertoolsplugins.common.ValueProperty
import io.ktor.util.*
import java.security.KeyFactory
import java.security.PrivateKey
import java.security.PublicKey
import java.security.interfaces.ECPrivateKey
import java.security.interfaces.ECPublicKey
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.swing.Icon


internal class JwtEncoderDecoder(
  private val configuration: DeveloperToolConfiguration, parentDisposable: Disposable
) : DeveloperTool(parentDisposable) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var liveConversion = configuration.register("liveConversion", true)
  var encodedText = configuration.register("encodedText", "", INPUT, EXAMPLE_JWT)
  var headerText = configuration.register("headerText", "", INPUT)
  var payloadText = configuration.register("payloadText", "", INPUT)

  private val highlightEncodedAlarm by lazy { Alarm(parentDisposable) }
  private val highlightHeaderAlarm by lazy { Alarm(parentDisposable) }
  private val highlightPayloadAlarm by lazy { Alarm(parentDisposable) }
  private val conversationAlarm by lazy { Alarm(parentDisposable) }

  private var lastActiveInput: DeveloperToolEditor? = null

  private val encodedEditor by lazy { createEncodedEditor() }
  private val headerEditor by lazy { createHeaderEditor() }
  private val payloadEditor by lazy { createPayloadEditor() }

  private val highlightingAttributes by lazy { EditorColorsManager.getInstance().globalScheme.getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES) }

  private val jwt by lazy { Jwt(configuration, encodedText, headerText, payloadText) }

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    liveConversion.afterChange(parentDisposable) { handleLiveConversionSwitch() }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  @Suppress("UnstableApiUsage")
  override fun Panel.buildUi() {
    row {
      cell(encodedEditor.createComponent())
        .validationOnApply(encodedEditor.bindValidator(jwt.encodedErrorHolder.asValidation()))
        .align(Align.FILL)
    }.bottomGap(BottomGap.NONE)
    row {
      val signatureInvalid = jwt.signatureErrorHolder.asComponentPredicate()
      icon(AllIcons.General.InspectionsOK).visibleIf(signatureInvalid.not()).gap(RightGap.SMALL)
      label("Valid signature").visibleIf(signatureInvalid.not())

      icon(AllIcons.General.Error).visibleIf(signatureInvalid).gap(RightGap.SMALL)
      label("").bindText(jwt.signatureErrorHolder.asObservableNonNullProperty()).visibleIf(signatureInvalid)
    }.topGap(TopGap.NONE)

    row {
      val liveConversionCheckBox = checkBox("Live conversion")
        .bindSelected(liveConversion)
        .gap(RightGap.SMALL)

      button("▼ Decode") { convert(ENCODED) }
        .enabledIf(liveConversionCheckBox.selected.not())
        .gap(RightGap.SMALL)
      button("▲ Encode") {
        // This will set the signature algorithm in the header and will run the encoding.
        convert(SIGNATURE_CONFIGURATION)
      }.enabledIf(liveConversionCheckBox.selected.not())
    }

    row {
      cell(headerEditor.createComponent())
        .validationOnApply(headerEditor.bindValidator(jwt.headerErrorHolder.asValidation()))
        .align(Align.FILL)
        .resizableColumn()

      cell(payloadEditor.createComponent())
        .validationOnApply(payloadEditor.bindValidator(jwt.payloadErrorHolder.asValidation()))
        .align(Align.FILL)
        .resizableColumn()
    }.resizableRow().topGap(TopGap.SMALL).bottomGap(BottomGap.NONE)

    groupRowsRange("Signature Algorithm") {
      lateinit var signatureAlgorithmComboBox: ComboBox<SignatureAlgorithm>
      row {
        signatureAlgorithmComboBox = comboBox(values().toList())
          .label("Algorithm:")
          .bindItem(jwt.signature.algorithm)
          .whenItemSelectedFromUi { convert(SIGNATURE_CONFIGURATION) }
          .component
      }.layout(RowLayout.PARENT_GRID).topGap(TopGap.NONE)

      row {
        label("Secret key:").gap(RightGap.SMALL)
        textField()
          .bindText(jwt.signature.secret)
          .whenTextChangedFromUi { convert(SIGNATURE_CONFIGURATION) }
      }.visibleIf(ComboBoxPredicate(signatureAlgorithmComboBox) { it?.kind == HMAC }).layout(RowLayout.PARENT_GRID)

      row {
        textArea()
          .rows(2)
          .resizableColumn()
          .align(Align.FILL)
          .label(label = "Public key:", position = LabelPosition.TOP)
          .bindText(jwt.signature.publicKey)
          .setValidationResultBorder()
          .whenTextChangedFromUi { convert(SIGNATURE_CONFIGURATION) }
          .validationInfo(jwt.signature.publicKeyErrorHolder.asValidation())

        textArea()
          .rows(2)
          .resizableColumn()
          .align(Align.FILL)
          .label(label = "Private key:", position = LabelPosition.TOP)
          .bindText(jwt.signature.privateKey)
          .setValidationResultBorder()
          .whenTextChangedFromUi { convert(SIGNATURE_CONFIGURATION) }
          .validationInfo(jwt.signature.privateKeyErrorHolder.asValidation())
      }.visibleIf(ComboBoxPredicate(signatureAlgorithmComboBox) { it?.kind != HMAC })
    }
  }

  override fun afterBuildUi() {
    convert(ENCODED)
  }

  override fun reset() {
    jwt.reset()
    convert(ENCODED)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun convert(changeOrigin: ChangeOrigin) {
    if (configuration.isResetting) {
      return
    }

    conversationAlarm.cancelAllRequests()
    conversationAlarm.addRequest({ doConvert(changeOrigin) }, 100)
  }

  private fun doConvert(changeOrigin: ChangeOrigin) {
    when (changeOrigin) {
      ENCODED -> jwt.decodeJwt()
      HEADER_PAYLOAD -> jwt.encodeJwt()
      SIGNATURE_CONFIGURATION -> {
        jwt.setAlgorithmInHeader()
        jwt.encodeJwt()
      }
    }

    highlightDotSeparator()
    highlightHeaderClaims()
    highlightPayloadClaims()

    // The `validate` in this class is not used as a validation mechanism. We
    // make use of its text field error UI to display the `errorHolder`.
    validate()
  }

  private fun highlightDotSeparator() {
    highlightEncodedAlarm.cancelAllRequests()
    val highlightDotSeparator = {
      encodedEditor.removeTextRangeHighlighters(ENCODED_DOT_SEPARATOR_GROUP_ID)
      val encoded = encodedText.get()
      var dotIndex = encoded.indexOf('.')
      var i = 0
      while (dotIndex != -1) {
        encodedEditor.highlightTextRange(
          TextRange(dotIndex, dotIndex + 1),
          ENCODED_DOT_SEPARATOR_HIGHLIGHTER_LAYER,
          highlightingAttributes,
          ENCODED_DOT_SEPARATOR_GROUP_ID
        )
        dotIndex = encoded.indexOf('.', dotIndex + 1)
        i++
      }
    }
    highlightEncodedAlarm.addRequest(highlightDotSeparator, 100)
  }

  private fun highlightHeaderClaims() {
    highlightHeaderAlarm.cancelAllRequests()
    highlightHeaderAlarm.addRequest({ doHighlightClaims(headerEditor) }, 100)
  }

  private fun highlightPayloadClaims() {
    highlightPayloadAlarm.cancelAllRequests()
    highlightPayloadAlarm.addRequest({ doHighlightClaims(payloadEditor) }, 100)
  }

  private fun doHighlightClaims(editor: DeveloperToolEditor) {
    editor.removeTextRangeHighlighters(CLAIMS_GROUP_ID)
    CLAIM_REGEX.findAll(editor.text)
      .mapNotNull {
        val claimMatchResult = it.groups[1]
        if (claimMatchResult != null) {
          val standardClaim = StandardClaim.findByFieldName(claimMatchResult.value)
          if (standardClaim != null) {
            return@mapNotNull claimMatchResult.range to standardClaim
          }
        }
        null
      }
      .forEach { (claimRange, standardClaim) ->
        val textRange = TextRange(claimRange.first, claimRange.last + 1)
        editor.highlightTextRange(
          textRange,
          CLAIM_REGEX_MATCH_HIGHLIGHT_LAYER,
          null,
          CLAIMS_GROUP_ID,
          StandardClaimGutterIconRenderer(textRange, standardClaim)
        )
      }
  }

  private fun createEncodedEditor(): DeveloperToolEditor =
    createEditor(
      changeOrigin = ENCODED,
      title = "Encoded",
      language = PlainTextLanguage.INSTANCE,
      textProperty = encodedText
    ) { highlightDotSeparator() }

  private fun createHeaderEditor(): DeveloperToolEditor =
    createEditor(
      changeOrigin = HEADER_PAYLOAD,
      title = "Header",
      language = JsonLanguage.INSTANCE,
      textProperty = headerText
    ) { highlightHeaderClaims() }

  private fun createPayloadEditor(): DeveloperToolEditor =
    createEditor(
      changeOrigin = HEADER_PAYLOAD,
      title = "Payload",
      language = JsonLanguage.INSTANCE,
      textProperty = payloadText
    ) { highlightPayloadClaims() }

  private fun createEditor(
    changeOrigin: ChangeOrigin,
    title: String,
    language: Language,
    textProperty: ValueProperty<String>,
    onTextChangeFromUi: () -> Unit
  ) = DeveloperToolEditor(title, INPUT_OUTPUT, parentDisposable, textProperty, language).apply {
    onFocusGained {
      lastActiveInput = this
    }
    this.onTextChangeFromUi { _ ->
      lastActiveInput = this
      if (liveConversion.get()) {
        convert(changeOrigin)
      }
      onTextChangeFromUi()
    }
  }

  private fun handleLiveConversionSwitch() {
    if (liveConversion.get()) {
      // Trigger a text change. So if the text was changed in manual mode, it
      // will now be encoded/decoded once during the switch to live mode.
      when (lastActiveInput) {
        encodedEditor -> convert(ENCODED)
        headerEditor, payloadEditor -> {
          // This will set the signature algorithm in the header and will run the encoding.
          convert(SIGNATURE_CONFIGURATION)
        }

        null -> {}
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class StandardClaimGutterIconRenderer(
    private val textRange: TextRange,
    private val standardClaim: StandardClaim
  ) : GutterIconRenderer() {

    override fun getTooltipText(): String = standardClaim.toString()

    override fun getIcon(): Icon = AllIcons.Gutter.JavadocRead

    override fun equals(other: Any?): Boolean {
      return if (other != null && other is StandardClaimGutterIconRenderer) {
        other.textRange == textRange && other.standardClaim == standardClaim
      }
      else {
        false
      }
    }

    override fun hashCode(): Int = Objects.hash(textRange, standardClaim)
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class ChangeOrigin {

    ENCODED,
    HEADER_PAYLOAD,
    SIGNATURE_CONFIGURATION
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class SignatureAlgorithmKind {

    HMAC, RSA, ECDSA
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class SignatureAlgorithm(val headerValue: String, val kind: SignatureAlgorithmKind) {

    HMAC256("HS256", HMAC),
    HMAC384("HS384", HMAC),
    HMAC512("HS512", HMAC),
    RSA256("RS256", RSA),
    RSA384("RS384", RSA),
    RSA512("RS512", RSA),
    ECDSA256("ES256", ECDSA),
    ECDSA384("ES384", ECDSA),
    ECDSA512("ES512", ECDSA);

    override fun toString(): String = "$name ($headerValue)"

    companion object {

      fun findByHeaderValue(headerValue: String): SignatureAlgorithm? =
        values().firstOrNull { it.headerValue == headerValue }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class Jwt(
    configuration: DeveloperToolConfiguration,
    val encoded: ValueProperty<String>,
    val header: ValueProperty<String>,
    val payload: ValueProperty<String>
  ) {

    val encodedErrorHolder = ErrorHolder()
    val headerErrorHolder = ErrorHolder()
    val payloadErrorHolder = ErrorHolder()
    val signatureErrorHolder = ErrorHolder()

    val signature = Signature(configuration, signatureErrorHolder)

    fun decodeJwt() {
      clearErrorHolders()

      val jwtParts = encoded.get().split(".")
      val numOfJwtParts = jwtParts.size

      // Header
      if (numOfJwtParts >= 1) {
        val handleError: (Exception) -> Unit = { error -> header.set(jwtParts[0]); headerErrorHolder.add(error) }
        parseAsJson(jwtParts[0].decodeBase64String(), handleError) {
          parseHeader(it); header.set(it.toPrettyStringWithDefaultObjectMapper())
        }
      }
      else {
        header.set("")
      }

      // Payload
      if (numOfJwtParts >= 2) {
        val handleError: (Exception) -> Unit = { error -> payload.set(jwtParts[1]); payloadErrorHolder.add(error) }
        parseAsJson(jwtParts[1].decodeBase64String(), handleError) {
          payload.set(it.toPrettyStringWithDefaultObjectMapper())
        }
      }
      else {
        payload.set("")
      }

      // Signature
      if (numOfJwtParts >= 3 && headerErrorHolder.isNotSet()) {
        signature.calculate(jwtParts[0].encodeToByteArray(), jwtParts[1].encodeToByteArray())?.let { expectedSignature ->
          val actualSignature = jwtParts[2]
          if (expectedSignature != actualSignature) {
            signatureErrorHolder.add("Invalid signature")
          }
        }
      }
      else {
        signatureErrorHolder.add("Encoded JWT does not have a signature part")
      }
    }

    fun encodeJwt() {
      clearErrorHolders()

      parseAsJson(header.get(), { error -> headerErrorHolder.add(error) }) {
        parseHeader(it)
      }
      parseAsJson(payload.get(), { error -> payloadErrorHolder.add(error) }) {
        // Nothing to do. This just validates that the payload is valid JSON.
      }

      if (headerErrorHolder.isSet() || payloadErrorHolder.isSet()) {
        encoded.set("")
        signatureErrorHolder.add("Unable to calculate signature due to header or payload errors")
      }
      else {
        val encodedHeader = urlEncoder.encode(header.get().encodeToByteArray())
        val encodedPayload = urlEncoder.encode(payload.get().encodeToByteArray())
        val signature = signature.calculate(encodedHeader, encodedPayload)
        if (signature == null) {
          encoded.set("")
        }
        else {
          encoded.set("${encodedHeader.decodeToString()}.${encodedPayload.decodeToString()}.$signature")
        }
      }
    }

    fun setAlgorithmInHeader() {
      parseAsJson(header.get(), { headerErrorHolder.add(it) }) { headerNode ->
        if (headerNode is ObjectNode) {
          headerNode.put("alg", signature.algorithm.get().headerValue)
          header.set(headerNode.toPrettyStringWithDefaultObjectMapper())
        }
      }
    }

    fun reset() {
      signature.reset()
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
        val algorithm = SignatureAlgorithm.findByHeaderValue(algFieldValue)
        if (algorithm != null) {
          signature.algorithm.set(algorithm)
        }
        else {
          headerErrorHolder.add("Unsupported algorithm: '$algFieldValue'")
        }
      }
      else {
        headerErrorHolder.add("Missing algorithm header field: 'alg'")
      }
    }

    private fun parseAsJson(text: String, handleError: (Exception) -> Unit, handleResult: (JsonNode) -> Unit) {
      try {
        val jsonNode = objectMapper.readTree(text)
        handleResult(jsonNode)
      } catch (e: Exception) {
        handleError(e)
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class Signature(
    configuration: DeveloperToolConfiguration,
    private val signatureErrorHolder: ErrorHolder
  ) {

    val algorithm = configuration.register("algorithm", DEFAULT_SIGNATURE_ALGORITHM)

    var secret = ValueProperty(if (DeveloperToolsPluginService.loadExamples) EXAMPLE_SECRET else "")
    var publicKey = ValueProperty("")
    var privateKey = ValueProperty("")

    val publicKeyErrorHolder = ErrorHolder()
    val privateKeyErrorHolder = ErrorHolder()

    init {
      handleAlgorithmChange()
      algorithm.afterChangeConsumeEvent(null) { e ->
        if (e.valueChanged()) {
          handleAlgorithmChange()
        }
      }
    }

    fun calculate(encodedHeader: ByteArray, encodedPayload: ByteArray): String? {
      publicKeyErrorHolder.clear()
      privateKeyErrorHolder.clear()

      val signatureAlgorithm = algorithm.get()
      val algorithm: Algorithm = when (signatureAlgorithm.kind) {
        HMAC -> {
          when (signatureAlgorithm) {
            HMAC256 -> Algorithm.HMAC256(secret.get())
            HMAC384 -> Algorithm.HMAC384(secret.get())
            HMAC512 -> Algorithm.HMAC512(secret.get())
            else -> error("Unexpected secret-based algorithm: $signatureAlgorithm")
          }
        }

        RSA, ECDSA -> {
          val keyFactory = when (signatureAlgorithm.kind) {
            RSA -> rsaKeyFactory
            ECDSA -> ecKeyFactory
            else -> error("Unexpected key-based algorithm: $signatureAlgorithm")
          }

          val publicKey: PublicKey? = try {
            val publicKeyValue = publicKey.get()
            if (publicKeyValue.isBlank()) {
              publicKeyErrorHolder.add("A public key must be provided")
              null
            }
            else {
              keyFactory.generatePublic(X509EncodedKeySpec(toRawKey(publicKeyValue)))
            }
          } catch (e: Exception) {
            publicKeyErrorHolder.add(e)
            null
          }
          assert(publicKey != null || publicKeyErrorHolder.isSet())

          val privateKey: PrivateKey? = try {
            val privateKeyValue = privateKey.get()
            if (privateKeyValue.isBlank()) {
              privateKeyErrorHolder.add("A private key must be provided")
              null
            }
            else {
              keyFactory.generatePrivate(PKCS8EncodedKeySpec(toRawKey(privateKey.get())))
            }
          } catch (e: Exception) {
            privateKeyErrorHolder.add(e)
            null
          }
          assert(privateKey != null || privateKeyErrorHolder.isSet())

          if (publicKeyErrorHolder.isSet() || privateKeyErrorHolder.isSet()) {
            signatureErrorHolder.add("Invalid signature algorithm configuration")
            return null
          }

          when (signatureAlgorithm) {
            RSA256 -> Algorithm.RSA256(publicKey as RSAPublicKey, privateKey as RSAPrivateKey)
            RSA384 -> Algorithm.RSA384(publicKey as RSAPublicKey, privateKey as RSAPrivateKey)
            RSA512 -> Algorithm.RSA512(publicKey as RSAPublicKey, privateKey as RSAPrivateKey)
            ECDSA256 -> Algorithm.ECDSA256(publicKey as ECPublicKey, privateKey as ECPrivateKey)
            ECDSA384 -> Algorithm.ECDSA384(publicKey as ECPublicKey, privateKey as ECPrivateKey)
            ECDSA512 -> Algorithm.ECDSA512(publicKey as ECPublicKey, privateKey as ECPrivateKey)
            else -> error("Unexpected key-based algorithm: $signatureAlgorithm")
          }
        }
      }

      return try {
        val signature = algorithm.sign(encodedHeader, encodedPayload)
        urlEncoder.encodeToString(signature)
      } catch (e: Exception) {
        signatureErrorHolder.add("Failed to calculate signature:", e)
        null
      }
    }

    fun reset() {
      if (DeveloperToolsPluginService.loadExamples) {
        secret.set(EXAMPLE_SECRET)
        when (algorithm.get().kind) {
          HMAC -> {}

          RSA -> {
            publicKey.set(EXAMPLE_RSA_PUBLIC_KEY)
            privateKey.set(EXAMPLE_RSA_PRIVATE_KEY)
          }

          ECDSA -> {
            publicKey.set(EXAMPLE_EC_PUBLIC_KEY)
            privateKey.set(EXAMPLE_EC_PRIVATE_KEY)
          }
        }
      }
      else {
        if (publicKey.get() == EXAMPLE_EC_PUBLIC_KEY || publicKey.get() == EXAMPLE_RSA_PUBLIC_KEY) {
          publicKey.set("")
        }
        if (privateKey.get() == EXAMPLE_EC_PRIVATE_KEY || privateKey.get() == EXAMPLE_RSA_PRIVATE_KEY) {
          privateKey.set("")
        }
        if (secret.get() == EXAMPLE_SECRET) {
          secret.set("")
        }
      }
    }

    private fun toRawKey(keyInput: String): ByteArray = keyInput
      .replace(RAW_KEY_REGEX, "")
      .decodeBase64()

    private fun handleAlgorithmChange() {
      if (DeveloperToolsPluginService.loadExamples) {
        loadExampleSecrets()
      }
    }

    private fun loadExampleSecrets() {
      val publicKeyValue = publicKey.get()
      val privateKeyValue = privateKey.get()
      when (algorithm.get().kind) {
        HMAC -> {
          if (secret.get().isBlank()) {
            secret.set(EXAMPLE_SECRET)
          }
        }

        RSA -> {
          if (publicKeyValue.isBlank() || publicKeyValue == EXAMPLE_EC_PUBLIC_KEY) {
            publicKey.set(EXAMPLE_RSA_PUBLIC_KEY)
          }
          if (privateKeyValue.isBlank() || privateKeyValue == EXAMPLE_EC_PRIVATE_KEY) {
            privateKey.set(EXAMPLE_RSA_PRIVATE_KEY)
          }
        }

        ECDSA -> {
          if (publicKeyValue.isBlank() || publicKeyValue == EXAMPLE_RSA_PUBLIC_KEY) {
            publicKey.set(EXAMPLE_EC_PUBLIC_KEY)
          }
          if (privateKeyValue.isBlank() || privateKeyValue == EXAMPLE_RSA_PRIVATE_KEY) {
            privateKey.set(EXAMPLE_EC_PRIVATE_KEY)
          }
        }
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class StandardClaim(val fieldName: String, val title: String, val description: String) {

    ISSUER("iss", "Issuer", "The issuer of the JWT."),
    SUBJECT("sub", "Subject", "The subject of the JWT (e.g., the user)."),
    AUDIENCE("aud", "Audience", "The recipient for which the JWT is intended."),
    EXPIRATION_TIME("exp", "Expiration Time", "The time after which the JWT expires."),
    NOT_BEFORE_TIME("nbf", "Not Before Time", "The time before which the JWT must not be accepted for processing."),
    ISSUED_AT_TIME("iat", "Issued at Time", "The time at which the JWT was issued."),
    JWT_ID("jti", "JWT ID", "An Unique identifier of this JWT."),
    ALG("alg", "Algorithm", "The algorithm to calculate the signature of this JWT."),
    AZP("azp", "Authorized Party", "The party to which the JWT was issued."),
    SID("sid", "Session ID", "An unique session ID."),
    NONCE("nonce", "Nonce", "A value used to associate a client session with this JWT."),
    AT_HASH("at_hash", "Access Token Hash Value", "The hash of an access token."),
    C_HASH("c_hash", "Code Hash Value", "The hash of a code."),
    ACT("act", "Actor", "The has of an access token.");

    override fun toString(): String = "<html>$title ($fieldName)<br/>$description</html>"

    companion object {

      fun findByFieldName(fieldName: String): StandardClaim? = values().firstOrNull { it.fieldName == fieldName }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory<JwtEncoderDecoder> {

    override fun getDeveloperToolContext() = DeveloperToolContext(
      menuTitle = "JWT",
      contentTitle = "JWT Decoder/Encoder"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable
    ): ((DeveloperToolConfiguration) -> JwtEncoderDecoder) =
      { configuration -> JwtEncoderDecoder(configuration, parentDisposable) }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val objectMapper = ObjectMapper()
    private val urlEncoder = Base64.getUrlEncoder().withoutPadding()

    private val CLAIM_REGEX = Regex("\\s?\"(?<name>[a-zA-Z]+)\":")
    private const val CLAIM_REGEX_MATCH_HIGHLIGHT_LAYER = HighlighterLayer.SELECTION - 1
    private const val CLAIMS_GROUP_ID = "claims"
    private const val ENCODED_DOT_SEPARATOR_GROUP_ID = "encodedDotSeparator"
    private const val ENCODED_DOT_SEPARATOR_HIGHLIGHTER_LAYER = HighlighterLayer.SELECTION - 1

    private val RAW_KEY_REGEX = Regex("\\r?\\n|\\r|\\s?-+(BEGIN|END).*KEY-+\\s?")

    private val DEFAULT_SIGNATURE_ALGORITHM = HMAC256
    private const val EXAMPLE_JWT =
      "ewogICJ0eXAiOiJKV1QiLAogICJhbGciOiJIUzI1NiIKfQ.ewogICJqdGkiOiAiOTY0OTJkNTktMGFkNS00YzAwLTg5MmQtNTkwYWQ1YWMwMGYzIiwKICAic3ViIjoiMDEyMzQ1Njc4OSIsCiAgIm5hbWUiOiJKb2huIERvZSIsCiAgImlhdCI6MTY4MTA0MDUxNQp9.xLAXegMLC1DAstovUxWo_hqmjXqE0Qd7g0USLtaCVbo"
    private const val EXAMPLE_SECRET = "s3cre!"

    private val rsaKeyFactory = KeyFactory.getInstance("RSA")
    private val ecKeyFactory = KeyFactory.getInstance("EC")

    private val EXAMPLE_RSA_PUBLIC_KEY = """
-----BEGIN RSA PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA3WnSxY9w6mmLaWdYpOno6SCAFNioRaso
E70ekkkvUZnBULv76Ix1s0FIZvGqqxmFZA1wh7xHMgT3GVJVgXM3A8EOWBfFZLUaTMXwCCvzmUZn
64SvYwGbMTjwglzDmPiaAmxKhDqbApWxym4DQYMOAH7CEs7ljg2uOyrnCEGndtNpVGTEOYgUPQkX
En0orOh8rwjE2OZYv/Wvju/BX6iBlBOBR3iabImsts9gDBM8AT6FEZE2ko4zk9uXvhmO5J7YpZ1Z
3tTkXTjzJznHdvAjnseeWfguEJTq9lCA2gIp0wBv9KM8FsXfDmlSSct5o93XdwHg9rwTJsnJ4azP
rHHKhwIDAQAB
-----END RSA PUBLIC KEY-----
    """.trimIndent()
    private val EXAMPLE_RSA_PRIVATE_KEY = """
-----BEGIN RSA PRIVATE KEY-----
MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQDdadLFj3DqaYtpZ1ik6ejpIIAU
2KhFqygTvR6SSS9RmcFQu/vojHWzQUhm8aqrGYVkDXCHvEcyBPcZUlWBczcDwQ5YF8VktRpMxfAI
K/OZRmfrhK9jAZsxOPCCXMOY+JoCbEqEOpsClbHKbgNBgw4AfsISzuWODa47KucIQad202lUZMQ5
iBQ9CRcSfSis6HyvCMTY5li/9a+O78FfqIGUE4FHeJpsiay2z2AMEzwBPoURkTaSjjOT25e+GY7k
ntilnVne1ORdOPMnOcd28COex55Z+C4QlOr2UIDaAinTAG/0ozwWxd8OaVJJy3mj3dd3AeD2vBMm
ycnhrM+sccqHAgMBAAECggEARelztZDg2QuhixMoUM5RDkeGWc69d14fZfgpzowQRmZTvZ/V32x2
f7bl2yeEucjxrxF1Tk67dkZOFa9DM4BDR0qusk8zM2Th3IsFizcBkIzEJIA9dvgbXjP58VfEJSme
S5SRBOaSaoME5APPwGBWy/46XoD4x912/dTCpX9Blwl81i7EO8o3NnYhsCWeVoUJTWBzN95OchZF
ozV4pFgv0tZqTNa7VhJtWHHiKkCpdK7gA9SeVEqEeL1TADAa2ngy3BIRfTgdAct6/4N+ZlVsaIXB
1Gnw2RaOoUbHy1PCA6ygtH5lz65p0JdWGcO5l+JNeYmOIeOdJ3QWbVI+CikPGQKBgQDv/JrTewDt
BK5KBpotFsrJeDFKOkC6A8aNeGliAEgJYvCk7zb8RtKCx7ViaYGYWJYj30oYejYEE+vFT7sgzXfE
JPAEiMw4uKeIrbX8QEIP+R25S8iRr657DkTxOvyhO2oQcC7UkZagvrVyQ17VgjtjxGWbc5bRBk5v
u1ZV9VMZuQKBgQDsL/PsLCX3YRBB5+0rpWoTKKrmFtGh31oue+d37Nd7oxBzb2uyF4Q29+zoy1on
EnHNamjjdR95NZoOjEsIIKDTV1C/bsS7be53m0mwKQfecKIXJ+7VN4UsYZXjCajHCr3NFHiIU8ct
pcKGtg7ga5cERIBtrPAi9Qzi7/o1MxUmPwKBgFuSaMWPZuAJ7DNE56mSy9gqa6xmI/KWpDmxG40Q
jGxAe5CD0thacdMDPzwJBDFMhCW1+wDyCRBvRYSpkr7GiA+pBIjGZh6ynwKxPgK9xjdwGB5vQ14L
yikcXcQqfOFM2YDiPYxQ7Ufy3St3d4VCx0SfWSIC7iZeIKnTsvLjxEzJAoGBAKcLFzou0z9N3+Cs
9pnK6OXZ+ly3QNZ6kF6V9VRlJtXjs0vhPsr7ROBXoq/WutEtg11j6AEPIg5o8adeY+bApN40QADU
h8GD84eWRZyYuF8DTDCSZqFYHhEQh6DGgR8dIrX7x2+ryRAozxbVhloE3g7/n9Fx4Xjn1ZBfZ5fe
pBOjAoGAcw2M22BK3NWOHhJ8EC4p6aUIR96lNcCWE/ij+MWCcRdotLDSDuT1q13C+UTxDZ5PsmDs
N/bhCDRZYZoLYo0/h6v4zKBDaX05nVUTCYux0Fo2HGrj5S0bjmgyRcr8+enA3CTzCHZPWZ7ZeADb
0Mbtt/Q4JyOCgwORgXJVQBHxxIQ=
-----END RSA PRIVATE KEY-----
    """.trimIndent()
    private val EXAMPLE_EC_PUBLIC_KEY = """
-----BEGIN EC PUBLIC KEY-----
MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAECqeGZJp0t+9bOgpgnWSpCeEyxTtbYUcobxrNkb4T
LFQWo96cYKzw0yr85CKeal9Nx4l1rG34ynzNxWZNfl7Rhg==
-----END RSA PUBLIC KEY----- 
    """.trimIndent()
    private val EXAMPLE_EC_PRIVATE_KEY = """
-----BEGIN EC PRIVATE KEY-----
MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCDQ+B6qEzr/M2sql4X+09X9YlYt8BKA
HX8Q7/6s4KC3qQ==
-----END RSA PRIVATE KEY-----
    """.trimIndent()
  }
}