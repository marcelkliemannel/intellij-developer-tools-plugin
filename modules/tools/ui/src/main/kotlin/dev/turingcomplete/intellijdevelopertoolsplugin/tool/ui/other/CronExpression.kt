package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other

import com.cronutils.descriptor.CronDescriptor
import com.cronutils.model.Cron
import com.cronutils.model.CronType
import com.cronutils.model.definition.CronDefinition
import com.cronutils.model.definition.CronDefinitionBuilder
import com.cronutils.model.definition.CronNicknames
import com.cronutils.model.field.CronField
import com.cronutils.model.field.CronFieldName
import com.cronutils.model.field.definition.FieldDefinition
import com.cronutils.model.field.value.SpecialChar
import com.cronutils.model.time.ExecutionTime
import com.cronutils.parser.CronParser
import com.cronutils.parser.CronParserField
import com.intellij.DynamicBundle
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.ui.dsl.listCellRenderer.textListCellRenderer
import com.intellij.util.text.DateFormatUtil
import dev.turingcomplete.intellijdevelopertoolsplugin.common.I18nUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.capitalize
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ifNotEmpty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AsyncTaskExecutor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AsyncTaskExecutor.Companion.defaultUiInputDelay
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.PropertyComponentPredicate.Companion.createPredicate
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import java.time.ZonedDateTime

