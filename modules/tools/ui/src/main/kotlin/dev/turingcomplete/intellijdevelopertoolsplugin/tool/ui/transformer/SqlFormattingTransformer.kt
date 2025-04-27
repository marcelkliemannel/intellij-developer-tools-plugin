package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer

import com.github.vertical_blank.sqlformatter.SqlFormatter
import com.github.vertical_blank.sqlformatter.core.FormatConfig
import com.github.vertical_blank.sqlformatter.languages.Dialect
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.bindIntTextImproved
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.validateLongValue
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.ConversionSideHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.UndirectionalConverter

class SqlFormattingTransformer(
  context: DeveloperUiToolContext,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  project: Project?,
) :
  UndirectionalConverter(
    context = context,
    configuration = configuration,
    parentDisposable = parentDisposable,
    project = project,
    title = "SQL Formatting",
    sourceTitle = "Plain SQL",
    targetTitle = "Formatted SQL",
    toTargetTitle = "Format",
  ),
  DeveloperToolConfiguration.ChangeListener {
  // -- Properties ---------------------------------------------------------- //

  private var dialect = configuration.register("dialect", DEFAULT_DIALECT)
  private var indentSpaces = configuration.register("indentSpaces", DEFAULT_INDENT_SPACES)
  private var uppercase = configuration.register("uppercase", DEFAULT_UPPERCASE)
  private var linesBetweenQueries =
    configuration.register("linesBetweenQueries", DEFAULT_LINES_BETWEEN_QUERIES)
  private var maxColumnLength = configuration.register("maxColumnLength", DEFAULT_MAX_COLUMN_LENGTH)

  private lateinit var formatConfig: FormatConfig

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun ConversionSideHandler.addSourceTextInputOutputHandler() {
    addTextInputOutputHandler(
      id = defaultSourceInputOutputHandlerId,
      exampleText = EXAMPLE_SOURCE_TEXT,
    )
  }

  override fun Panel.buildSourceTopConfigurationUi() {
    row { comboBox(Dialect.entries).label("Dialect:").bindItem(dialect) }
      .layout(RowLayout.PARENT_GRID)

    row {
        textField()
          .label("Indent spaces:")
          .bindIntTextImproved(indentSpaces)
          .validateLongValue(LongRange(0, 99))
      }
      .layout(RowLayout.PARENT_GRID)

    row {
        textField()
          .label("Lines between queries:")
          .bindIntTextImproved(linesBetweenQueries)
          .validateLongValue(LongRange(0, 99))
      }
      .layout(RowLayout.PARENT_GRID)

    row {
        textField()
          .label("Maximum column length:")
          .bindIntTextImproved(maxColumnLength)
          .validateLongValue(LongRange(0, 99))
      }
      .layout(RowLayout.PARENT_GRID)

    row { checkBox("Convert keywords to uppercase").bindSelected(uppercase) }
      .layout(RowLayout.PARENT_GRID)
  }

  override fun afterBuildUi() {
    updateFormatConfig()
    super.afterBuildUi()
  }

  override fun doConvertToTarget(source: ByteArray): ByteArray =
    SqlFormatter.of(dialect.get()).format(String(source), formatConfig).toByteArray()

  override fun configurationChanged(property: ValueProperty<out Any>) {
    updateFormatConfig()
    super.configurationChanged(property)
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun updateFormatConfig() {
    formatConfig =
      FormatConfig.builder()
        .indent(" ".repeat(indentSpaces.get()))
        .uppercase(uppercase.get())
        .linesBetweenQueries(linesBetweenQueries.get())
        .maxColumnLength(maxColumnLength.get())
        .build()
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<SqlFormattingTransformer> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(menuTitle = "SQL Formatting", contentTitle = "SQL Formatting")

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> SqlFormattingTransformer) = { configuration ->
      SqlFormattingTransformer(context, configuration, parentDisposable, project)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val EXAMPLE_SOURCE_TEXT =
      "select c.id, c.name, o.address, o.orderedAt from customers c left join orders o ON (o.customerId = c.id) order by o.orderedAt"

    private val DEFAULT_DIALECT = Dialect.N1ql
    private const val DEFAULT_INDENT_SPACES = 2
    private const val DEFAULT_UPPERCASE = true
    private const val DEFAULT_LINES_BETWEEN_QUERIES = 1
    private const val DEFAULT_MAX_COLUMN_LENGTH = 30
  }
}
