package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer

import com.github.vertical_blank.sqlformatter.SqlFormatter
import com.github.vertical_blank.sqlformatter.core.FormatConfig
import com.github.vertical_blank.sqlformatter.languages.Dialect
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.bindIntTextImproved
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.validateLongValue

class SqlFormatterTransformer(
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : TextTransformer(
  developerToolContext = DeveloperToolContext(
    menuTitle = "SQL Formatter",
    contentTitle = "SQL Formatter",
    supportsReset = true
  ),
  textTransformerContext = TextTransformerContext(
    transformActionTitle = "Format",
    sourceTitle = "Plain SQL",
    resultTitle = "Formatted SQL",
    initialSourceText = EXAMPLE_SQL
  ),
  configuration = configuration,
  parentDisposable = parentDisposable
), DeveloperToolConfiguration.ChangeListener {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var dialect = configuration.register("dialect", DEFAULT_DIALECT)
  private var indentSpaces = configuration.register("indentSpaces", DEFAULT_INDENT_SPACES)
  private var uppercase = configuration.register("uppercase", DEFAULT_UPPERCASE)
  private var linesBetweenQueries = configuration.register("linesBetweenQueries", DEFAULT_LINES_BETWEEN_QUERIES)
  private var maxColumnLength = configuration.register("maxColumnLength", DEFAULT_MAX_COLUMN_LENGTH)

  private lateinit var formatConfig: FormatConfig

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildMiddleConfigurationUi() {
    row {
      comboBox(Dialect.values().toList())
        .label("Dialect:")
        .bindItem(dialect)
    }.layout(RowLayout.PARENT_GRID)

    row {
      textField()
        .label("Indent spaces:")
        .bindIntTextImproved(indentSpaces)
        .validateLongValue(LongRange(0, 99))
    }.layout(RowLayout.PARENT_GRID)

    row {
      textField()
        .label("Lines between queries:")
        .bindIntTextImproved(linesBetweenQueries)
        .validateLongValue(LongRange(0, 99))
    }.layout(RowLayout.PARENT_GRID)

    row {
      textField()
        .label("Maximum column length:")
        .bindIntTextImproved(maxColumnLength)
        .validateLongValue(LongRange(0, 99))
    }.layout(RowLayout.PARENT_GRID)

    row {
      checkBox("Convert keywords to uppercase")
        .bindSelected(uppercase)
    }.layout(RowLayout.PARENT_GRID)

    onReset {
      configuration.bulkChange {
        dialect.set(DEFAULT_DIALECT)
        indentSpaces.set(DEFAULT_INDENT_SPACES)
        uppercase.set(DEFAULT_UPPERCASE)
        linesBetweenQueries.set(DEFAULT_LINES_BETWEEN_QUERIES)
        maxColumnLength.set(DEFAULT_MAX_COLUMN_LENGTH)
      }
    }
  }

  override fun afterBuildUi() {
    updateFormatConfig()
    super.afterBuildUi()
  }

  override fun transform() {
    resultText = SqlFormatter.of(dialect.get()).format(sourceText, formatConfig)
  }

  override fun configurationChanged() {
    updateFormatConfig()
    super.configurationChanged()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun updateFormatConfig() {
    formatConfig = FormatConfig.builder()
      .indent(" ".repeat(indentSpaces.get()))
      .uppercase(uppercase.get())
      .linesBetweenQueries(linesBetweenQueries.get())
      .maxColumnLength(maxColumnLength.get())
      .build()
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory<SqlFormatterTransformer> {

    override fun createDeveloperTool(
      configuration: DeveloperToolConfiguration,
      project: Project?,
      parentDisposable: Disposable
    ) = SqlFormatterTransformer(configuration, parentDisposable)
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val EXAMPLE_SQL = "select c.id, c.name, o.address, o.orderedAt from customers c left join orders o ON (o.customerId = c.id) order by o.orderedAt"

    private val DEFAULT_DIALECT = Dialect.N1ql
    private const val DEFAULT_INDENT_SPACES = 2
    private const val DEFAULT_UPPERCASE = true
    private const val DEFAULT_LINES_BETWEEN_QUERIES = 1
    private const val DEFAULT_MAX_COLUMN_LENGTH = 30
  }
}