class CronExpression(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  DeveloperUiTool(parentDisposable = parentDisposable) {
  // -- Properties ---------------------------------------------------------- //

  private val selectedCronType =
    configuration.register(key = "selectedCronType", defaultValue = CronType.UNIX)

  private val cronInstances = CronType.entries.associateWith { CronInstance(it, configuration) }

  private val updateCronHandlerExecutor by lazy { AsyncTaskExecutor.onEdt(parentDisposable) }

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
        comboBox(items = cronInstances.keys, renderer = textListCellRenderer { it!!.title() })
          .bindItem(selectedCronType)
          .gap(RightGap.SMALL)
          .label(UiToolsBundle.message("cron-expression.cron-type"))
      }
      .bottomGap(BottomGap.MEDIUM)

    cronInstances.forEach { (cronType, cronInstance) ->
      rowsRange {
          row {
              expandableTextField()
                .bindText(cronInstance.cronExpression)
                .label(UiToolsBundle.message("cron-expression.cron-expression"), LabelPosition.TOP)
                .whenTextChangedFromUi { updateCronFieldValues(cronInstance) }
                .validationOnApply(cronInstance.cronExpressionErrorHolder.asValidation())
                .align(Align.FILL)
            }
            .bottomGap(BottomGap.NONE)

          row {
              cronInstance.cronDefinition.cronNicknames.ifNotEmpty {
                comment(
                  UiToolsBundle.message(
                    "cron-expression.cron-expression.macros",
                    I18nUtils.formatLocalizedList(this.sorted().map { it.expression() }),
                  )
                )
              }
            }
            .bottomGap(BottomGap.MEDIUM)

          rowsRange {
            cronInstance.cronFieldInstances.forEach { fieldInstance ->
              row {
                  textField()
                    .bindText(fieldInstance.fieldValue)
                    .whenTextChangedFromUi { updateCronExpression(cronInstance) }
                    .columns(10)
                    .validationOnApply(fieldInstance.fieldErrorHolder.asValidation())
                    .label(fieldInstance.fieldDefinition.inputLabel())
                    .gap(RightGap.SMALL)

                  contextHelp(fieldInstance.fieldDefinition.describe())
                }
                .layout(RowLayout.PARENT_GRID)
            }
            row { comment(UiToolsBundle.message("cron-expression.field-formats")) }
          }

          rowsRange {
            row {
                text("")
                  .bindText(cronInstance.cronExplanation)
                  .label(UiToolsBundle.message("cron-expression.cron-explanation"))
              }
              .topGap(TopGap.MEDIUM)
              .bottomGap(BottomGap.NONE)
              .layout(RowLayout.PARENT_GRID)
            row {
                text("")
                  .bindText(cronInstance.cronLastExecution)
                  .label(UiToolsBundle.message("cron-expression.cron-last-execution"))
              }
              .bottomGap(BottomGap.NONE)
              .layout(RowLayout.PARENT_GRID)
            row {
                text("")
                  .bindText(cronInstance.cronNextExecution)
                  .label(UiToolsBundle.message("cron-expression.cron-next-execution"))
              }
              .layout(RowLayout.PARENT_GRID)
          }
        }
        .visibleIf(selectedCronType.createPredicate(cronType))
    }
  }

  override fun afterBuildUi() {
    selectedCronType.afterChange(parentDisposable) { updateCronFieldValues(cronInstances[it]!!) }

    updateCronFieldValues(cronInstances[selectedCronType.get()]!!)
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun updateCronFieldValues(cronInstance: CronInstance) {
    updateCronHandlerExecutor.replaceTasks(defaultUiInputDelay) {
      cronInstance.parseCronExpression()

      // Show errors
      validate(true)
    }
  }

  private fun updateCronExpression(cronInstance: CronInstance) {
    updateCronHandlerExecutor.replaceTasks(defaultUiInputDelay) {
      cronInstance.parseCronFields()

      // Show errors
      validate(true)
    }
  }

  private fun FieldDefinition.inputLabel() =
    when (fieldName) {
      CronFieldName.SECOND -> UiToolsBundle.message("cron-expression.cron-fields.input-second")
      CronFieldName.MINUTE -> UiToolsBundle.message("cron-expression.cron-fields.input-minute")
      CronFieldName.HOUR -> UiToolsBundle.message("cron-expression.cron-fields.input-hour")
      CronFieldName.DAY_OF_MONTH ->
        UiToolsBundle.message("cron-expression.cron-fields.input-day-of-month")
      CronFieldName.MONTH -> UiToolsBundle.message("cron-expression.cron-fields.input-month")
      CronFieldName.DAY_OF_WEEK ->
        UiToolsBundle.message("cron-expression.cron-fields.input-day-of-week")
      CronFieldName.YEAR ->
        if (isOptional) {
          UiToolsBundle.message("cron-expression.cron-fields.input-year-optional")
        } else {
          UiToolsBundle.message("cron-expression.cron-fields.input-year")
        }
      CronFieldName.DAY_OF_YEAR ->
        UiToolsBundle.message("cron-expression.cron-fields.input-day-of-year")
    }

  private fun CronType.title() =
    when (this) {
      CronType.CRON4J -> UiToolsBundle.message("cron-expression.cron-type.cron4j")
      CronType.QUARTZ -> UiToolsBundle.message("cron-expression.cron-type.quartz")
      CronType.UNIX -> UiToolsBundle.message("cron-expression.cron-type.unix")
      CronType.SPRING -> UiToolsBundle.message("cron-expression.cron-type.spring-before-53")
      CronType.SPRING53 -> UiToolsBundle.message("cron-expression.cron-type.spring-after-53")
    }

  private fun CronNicknames.expression(): String =
    when (this) {
      CronNicknames.YEARLY -> "@yearly"
      CronNicknames.ANNUALLY -> "@annually"
      CronNicknames.MONTHLY -> "@monthly"
      CronNicknames.WEEKLY -> "@weekly"
      CronNicknames.DAILY -> "@daily"
      CronNicknames.MIDNIGHT -> "@midnight"
      CronNicknames.HOURLY -> "@hourly"
      CronNicknames.REBOOT -> "@reboot"
    }

  private fun FieldDefinition.describe(): String {
    val characters = mutableListOf<String>()

    with(constraints) {
      val valueMappings: MutableMap<Int, String> = mutableMapOf()
      stringMappingKeySet.forEach {
        val valueMapping = getStringMappingValue(it)
        valueMappings[valueMapping] = it
        getIntMappingValue(endRange)?.let { equivalentValue -> valueMappings[equivalentValue] = it }
      }
      val startRangeValueMapping: String? = valueMappings[startRange]
      val endRangeValueMapping: String? = valueMappings[endRange]
      if (startRangeValueMapping != null && endRangeValueMapping != null) {
        characters +=
          UiToolsBundle.message(
            "cron-expression.field-constraints.numbers-with-value-mapping",
            startRange.toString(),
            startRangeValueMapping,
            endRange.toString(),
            endRangeValueMapping,
          )
      } else {
        characters +=
          UiToolsBundle.message(
            "cron-expression.field-constraints.numbers",
            startRange.toString(),
            endRange.toString(),
          )
      }

      if (stringMappingKeySet.contains("MON")) {
        characters += UiToolsBundle.message("cron-expression.field-constraints.named-day-values")
      }

      if (stringMappingKeySet.contains("JAN")) {
        characters += UiToolsBundle.message("cron-expression.field-constraints.named-month-values")
      }

      specialChars.forEach { special ->
        when (special) {
          SpecialChar.LW ->
            characters +=
              UiToolsBundle.message("cron-expression.field-constraints.special.last-weekday")

          SpecialChar.L ->
            when (fieldName) {
              CronFieldName.DAY_OF_MONTH ->
                characters +=
                  UiToolsBundle.message(
                    "cron-expression.field-constraints.special.last-day-of-month"
                  )

              CronFieldName.DAY_OF_WEEK ->
                characters +=
                  UiToolsBundle.message(
                    "cron-expression.field-constraints.special.last-occurrence-of-a-weekday"
                  )

              else -> Unit
            }

          SpecialChar.W ->
            characters +=
              UiToolsBundle.message("cron-expression.field-constraints.special.nearest-weekday")

          SpecialChar.HASH ->
            characters +=
              UiToolsBundle.message("cron-expression.field-constraints.special.weekday-month")

          SpecialChar.QUESTION_MARK ->
            characters +=
              UiToolsBundle.message("cron-expression.field-constraints.special.no-specific-value")

          SpecialChar.NONE -> Unit
        }
      }

      characters += UiToolsBundle.message("cron-expression.field-constraints.special.asterix")
    }

    return UiToolsBundle.message(
      "cron-expression.field-constraints.description",
      characters.joinToString(separator = "") { "<li>$it</li>" },
    )
  }

  // -- Inner Type ---------------------------------------------------------- //

  private data class CronFieldInstance(val fieldDefinition: FieldDefinition) {

    val fieldValue = ValueProperty("")
    val fieldErrorHolder = ErrorHolder()

    private val cronFieldParser =
      CronParserField(fieldDefinition.fieldName, fieldDefinition.constraints)

    fun parseCronField(): CronField? =
      try {
        fieldErrorHolder.clear()
        val finalFieldValue = fieldValue.get()
        if (finalFieldValue.isBlank() && fieldDefinition.isOptional) {
          null
        } else {
          cronFieldParser.parse(finalFieldValue)
        }
      } catch (e: Exception) {
        fieldErrorHolder.add(e)
        null
      }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private data class CronInstance(
    val cronType: CronType,
    val configuration: DeveloperToolConfiguration,
  ) {

    val cronDefinition: CronDefinition = CronDefinitionBuilder.instanceDefinitionFor(cronType)
    private val cronParser: CronParser = CronParser(cronDefinition)

    val cronExpression =
      configuration.register(
        key = "cronExpression-${cronType.name}",
        defaultValue = cronType.defaultCronExpression(),
        propertyType = INPUT,
        example = cronType.exampleCronExpression(),
      )
    val cronExpressionErrorHolder = ErrorHolder()
    val cronFieldInstances: List<CronFieldInstance> =
      cronDefinition.fieldDefinitions.sortedBy { it.fieldName.order }.map { CronFieldInstance(it) }
    val cronExplanation = ValueProperty(UiToolsBundle.message("cron-expression.unknown"))
    val cronLastExecution = ValueProperty(UiToolsBundle.message("cron-expression.unknown"))
    val cronNextExecution = ValueProperty(UiToolsBundle.message("cron-expression.unknown"))

    fun parseCronExpression() {
      val cron = parseExpression()
      if (cron != null) {
        cronFieldInstances.forEach { cronField ->
          cronField.fieldValue.set(
            cron.retrieve(cronField.fieldDefinition.fieldName)?.expression?.asString() ?: ""
          )
        }
      }
      updateInformation(cron)
    }

    fun parseCronFields() {
      val newCronExpression =
        cronFieldInstances
          .mapNotNull { cronFieldInstance ->
            val cronField = cronFieldInstance.parseCronField()

            val fieldValue = cronFieldInstance.fieldValue.get()
            if (fieldValue.isBlank() && cronFieldInstance.fieldDefinition.isOptional) {
              null
            } else {
              // Add invalid value as it is in case of parsing errors
              cronField?.expression?.asString() ?: fieldValue
            }
          }
          .joinToString(separator = " ")

      cronExpression.set(newCronExpression)

      // Validate resulting expression
      val cron = parseExpression()
      updateInformation(cron)
    }

    fun updateInformation(cron: Cron?) {
      if (cron == null) {
        cronExplanation.set(UiToolsBundle.message("cron-expression.unknown"))
        cronLastExecution.set(UiToolsBundle.message("cron-expression.unknown"))
        cronNextExecution.set(UiToolsBundle.message("cron-expression.unknown"))
        return
      }

      // The cronutils library has already translations for various languages
      val ideaLocal = DynamicBundle.getLocale()
      val description = CronDescriptor.instance(ideaLocal).describe(cron)
      cronExplanation.set(description.capitalize())

      val executionTime = ExecutionTime.forCron(cron)
      val now = ZonedDateTime.now()
      cronLastExecution.set(
        executionTime
          .lastExecution(now)
          .map { DateFormatUtil.formatDateTime(it.toEpochSecond() * 1000) }
          .orElse(UiToolsBundle.message("cron-expression.unknown"))
      )
      cronNextExecution.set(
        executionTime
          .nextExecution(now)
          .map { DateFormatUtil.formatDateTime(it.toEpochSecond() * 1000) }
          .orElse(UiToolsBundle.message("cron-expression.unknown"))
      )
    }

    private fun CronType.defaultCronExpression(): String =
      when (this) {
        CronType.CRON4J,
        CronType.UNIX -> "* * * * *"
        CronType.QUARTZ -> "* * * * * ? *"
        CronType.SPRING,
        CronType.SPRING53 -> "* * * * * *"
      }

    private fun CronType.exampleCronExpression(): String =
      when (this) {
        CronType.CRON4J,
        CronType.UNIX -> "0 0 * * 1-5" // Every hour on weekdays
        CronType.QUARTZ -> "0 0 12 ? * MON-FRI" // 12 PM on weekdays
        CronType.SPRING,
        CronType.SPRING53 -> "0 0 12 * * MON-FRI" // 12 PM on weekdays
      }

    private fun parseExpression(): Cron? =
      try {
        cronExpressionErrorHolder.clear()
        cronParser.parse(cronExpression.get()).validate()
      } catch (e: Exception) {
        cronExpressionErrorHolder.add(e)
        null
      }
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<CronExpression> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("cron-expression.menu-title"),
        contentTitle = UiToolsBundle.message("cron-expression.content-title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> CronExpression) = { configuration ->
      CronExpression(configuration, parentDisposable)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val cronFieldNames: Map<CronType, List<String>> =
      mapOf(
        CronType.QUARTZ to
          listOf("second", "minute", "hour", "dayOfMonth", "month", "dayOfWeek", "year"),
        CronType.SPRING to listOf("second", "minute", "hour", "dayOfMonth", "month", "dayOfWeek"),
        CronType.SPRING53 to listOf("second", "minute", "hour", "dayOfMonth", "month", "dayOfWeek"),
        CronType.UNIX to listOf("minute", "hour", "dayOfMonth", "month", "dayOfWeek"),
        CronType.CRON4J to listOf("minute", "hour", "dayOfMonth", "month", "dayOfWeek"),
      )
  }
}
