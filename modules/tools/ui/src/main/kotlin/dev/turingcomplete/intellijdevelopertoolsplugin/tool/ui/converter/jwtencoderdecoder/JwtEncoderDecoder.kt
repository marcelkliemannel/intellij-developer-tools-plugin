package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.jwtencoderdecoder

import com.intellij.icons.AllIcons
import com.intellij.json.JsonLanguage
import com.intellij.lang.Language
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.editor.colors.EditorColors
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.fileTypes.PlainTextLanguage
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.getUserData
import com.intellij.openapi.ui.putUserData
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.actionButton
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.rows
import com.intellij.ui.dsl.builder.selected
import com.intellij.ui.dsl.builder.whenItemSelectedFromUi
import com.intellij.ui.dsl.builder.whenStateChangedFromUi
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.ui.layout.ComboBoxPredicate
import com.intellij.ui.layout.not
import com.intellij.util.Alarm
import com.intellij.util.ui.JBUI
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolsApplicationSettings.Companion.generalSettings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.GeneralSettings.Companion.createSensitiveInputsHandlingToolTipText
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor.EditorMode
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.PropertyComponentPredicate
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.SimpleToggleAction
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.UiUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.onSelectionChanged
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.registerDynamicToolTip
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.setValidationResultBorder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.wrapTabbedPaneContent
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import java.util.Base64
import javax.swing.JComponent

