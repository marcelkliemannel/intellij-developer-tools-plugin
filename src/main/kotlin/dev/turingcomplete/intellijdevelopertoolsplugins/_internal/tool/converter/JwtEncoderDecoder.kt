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
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.TextRange
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.ui.layout.ComboBoxPredicate
import com.intellij.ui.layout.not
import com.intellij.util.Alarm
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.BooleanComponentPredicate
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor.EditorMode.INPUT_OUTPUT
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.ChangeOrigin.ENCODED
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.ChangeOrigin.HEADER
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.ChangeOrigin.PAYLOAD
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.converter.JwtEncoderDecoder.ChangeOrigin.SIGNATURE
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
import java.util.Base64


internal class JwtEncoderDecoder(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  DeveloperTool(
    developerToolContext = DeveloperToolContext("JWT", "JWT Decoder/Encoder"),
    parentDisposable = parentDisposable
  ) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var liveConversion: Boolean by configuration.register("liveConversion", true)
  private val conversationAlarm by lazy { Alarm(parentDisposable) }

  private var lastActiveInput: DeveloperToolEditor? = null

  val encodedEditor by lazy { createEditor(ENCODED, "Encoded", PlainTextLanguage.INSTANCE) }
  val headerEditor by lazy { createEditor(HEADER, "Header", JsonLanguage.INSTANCE) }
  val payloadEditor by lazy { createEditor(PAYLOAD, "Payload", JsonLanguage.INSTANCE) }

  private val encodedErrorHolder = ErrorHolder()
  private val headerErrorHolder = ErrorHolder()
  private val payloadErrorHolder = ErrorHolder()


  private var signatureAlgorithm = configuration.register("signatureAlgorithm", DEFAULT_SIGNATURE_ALGORITHM)
  private var signatureSecret = AtomicProperty("s3cre!")
  private var signaturePublicKey = AtomicProperty("")
  private var signaturePrivateKey = AtomicProperty("")
  private val signatureVerified = BooleanComponentPredicate(true)

  private val encodedDotSeparatorAttributes by lazy { EditorColorsManager.getInstance().globalScheme.getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES) }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
      cell(encodedEditor.createComponent())
        .validationOnApply(encodedEditor.bindValidator(encodedErrorHolder.asValidation()))
        .align(Align.FILL)
    }

    row {
      cell(headerEditor.createComponent())
        .validationOnApply(headerEditor.bindValidator(headerErrorHolder.asValidation()))
        .align(Align.FILL)
        .resizableColumn()

      cell(payloadEditor.createComponent())
        .validationOnApply(payloadEditor.bindValidator(payloadErrorHolder.asValidation()))
        .align(Align.FILL)
        .resizableColumn()
    }.resizableRow().topGap(TopGap.SMALL)

    groupRowsRange("Signature") {
      lateinit var signatureAlgorithmComboBox: ComboBox<SignatureAlgorithm>
      row {
        signatureAlgorithmComboBox = comboBox(values().toList())
          .label("Algorithm:")
          .bindItem(signatureAlgorithm)
          .whenItemSelectedFromUi { convert(SIGNATURE) }
          .component
      }
      row {
        textField()
          .label("Secret:")
          .bindText(signatureSecret)
          .whenTextChangedFromUi { convert(SIGNATURE) }
      }.visibleIf(ComboBoxPredicate(signatureAlgorithmComboBox) { it?.kind == AlgorithmKind.HMAC })
      row {
        icon(AllIcons.General.InspectionsOK).visibleIf(signatureVerified).gap(RightGap.SMALL)
        label("Signature verified").visibleIf(signatureVerified)

        icon(AllIcons.General.InspectionsError).visibleIf(signatureVerified.not()).gap(RightGap.SMALL)
        label("Invalid signature").visibleIf(signatureVerified.not())
      }.topGap(TopGap.SMALL)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun convert(changeOrigin: ChangeOrigin) {
    conversationAlarm.cancelAllRequests()
    conversationAlarm.addRequest({ doConvert(changeOrigin) }, 100)
  }

  private fun doConvert(changeOrigin: ChangeOrigin) {
    encodedErrorHolder.unset()
    headerErrorHolder.unset()

    when (changeOrigin) {
      ENCODED -> {
        decodeJwt()
      }

      HEADER -> {
        try {
          val headerJson = objectMapper.readTree(headerEditor.text)
          val headerAlgorithm = headerJson.get("alg").asText()?.let { SignatureAlgorithm.findByFieldValue(it) }
            ?: DEFAULT_SIGNATURE_ALGORITHM
          signatureAlgorithm.set(headerAlgorithm)
          encodeJwt()
        }
        catch (e: Exception) {
          headerErrorHolder.set(e)
        }
      }

      PAYLOAD -> {
        encodeJwt()
      }

      SIGNATURE -> {
        setAlgorithmInHeader()
        encodeJwt()
      }
    }

    // Encoded dot separator highlighting
    encodedEditor.removeAllTextRangeHighlighters()
    val encoded = encodedEditor.text
    var dotIndex = encoded.indexOf('.')
    while (dotIndex != -1) {
      encodedEditor.highlightTextRange(
        TextRange(dotIndex, dotIndex + 1),
        ENCODED_DOT_SEPARATOR_HIGHLIGHTER_LAYER,
        encodedDotSeparatorAttributes,
        ENCODED_DOT_SEPARATOR_HIGHLIGHTING_GROUP_ID
      )
      dotIndex = encoded.indexOf('.', dotIndex + 1)
    }

    // The `validate` in this class is not used as a validation mechanism. We
    // make use of its text field error UI to display the `errorHolder`.
    validate()
  }

  private fun decodeJwt() {
    try {
      val jwtParts = encodedEditor.text.split(".")
      if (jwtParts.isNotEmpty()) {
        val header = encodeJwtPart(jwtParts[0])
        headerEditor.text = header?.toPrettyString() ?: jwtParts[0]
        if (header != null) {
          val headerAlgorithm = header.get("alg")?.asText()?.let { algFieldValue -> SignatureAlgorithm.findByFieldValue(algFieldValue) }
          if (headerAlgorithm != null) {
            signatureAlgorithm.set(headerAlgorithm)
          }
          else {
            signatureAlgorithm.set(DEFAULT_SIGNATURE_ALGORITHM)
            setAlgorithmInHeader()
          }
        }

        if (jwtParts.size > 1) {
          payloadEditor.text = encodeJwtPart(jwtParts[1])?.toPrettyString() ?: jwtParts[1]
        }
      }
    } catch (e: Exception) {
      encodedErrorHolder.set(e)
    }
  }

  private fun encodeJwt() {
    val encodedHeader = urlEncoder.encode(headerEditor.text.encodeToByteArray())
    val encodedPayload = urlEncoder.encode(payloadEditor.text.encodeToByteArray())

    val signatureBytes: ByteArray = when (signatureAlgorithm.get()) {
      RSA256 -> TODO()
      RSA384 -> TODO()
      RSA512 -> TODO()
      HMAC256 -> Algorithm.HMAC256(signatureSecret.get())
      HMAC384 -> Algorithm.HMAC384(signatureSecret.get())
      HMAC512 -> Algorithm.HMAC512(signatureSecret.get())
      ECDSA256 -> TODO()
      ECDSA384 -> TODO()
      ECDSA512 -> TODO()
    }.sign(encodedHeader, encodedPayload)
    val signature = urlEncoder.encodeToString(signatureBytes)

    encodedEditor.text = "${encodedHeader.decodeToString()}.${encodedPayload.decodeToString()}.$signature"
  }

  private fun setAlgorithmInHeader() {
    try {
      val headerJson = objectMapper.readTree(headerEditor.text)
      if (headerJson is ObjectNode) {
        headerJson.put("alg", signatureAlgorithm.get().fieldValue)
        headerEditor.text = headerJson.toPrettyString()
      }
    } catch (ignore: Exception) {
    }
  }

  private fun encodeJwtPart(jwtPartUrlEncoded: String): JsonNode? {
    val jwtPartDecode = Base64.getUrlDecoder().decode(jwtPartUrlEncoded.encodeToByteArray())
    return try {
      objectMapper.readTree(jwtPartDecode)
    } catch (e: Exception) {
      null
    }
  }

  private fun createEditor(changeOrigin: ChangeOrigin, title: String, language: Language) =
    DeveloperToolEditor(title, INPUT_OUTPUT, parentDisposable, language).apply {
      onFocusGained {
        lastActiveInput = this
      }
      this.onTextChangeFromUi { _ ->
        if (liveConversion) {
          convert(changeOrigin)
        }
      }
    }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  protected enum class ChangeOrigin {

    ENCODED,
    HEADER,
    PAYLOAD,
    SIGNATURE
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class AlgorithmKind {

    RSA,
    HMAC,
    ECDSA
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class SignatureAlgorithm(val fieldValue: String, val kind: AlgorithmKind) {

    RSA256("RS256", AlgorithmKind.RSA),
    RSA384("RS384", AlgorithmKind.RSA),
    RSA512("RS512", AlgorithmKind.RSA),
    HMAC256("HS256", AlgorithmKind.HMAC),
    HMAC384("HS384", AlgorithmKind.HMAC),
    HMAC512("HS512", AlgorithmKind.HMAC),
    ECDSA256("ES256", AlgorithmKind.ECDSA),
    ECDSA384("ES384", AlgorithmKind.ECDSA),
    ECDSA512("ES512", AlgorithmKind.ECDSA);

    override fun toString(): String = "$name ($fieldValue)"

    companion object {

      fun findByFieldValue(fieldValue: String): SignatureAlgorithm? =
        values().firstOrNull { it.fieldValue == fieldValue }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory<JwtEncoderDecoder> {

    override fun createDeveloperTool(
      configuration: DeveloperToolConfiguration,
      project: Project?,
      parentDisposable: Disposable
    ) = JwtEncoderDecoder(configuration, parentDisposable)
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val objectMapper = ObjectMapper()
    private val urlEncoder = Base64.getUrlEncoder().withoutPadding()

    private const val ENCODED_DOT_SEPARATOR_HIGHLIGHTER_LAYER = HighlighterLayer.SELECTION - 1
    private const val ENCODED_DOT_SEPARATOR_HIGHLIGHTING_GROUP_ID = "selectedMatchResultHighlighting"

    private val DEFAULT_SIGNATURE_ALGORITHM = HMAC256
  }
}