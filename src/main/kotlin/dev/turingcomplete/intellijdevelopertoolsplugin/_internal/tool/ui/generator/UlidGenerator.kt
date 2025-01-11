package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.generator

import com.github.f4b6a3.ulid.Ulid
import com.github.f4b6a3.ulid.UlidCreator
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.openapi.util.text.StringUtil
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.actionButton
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.CopyAction
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.bindLongTextImproved
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.not
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.validateLongValue
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolPresentation
import java.time.format.DateTimeFormatter

class UlidGenerator(
  project: Project?,
  context: DeveloperUiToolContext,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : OneLineTextGenerator(
  context = context,
  configuration = configuration,
  parentDisposable = parentDisposable,
  project = project
), DataProvider {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val generateMonotonicUlid = configuration.register("generateMonotonicUlid", false)
  private val ulidFormat = configuration.register("ulidFormat", UlidFormat.NONE)
  private val useIndividualTime = configuration.register("useIndividualTime", false)
  private val individualTime = configuration.register("individualTime", System.currentTimeMillis())

  private val parseUlidInput = ValueProperty(UlidCreator.getUlid().toString())
  private val parseUlidTransformFormat = ValueProperty(UlidFormat.UUID)
  private val parsedUlIdTimestamp = ValueProperty("")
  private val parsedUlIdTransformed = ValueProperty("")

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  
  init {
    parseUlidInput.afterChange { parseUlid() }
    parseUlidTransformFormat.afterChange { parseUlid() }
    parseUlid()
  }

  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun Panel.buildConfigurationUi() {
    row {
      comboBox(UlidFormat.entries)
        .label("Format:")
        .bindItem(ulidFormat)
    }
    row {
      checkBox("Monotonic")
        .bindSelected(generateMonotonicUlid)
        .gap(RightGap.SMALL)
      contextHelp("If selected, the random component is incremented for each new ULID generated in the same millisecond. Otherwise, the random component is reset for each new ULID generated.")
    }

    buttonsGroup {
      row {
        radioButton("Use current Unix timestamp")
          .bindSelected(useIndividualTime.not())
      }
      row {
        radioButton("Use individual timestamp:")
          .bindSelected(useIndividualTime)
          .gap(RightGap.SMALL)
        textField().validateLongValue(LongRange(0, Long.MAX_VALUE))
          .gap(RightGap.SMALL)
          .enabledIf(useIndividualTime)
          .bindLongTextImproved(individualTime)
          .columns(12)
      }
    }
  }

  override fun Panel.buildAdditionalUi() {
    group("Parse ULID") {
      row {
        expandableTextField()
          .bindText(parseUlidInput)
          .validationOnInput { if (!Ulid.isValid(it.text)) ValidationInfo("Invalid ULID") else null }
          .align(Align.FILL)
      }
      row {
        label("")
          .label("Timestamp:")
          .bindText(parsedUlIdTimestamp)
          .gap(RightGap.SMALL)
        actionButton(CopyAction(parsedUlIdTimestampDataKey), UlidGenerator::class.java.name)
      }
      row {
        comboBox(UlidFormat.entries.filter { it != UlidFormat.NONE })
          .label("Transform to:")
          .bindItem(parseUlidTransformFormat)
      }.bottomGap(BottomGap.NONE)
      row {
        label("")
          .bindText(parsedUlIdTransformed)
          .gap(RightGap.SMALL)
        actionButton(CopyAction(parsedUlIdTransformedDataKey), UlidGenerator::class.java.name)
      }.topGap(TopGap.NONE)
    }
  }

  override fun generate(): String {
    val ulid: Ulid = if (generateMonotonicUlid.get()) {
      if (useIndividualTime.get()) {
        UlidCreator.getMonotonicUlid(individualTime.get())
      }
      else {
        UlidCreator.getMonotonicUlid()
      }
    }
    else {
      if (useIndividualTime.get()) {
        UlidCreator.getUlid(individualTime.get())
      }
      else {
        UlidCreator.getUlid()
      }
    }
    return ulidFormat.get().format(ulid)
  }

  override fun getData(dataId: String): Any? = when {
    parsedUlIdTimestampDataKey.`is`(dataId) -> StringUtil.stripHtml(parsedUlIdTimestamp.get(), false)
    parsedUlIdTransformedDataKey.`is`(dataId) -> StringUtil.stripHtml(parsedUlIdTransformed.get(), false)
    else -> super.getData(dataId)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun parseUlid() {
    val parseUlidInputValue = parseUlidInput.get()
    if (!Ulid.isValid(parseUlidInputValue)) {
      return
    }

    val ulid = Ulid.from(parseUlidInputValue)
    parsedUlIdTimestamp.set("<html><code>${ulid.time}</code> (${DateTimeFormatter.ISO_INSTANT.format(ulid.instant)})")
    parsedUlIdTransformed.set("<html><code>${parseUlidTransformFormat.get().format(ulid)}</code></html>")
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class UlidFormat(val title: String, val format: (Ulid) -> String) {

    NONE("None", { it.toString() }),
    TO_LOWERCASE("Lowercase", { it.toLowerCase() }),
    UUID("UUID", { it.toUuid().toString() }),
    RFC_4122_UUID("RFC-4122 UUIDv4", { it.toRfc4122().toUuid().toString() });

    override fun toString(): String = title
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<UlidGenerator> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "ULID",
      contentTitle = "ULID Generator"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> UlidGenerator) = { configuration ->
      UlidGenerator(project, context, configuration, parentDisposable)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val parsedUlIdTimestampDataKey = DataKey.create<String>("parsedUlIdTimestamp")
    private val parsedUlIdTransformedDataKey = DataKey.create<String>("parsedUlIdTransformed")
  }
}