class JwtEncoderDecoder(
  private val context: DeveloperUiToolContext,
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  private val project: Project?,
) : DeveloperUiTool(parentDisposable) {
  // -- Properties ---------------------------------------------------------- //

  private var liveConversion = configuration.register("liveConversion", true)
  private var encodedText = configuration.register("encodedText", "", INPUT, EXAMPLE_ENCODED)
  private var headerText = configuration.register("headerText", "", INPUT, exampleHeader)
  private var payloadText = configuration.register("payloadText", "", INPUT, examplePayload)

  private val conversionAlarm by lazy { Alarm(parentDisposable) }
  private val validationAlarm by lazy { Alarm(parentDisposable) }

  private var selectedTab = JwtTab.DECODE_ENCODE
  private val encodedEditorLabel = AtomicProperty(sharedEncodedEditorLabel(selectedTab))
  private var lastActiveInput: AdvancedEditor? = null
  private val encodedEditor by lazy { createEncodedEditor() }
  private val headerEditor by lazy { createHeaderEditor() }
  private val payloadEditor by lazy { createPayloadEditor() }

  private val highlightingAttributes by lazy {
    EditorColorsManager.getInstance()
      .globalScheme
      .getAttributes(EditorColors.SEARCH_RESULT_ATTRIBUTES)
  }

  private val highlighter by lazy {
    JwtEditorHighlighter(parentDisposable, encodedText, highlightingAttributes)
  }
  private val jwt = Jwt(configuration, encodedText, headerText, payloadText)
  private val validation = JwtValidation(configuration)

  // -- Initialization ------------------------------------------------------ //

  init {
    liveConversion.afterChange(parentDisposable) { handleLiveConversionSwitch() }

    jwt.signature.secretEncodingMode.afterChangeConsumeEvent(null) { event ->
      if (event.valueChanged()) {
        convertFromUi(ChangeOrigin.SIGNATURE_CONFIGURATION)
      }
    }

    validation.secretEncodingMode.afterChangeConsumeEvent(null) { event ->
      if (event.valueChanged()) {
        validateFromUi()
      }
    }
  }

  // -- Exposed Methods ----------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
        cell(
            JBSplitter(true, 0.2f).apply {
              firstComponent = createSharedEncodedComponent()
              secondComponent =
                JBTabbedPane().apply {
                  tabComponentInsets = JBUI.emptyInsets()

                  addTab(
                    UiToolsBundle.message("jwt-encoder-decoder.tab.decode-encode"),
                    createTabContent(JwtTab.DECODE_ENCODE, createEncodingDecodingTab()),
                  )
                  addTab(
                    UiToolsBundle.message("jwt-encoder-decoder.tab.validate"),
                    createTabContent(JwtTab.VALIDATE, createValidationTab()),
                  )

                  onSelectionChanged { selectedComponent ->
                    selectedComponent.getUserData(jwtTabKey)?.let { handleTabSelectionChanged(it) }
                  }
                }
            }
          )
          .align(Align.FILL)
          .resizableColumn()
      }
      .resizableRow()
  }

  override fun afterBuildUi() {
    convertFromUi(ChangeOrigin.ENCODED)
    validateFromUi()
  }

  override fun reset() {
    convert(ChangeOrigin.ENCODED)
    validateJwt()
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun createSharedEncodedComponent(): JComponent = panel {
    row { label("").bindText(encodedEditorLabel).resizableColumn() }.bottomGap(BottomGap.NONE)
    row {
        cell(encodedEditor.component)
          .validationOnApply(encodedEditor.bindValidator(jwt.encodedErrorHolder.asValidation()))
          .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
          .align(Align.FILL)
          .resizableColumn()
      }
      .resizableRow()
  }

  private fun createEncodingDecodingTab(): JComponent = panel {
    val signatureErrors = jwt.signatureErrorHolder.asComponentPredicate()
    row {
        text("")
          .bindText(jwt.signatureErrorHolder.asPropertyForTextCell())
          .visibleIf(signatureErrors)
          .resizableColumn()
      }
      .topGap(TopGap.NONE)
    row {
        cell(createEncodingDecodingComponent()).align(Align.FILL).resizableColumn()
      }
      .resizableRow()
      .topGap(TopGap.NONE)
  }

  private fun createValidationTab(): JComponent = panel {
    row { text("").bindText(validation.tokenMetadata).resizableColumn() }.topGap(TopGap.NONE)
    row {
        text("")
          .bindText(validation.validResultMessage)
          .visibleIf(
            PropertyComponentPredicate(validation.resultState, ValidationResultState.VALID)
          )
          .resizableColumn()
        text("")
          .bindText(validation.invalidResultMessage)
          .visibleIf(
            PropertyComponentPredicate(validation.resultState, ValidationResultState.INVALID)
          )
          .resizableColumn()
      }
      .topGap(TopGap.NONE)
    row {
        cell(createValidationComponent()).align(Align.FILL).resizableColumn()
      }
      .resizableRow()
      .topGap(TopGap.NONE)
  }

  @Suppress("UnstableApiUsage")
  private fun createEncodingDecodingComponent(): JComponent = panel {
    row {
      val liveConversionCheckBox =
        checkBox(UiToolsBundle.message("converter.live-conversion"))
          .bindSelected(liveConversion)
          .gap(RightGap.SMALL)

      button(UiToolsBundle.message("jwt-encoder-decoder.button.decode")) {
          convert(ChangeOrigin.ENCODED)
        }
        .enabledIf(liveConversionCheckBox.selected.not())
        .gap(RightGap.SMALL)
      button(UiToolsBundle.message("jwt-encoder-decoder.button.encode")) {
          convert(ChangeOrigin.SIGNATURE_CONFIGURATION)
        }
        .enabledIf(liveConversionCheckBox.selected.not())
    }

    if (context.prioritizeVerticalLayout) {
      row {
          cell(
              Splitter(true, 0.3f).apply {
                firstComponent = createHeaderEditorComponent()
                secondComponent = createPayloadEditorComponent()
              }
            )
            .align(Align.FILL)
            .resizableColumn()
        }
        .resizableRow()
        .bottomGap(BottomGap.NONE)
    } else {
      row {
          cell(
              Splitter(false, 0.5f).apply {
                firstComponent = createHeaderEditorComponent()
                secondComponent = createPayloadEditorComponent()
              }
            )
            .align(Align.FILL)
            .resizableColumn()
        }
        .resizableRow()
        .bottomGap(BottomGap.NONE)
    }

    collapsibleGroup(UiToolsBundle.message("jwt-encoder-decoder.signature-configuration.title")) {
        lateinit var signatureAlgorithmComboBox: ComboBox<SignatureAlgorithm>
        row {
            signatureAlgorithmComboBox =
              comboBox(SignatureAlgorithm.entries)
                .label(
                  UiToolsBundle.message("jwt-encoder-decoder.signature-configuration.algorithm")
                )
                .bindItem(jwt.signature.algorithm)
                .whenItemSelectedFromUi { convertFromUi(ChangeOrigin.SIGNATURE_CONFIGURATION) }
                .component
          }
          .layout(RowLayout.PARENT_GRID)
          .topGap(TopGap.NONE)

        row {
            label(UiToolsBundle.message("jwt-encoder-decoder.signature-configuration.secret-key"))
            expandableTextField()
              .align(AlignX.FILL)
              .bindText(jwt.signature.secret)
              .whenTextChangedFromUi { convertFromUi(ChangeOrigin.SIGNATURE_CONFIGURATION) }
              .gap(RightGap.SMALL)
              .resizableColumn()
              .registerDynamicToolTip { generalSettings.createSensitiveInputsHandlingToolTipText() }

            val encodingActions =
              mutableListOf<AnAction>().apply {
                SecretKeyEncodingMode.entries.forEach { secretKeyEncodingModeValue ->
                  add(
                    SimpleToggleAction(
                      text = secretKeyEncodingModeValue.title,
                      icon = AllIcons.Actions.ToggleSoftWrap,
                      isSelected = {
                        jwt.signature.secretEncodingMode.get() == secretKeyEncodingModeValue
                      },
                      setSelected = {
                        jwt.signature.secretEncodingMode.set(secretKeyEncodingModeValue)
                      },
                    )
                  )
                }
              }
            actionButton(
              UiUtils.actionsPopup(
                title =
                  UiToolsBundle.message("jwt-encoder-decoder.signature-configuration.encoding"),
                icon = AllIcons.General.Settings,
                actions = encodingActions,
              )
            )
          }
          .visibleIf(
            ComboBoxPredicate(signatureAlgorithmComboBox) {
              it?.kind == SignatureAlgorithmKind.HMAC
            }
          )
          .layout(RowLayout.PARENT_GRID)

        row {
            textArea()
              .rows(5)
              .align(Align.FILL)
              .label(
                label =
                  UiToolsBundle.message("jwt-encoder-decoder.signature-configuration.private-key"),
                position = LabelPosition.TOP,
              )
              .bindText(jwt.signature.privateKey)
              .setValidationResultBorder()
              .whenTextChangedFromUi { convertFromUi(ChangeOrigin.SIGNATURE_CONFIGURATION) }
              .validationInfo(jwt.signature.privateKeyErrorHolder.asValidation())
              .registerDynamicToolTip { generalSettings.createSensitiveInputsHandlingToolTipText() }
          }
          .visibleIf(ComboBoxPredicate(signatureAlgorithmComboBox) { it?.kind?.keyFactory != null })

        row {
            checkBox(UiToolsBundle.message("jwt-encoder-decoder.strict-key-validation"))
              .bindSelected(jwt.signature.strictSigningKeyValidation)
              .whenStateChangedFromUi { convertFromUi(ChangeOrigin.SIGNATURE_CONFIGURATION) }
              .gap(RightGap.SMALL)
            contextHelp(UiToolsBundle.message("jwt-encoder-decoder.strict-key-validation.help"))
          }
          .visibleIf(
            ComboBoxPredicate(signatureAlgorithmComboBox) {
              it?.kind != SignatureAlgorithmKind.NONE
            }
          )
      }
      .apply { expanded = false }
      .topGap(TopGap.NONE)
  }

  @Suppress("UnstableApiUsage")
  private fun createValidationComponent(): JComponent = panel {
    lateinit var validationSourceComboBox: ComboBox<ValidationKeySource>
    row {
        validationSourceComboBox =
          comboBox(ValidationKeySource.entries)
            .label(UiToolsBundle.message("jwt-encoder-decoder.validation.key-source"))
            .bindItem(validation.keySource)
            .whenItemSelectedFromUi { validateFromUi() }
            .component
      }
      .layout(RowLayout.PARENT_GRID)
      .topGap(TopGap.NONE)

    row {
        label(UiToolsBundle.message("jwt-encoder-decoder.signature-configuration.secret-key"))
        expandableTextField()
          .align(AlignX.FILL)
          .bindText(validation.secret)
          .whenTextChangedFromUi { validateFromUi() }
          .gap(RightGap.SMALL)
          .resizableColumn()
          .registerDynamicToolTip { generalSettings.createSensitiveInputsHandlingToolTipText() }

        val encodingActions =
          mutableListOf<AnAction>().apply {
            SecretKeyEncodingMode.entries.forEach { secretKeyEncodingModeValue ->
              add(
                SimpleToggleAction(
                  text = secretKeyEncodingModeValue.title,
                  icon = AllIcons.Actions.ToggleSoftWrap,
                  isSelected = {
                    validation.secretEncodingMode.get() == secretKeyEncodingModeValue
                  },
                  setSelected = { validation.secretEncodingMode.set(secretKeyEncodingModeValue) },
                )
              )
            }
          }
        actionButton(
          UiUtils.actionsPopup(
            title = UiToolsBundle.message("jwt-encoder-decoder.signature-configuration.encoding"),
            icon = AllIcons.General.Settings,
            actions = encodingActions,
          )
        )
      }
      .visibleIf(ComboBoxPredicate(validationSourceComboBox) { it == ValidationKeySource.SECRET })
      .layout(RowLayout.PARENT_GRID)

    row {
        textArea()
          .rows(5)
          .align(Align.FILL)
          .label(
            label = UiToolsBundle.message("jwt-encoder-decoder.validation.public-key-or-jwk"),
            position = LabelPosition.TOP,
          )
          .bindText(validation.publicKey)
          .setValidationResultBorder()
          .whenTextChangedFromUi { validateFromUi() }
          .registerDynamicToolTip { generalSettings.createSensitiveInputsHandlingToolTipText() }
      }
      .visibleIf(
        ComboBoxPredicate(validationSourceComboBox) { it == ValidationKeySource.PUBLIC_KEY }
      )

    row {
        val jwksFetchEnabled = PropertyComponentPredicate(validation.fetchingJwks, false)

        textField()
          .align(Align.FILL)
          .label(UiToolsBundle.message("jwt-encoder-decoder.validation.jwks-url"))
          .bindText(validation.jwksUrl)
          .whenTextChangedFromUi { validateFromUi() }
          .gap(RightGap.SMALL)
          .resizableColumn()
          .enabledIf(jwksFetchEnabled)
        button(UiToolsBundle.message("jwt-encoder-decoder.validation.fetch")) { fetchJwks() }
          .enabledIf(jwksFetchEnabled)
      }
      .visibleIf(ComboBoxPredicate(validationSourceComboBox) { it == ValidationKeySource.JWKS })
      .layout(RowLayout.PARENT_GRID)

    row {
        textArea()
          .rows(8)
          .align(Align.FILL)
          .label(
            label = UiToolsBundle.message("jwt-encoder-decoder.validation.jwks-json"),
            position = LabelPosition.TOP,
          )
          .bindText(validation.jwksJson)
          .setValidationResultBorder()
          .whenTextChangedFromUi { validateFromUi() }
      }
      .visibleIf(ComboBoxPredicate(validationSourceComboBox) { it == ValidationKeySource.JWKS })

    row {
        checkBox(UiToolsBundle.message("jwt-encoder-decoder.strict-key-validation"))
          .bindSelected(validation.strictKeyValidation)
          .whenStateChangedFromUi { validateFromUi() }
          .gap(RightGap.SMALL)
        contextHelp(UiToolsBundle.message("jwt-encoder-decoder.strict-key-validation.help"))
      }
      .topGap(TopGap.NONE)
  }

  private fun createPayloadEditorComponent(): JComponent = panel {
    row {
        cell(payloadEditor.component)
          .validationOnApply(payloadEditor.bindValidator(jwt.payloadErrorHolder.asValidation()))
          .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
          .align(Align.FILL)
          .resizableColumn()
      }
      .resizableRow()
  }

  private fun createHeaderEditorComponent(): JComponent = panel {
    row {
        cell(headerEditor.component)
          .validationOnApply(headerEditor.bindValidator(jwt.headerErrorHolder.asValidation()))
          .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
          .align(Align.FILL)
          .resizableColumn()
      }
      .resizableRow()
  }

  private fun convertFromUi(changeOrigin: ChangeOrigin) {
    if (!liveConversion.get()) {
      return
    }

    convert(changeOrigin)
  }

  private fun validateFromUi() {
    if (configuration.isResetting) {
      return
    }

    if (!isDisposed && !validationAlarm.isDisposed) {
      validationAlarm.cancelAllRequests()
      validationAlarm.addRequest({ validateJwt() }, 100)
    }
  }

  private fun convert(changeOrigin: ChangeOrigin) {
    if (configuration.isResetting) {
      return
    }

    if (!isDisposed && !conversionAlarm.isDisposed) {
      conversionAlarm.cancelAllRequests()
      conversionAlarm.addRequest({ doConvert(changeOrigin) }, 100)
    }
  }

  private fun doConvert(changeOrigin: ChangeOrigin) {
    when (changeOrigin) {
      ChangeOrigin.ENCODED -> jwt.decodeJwt()
      ChangeOrigin.HEADER_OR_PAYLOAD -> jwt.encodeJwt()
      ChangeOrigin.SIGNATURE_CONFIGURATION -> {
        jwt.setAlgorithmInHeader()
        jwt.encodeJwt()
      }
    }

    validation.validate(encodedText.get())
    refreshHighlights()
    validate()
  }

  private fun validateJwt() {
    jwt.decodeJwt()
    validation.validate(encodedText.get())
    refreshHighlights()
    validate()
  }

  private fun refreshHighlights() {
    highlighter.refresh(
      encodedEditor = encodedEditor,
      headerEditor = headerEditor,
      payloadEditor = payloadEditor,
      isToolDisposed = isDisposed,
    )
  }

  private fun createEncodedEditor(): AdvancedEditor =
    createEditor(
      id = "encoded",
      changeOrigin = ChangeOrigin.ENCODED,
      title = null,
      language = PlainTextLanguage.INSTANCE,
      textProperty = encodedText,
    ) {
      if (selectedTab == JwtTab.VALIDATE && !liveConversion.get()) {
        validateFromUi()
      }
      highlighter.scheduleDotSeparatorHighlight(encodedEditor, isDisposed)
    }

  private fun createHeaderEditor(): AdvancedEditor =
    createEditor(
      id = "header",
      changeOrigin = ChangeOrigin.HEADER_OR_PAYLOAD,
      title = UiToolsBundle.message("jwt-encoder-decoder.editor.header"),
      language = JsonLanguage.INSTANCE,
      textProperty = headerText,
    ) {
      highlighter.scheduleHeaderClaimsHighlight(headerEditor, isDisposed)
    }

  private fun createPayloadEditor(): AdvancedEditor =
    createEditor(
      id = "payload",
      changeOrigin = ChangeOrigin.HEADER_OR_PAYLOAD,
      title = UiToolsBundle.message("jwt-encoder-decoder.editor.payload"),
      language = JsonLanguage.INSTANCE,
      textProperty = payloadText,
    ) {
      highlighter.schedulePayloadClaimsHighlight(payloadEditor, isDisposed)
    }

  private fun createEditor(
    id: String,
    changeOrigin: ChangeOrigin,
    title: String?,
    language: Language,
    textProperty: ValueProperty<String>,
    onTextChangeFromUi: (() -> Unit)? = null,
  ) =
    AdvancedEditor(
        id = id,
        context = context,
        configuration = configuration,
        project = project,
        title = title,
        editorMode = EditorMode.INPUT_OUTPUT,
        parentDisposable = parentDisposable,
        textProperty = textProperty,
        initialLanguage = language,
      )
      .apply {
        onFocusGained { lastActiveInput = this }
        this.onTextChangeFromUi { _ ->
          lastActiveInput = this
          convertFromUi(changeOrigin)
          onTextChangeFromUi?.invoke()
        }
      }

  private fun handleLiveConversionSwitch() {
    if (liveConversion.get()) {
      when (lastActiveInput) {
        encodedEditor -> convert(ChangeOrigin.ENCODED)
        headerEditor,
        payloadEditor -> convert(ChangeOrigin.SIGNATURE_CONFIGURATION)

        null -> {}
      }
    }
  }

  private fun createTabContent(jwtTab: JwtTab, component: JComponent): JComponent =
    component.apply { putUserData(jwtTabKey, jwtTab) }.wrapTabbedPaneContent()

  private fun handleTabSelectionChanged(jwtTab: JwtTab) {
    selectedTab = jwtTab
    encodedEditorLabel.set(sharedEncodedEditorLabel(jwtTab))
    if (jwtTab == JwtTab.VALIDATE) {
      validateFromUi()
    }
  }

  private fun sharedEncodedEditorLabel(jwtTab: JwtTab): String =
    when (jwtTab) {
      JwtTab.DECODE_ENCODE ->
        UiToolsBundle.message("jwt-encoder-decoder.editor.jwt-input-output")
      JwtTab.VALIDATE -> UiToolsBundle.message("jwt-encoder-decoder.editor.jwt-input")
    }

  private fun fetchJwks() {
    validation.fetchJwks(project) { validateJwt() }
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<JwtEncoderDecoder> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("jwt-encoder-decoder.menu-title"),
        contentTitle = UiToolsBundle.message("jwt-encoder-decoder.content-title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> JwtEncoderDecoder) = { configuration ->
      JwtEncoderDecoder(context, configuration, parentDisposable, project)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {
    private val jwtTabKey = com.intellij.openapi.util.Key.create<JwtTab>("jwtTab")

    internal val urlEncoder: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()

    internal val unixTimestampSecondsJsonValueRegex =
      Regex(":\\s*(?<unixTimestampSeconds>\\b\\d{1,10}\\b)")
    internal val claimRegex = Regex("\\s?\"(?<name>[a-zA-Z]+)\"\\s?:")
    internal val rawKeyRegex = Regex("\\r?\\n|\\r|\\s?-+(BEGIN|END).*KEY-+\\s?")

    internal val defaultSignatureAlgorithm = SignatureAlgorithm.HMAC256
    internal const val SIGNING_KEY_VALIDATION_DEFAULT = false

    internal const val EXAMPLE_ENCODED =
      "ewogICJ0eXAiOiJKV1QiLAogICJhbGciOiJIUzI1NiIKfQ.ewogICJqdGkiOiI5NjQ5MmQ1OS0wYWQ1LTRjMDAtODkyZC01OTBhZDVhYzAwZjMiLAogICJzdWIiOiIwMTIzNDU2Nzg5IiwKICAibmFtZSI6IkpvaG4gRG9lIiwKICAiaWF0IjoxNjgxMDQwNTE1Cn0.IqeNl3lHSUfPfEYmttvlQp1sH9LpAoPJlUiSv4XPDSE"
    internal const val EXAMPLE_SECRET = "s3cre!"
    internal val exampleHeader =
      """
          {
            "typ":"JWT",
            "alg":"HS256"
          }
          """
        .trimIndent()
    internal val examplePayload =
      """
          {
            "jti":"96492d59-0ad5-4c00-892d-590ad5ac00f3",
            "sub":"0123456789",
            "name":"John Doe",
            "iat":1681040515
          }
          """
        .trimIndent()

    internal val exampleRsaPrivateKey =
      """
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
      """
        .trimIndent()

    internal val exampleEcPrivateKey =
      """
-----BEGIN EC PRIVATE KEY-----
MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCDQ+B6qEzr/M2sql4X+09X9YlYt8BKA
HX8Q7/6s4KC3qQ==
-----END RSA PRIVATE KEY-----
      """
        .trimIndent()
  }

  private enum class JwtTab {
    DECODE_ENCODE,
    VALIDATE,
  }
}
