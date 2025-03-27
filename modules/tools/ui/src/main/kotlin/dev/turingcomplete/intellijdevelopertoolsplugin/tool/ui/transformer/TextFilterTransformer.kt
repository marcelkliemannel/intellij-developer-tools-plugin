package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.selected
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.bind
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.regex.RegexTextField
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.regex.SelectRegexOptionsAction

class TextFilterTransformer(
  context: DeveloperUiToolContext,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  project: Project?,
) :
  TextTransformer(
    textTransformerContext =
      TextTransformerContext(
        transformActionTitle = "Filter",
        sourceTitle = "Unfiltered",
        resultTitle = "Filtered",
        initialSourceExampleText = EXAMPLE_INPUT,
        diffSupport = DiffSupport(title = "Text Filter"),
      ),
    context = context,
    configuration = configuration,
    parentDisposable = parentDisposable,
    project = project,
  ) {
  // -- Properties ---------------------------------------------------------- //

  private val tokenMode = configuration.register("tokenSelectionMode", DEFAULT_TOKEN_SELECTION_MODE)
  private val filteringMode = configuration.register("filteringMode", DEFAULT_FILTERING_MODE)
  private val filteringContainingModeText =
    configuration.register(
      "filteringContainingModeText",
      "",
      INPUT,
      EXAMPLE_FILTERING_CONTAINING_MODE_TEXT,
    )
  private val filteringNotContainingModeText =
    configuration.register(
      "filteringNotContainingModeText",
      "",
      INPUT,
      EXAMPLE_FILTERING_NOT_CONTAINING_MODE_TEXT,
    )
  private val filteringRegexModeText =
    configuration.register("filteringRegexModeText", "", INPUT, EXAMPLE_FILTERING_REGEX_MODE_TEXT)
  private val filteringRegexModeOptions = configuration.register("filteringRegexModeOptions", 0)
  private val filteringRegexModeErrorHolder = ErrorHolder()

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun Panel.buildMiddleConfigurationUi() {
    row { comboBox(TokenMode.entries).label("Filter:").bindItem(tokenMode) }
      .layout(RowLayout.PARENT_GRID)
      .topGap(TopGap.NONE)
      .bottomGap(BottomGap.NONE)

    buttonsGroup {
      row {
          cell()
          val containingFilteringModeRadioButton =
            radioButton("Containing:")
              .bind(filteringMode, FilteringMode.CONTAINING)
              .gap(RightGap.SMALL)
          expandableTextField()
            .bindText(filteringContainingModeText)
            .enabledIf(containingFilteringModeRadioButton.selected)
            .resizableColumn()
            .align(Align.FILL)
        }
        .layout(RowLayout.PARENT_GRID)
        .bottomGap(BottomGap.NONE)

      row {
          cell()
          val containingFilteringModeRadioButton =
            radioButton("Not containing:")
              .bind(filteringMode, FilteringMode.NOT_CONTAINING)
              .gap(RightGap.SMALL)
          expandableTextField()
            .bindText(filteringNotContainingModeText)
            .enabledIf(containingFilteringModeRadioButton.selected)
            .resizableColumn()
            .align(Align.FILL)
        }
        .layout(RowLayout.PARENT_GRID)
        .topGap(TopGap.NONE)
        .bottomGap(BottomGap.NONE)

      row {
          cell()
          radioButton("Matching regular expression:")
            .bind(filteringMode, FilteringMode.REGEX)
            .gap(RightGap.SMALL)
          cell(RegexTextField(project, parentDisposable, filteringRegexModeText))
            .validationOnApply(filteringRegexModeErrorHolder.asValidation())
            .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
            .resizableColumn()
            .align(Align.FILL)
            .gap(RightGap.SMALL)
          cell(SelectRegexOptionsAction.createActionButton(filteringRegexModeOptions))
        }
        .layout(RowLayout.PARENT_GRID)
        .topGap(TopGap.NONE)
    }
  }

  override fun transform() {
    val tokenFilter: (String) -> Boolean =
      when (filteringMode.get()) {
        FilteringMode.CONTAINING -> {
          val filteringContainingModeTextValue = filteringContainingModeText.get()
          ({ it.contains(filteringContainingModeTextValue) })
        }

        FilteringMode.NOT_CONTAINING -> {
          val filteringNotContainingModeTextValue = filteringNotContainingModeText.get()
          ({ !it.contains(filteringNotContainingModeTextValue) })
        }

        FilteringMode.REGEX -> {
          val filteringRegexModeTextValue =
            try {
              Regex(filteringRegexModeText.get())
            } catch (e: Exception) {
              filteringRegexModeErrorHolder.add(e)
              return
            }
          filteringRegexModeTextValue::matches
        }
      }

    val result =
      when (tokenMode.get()) {
        TokenMode.WORD -> {
          with(StringBuilder()) {
            var lastWasWord = false
            for (match in WORDS_SPLIT_REGEX.findAll(sourceText.get())) {
              val token = match.value
              if (token.isBlank() && !token.contains("\n")) {
                if (lastWasWord) {
                  append(token)
                }
              } else if (token.contains("\n")) {
                append(token)
                lastWasWord = false
              } else if (tokenFilter(token)) {
                append(token)
                lastWasWord = true
              } else {
                lastWasWord = false
              }
            }
            toString().trim()
          }
        }

        TokenMode.LINE ->
          sourceText.get().lines().filter(tokenFilter).joinToString(System.lineSeparator())
      }
    resultText.set(result)
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  private enum class TokenMode(val pluralTitle: String) {

    WORD("Words"),
    LINE("Lines");

    override fun toString(): String = pluralTitle
  }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class FilteringMode {

    CONTAINING,
    NOT_CONTAINING,
    REGEX,
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<TextFilterTransformer> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(menuTitle = "Text Filter", contentTitle = "Text Filter")

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> TextFilterTransformer) = { configuration ->
      TextFilterTransformer(context, configuration, parentDisposable, project)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val WORDS_SPLIT_REGEX = Regex("(\\S+|\\s+)")

    private val DEFAULT_TOKEN_SELECTION_MODE = TokenMode.LINE
    private val DEFAULT_FILTERING_MODE = FilteringMode.CONTAINING

    private const val EXAMPLE_FILTERING_CONTAINING_MODE_TEXT = "[error]"
    private const val EXAMPLE_FILTERING_NOT_CONTAINING_MODE_TEXT = "[info]"
    private const val EXAMPLE_FILTERING_REGEX_MODE_TEXT = "^\\[error\\].*$"

    private val EXAMPLE_INPUT =
      """
      [info] Application started
      [error] Error occurred while processing request
    """
        .trimIndent()
  }
}
