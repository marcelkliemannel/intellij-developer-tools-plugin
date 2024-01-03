package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.NaturalComparator
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.layout.ComboBoxPredicate
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.makeCaseInsensitive
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.transformer.TextSortingTransformer.WordsDelimiter.INDIVIDUAL
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.transformer.TextSortingTransformer.WordsDelimiter.LINE_BREAK

internal class TextSortingTransformer(
  context: DeveloperUiToolContext,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  project: Project?
) : TextTransformer(
  textTransformerContext = TextTransformerContext(
    transformActionTitle = "Sort",
    sourceTitle = "Unsorted",
    resultTitle = "Sorted",
    initialSourceExampleText = EXAMPLE_INPUT,
    diffSupport = DiffSupport(
      title = "Text Sorting"
    )
  ),
  context = context,
  configuration = configuration,
  parentDisposable = parentDisposable,
  project = project
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var unsortedSplitWordsDelimiter = configuration.register("unsortedPredefinedDelimiter", LINE_BREAK)
  private var unsortedIndividualSplitWordsDelimiter = configuration.register("unsortedIndividualSplitWordsDelimiter", " ")

  private var sortedJoinWordsDelimiter = configuration.register("sortedJoinWordsDelimiter", LINE_BREAK)
  private var sortedIndividualJoinWordsDelimiter = configuration.register("sortedIndividualJoinWordsDelimiter", " ")

  private var sortingOrder = configuration.register("sortingOrder", SortingOrder.LEXICOGRAPHIC)

  private var removeDuplicates = configuration.register("removeDuplicates", true)
  private var removeBlankWords = configuration.register("removeBlankWords", true)
  private var trimWords = configuration.register("trimWords", true)
  private var caseInsensitive = configuration.register("caseInsensitive", false)
  private var reverseOrder = configuration.register("reverseOrder", false)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun transform() {
    val unsortedSplitWordsDelimiterPattern: Regex = unsortedSplitWordsDelimiter.get().splitPattern
      ?: Regex("${Regex.escape(unsortedIndividualSplitWordsDelimiter.get())}+")
    val sortedJoinWordsDelimiter = sortedJoinWordsDelimiter.get().joinDelimiter
      ?: sortedIndividualJoinWordsDelimiter.get()
    var unsortedWords = sourceText.get().split(unsortedSplitWordsDelimiterPattern)

    if (trimWords.get()) {
      unsortedWords = unsortedWords.map { it.trim() }
    }
    if (removeDuplicates.get()) {
      unsortedWords = unsortedWords.distinct()
    }
    if (removeBlankWords.get()) {
      unsortedWords = unsortedWords.filter { it.isNotBlank() }
    }

    var comparator = sortingOrder.get().comparator
    if (caseInsensitive.get()) {
      comparator = comparator.makeCaseInsensitive()
    }
    if (reverseOrder.get()) {
      comparator = comparator.reversed()
    }
    unsortedWords = unsortedWords.sortedWith(comparator)

    resultText.set(unsortedWords.joinToString(sortedJoinWordsDelimiter))
  }

  override fun Panel.buildMiddleConfigurationUi() {
    row {
      buildSplitConfigurationUi(
        "Split unsorted words by:",
        unsortedSplitWordsDelimiter,
        unsortedIndividualSplitWordsDelimiter
      )
    }

    row {
      buildSplitConfigurationUi(
        "Join sorted words by:",
        sortedJoinWordsDelimiter,
        sortedIndividualJoinWordsDelimiter
      )
    }

    row {
      comboBox(SortingOrder.entries)
        .label("Order:")
        .bindItem(sortingOrder)
      checkBox("Reverse")
        .bindSelected(reverseOrder)
      checkBox("Case insensitive")
        .bindSelected(caseInsensitive)
    }

    row {
      checkBox("Remove duplicates")
        .bindSelected(removeDuplicates)
      checkBox("Trim words")
        .bindSelected(trimWords)
      checkBox("Remove blank words")
        .bindSelected(removeBlankWords)
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun Row.buildSplitConfigurationUi(
    title: String,
    splitWordsDelimiter: ObservableMutableProperty<WordsDelimiter>,
    individualDelimiter: ObservableMutableProperty<String>
  ) {
    val splitWordsDelimiterComboBox = comboBox(WordsDelimiter.entries)
      .label(title)
      .bindItem(splitWordsDelimiter)
      .component
    textField()
      .bindText(individualDelimiter)
      .visibleIf(ComboBoxPredicate(splitWordsDelimiterComboBox) { it == INDIVIDUAL })
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class SortingOrder(private val title: String, val comparator: Comparator<String>) {

    NATURAL("Natural", NaturalComparator()),
    LEXICOGRAPHIC("Lexicographic", { a, b -> a.compareTo(b) }),
    WORD_LENGTH("Word length", Comparator { a, b -> a.length - b.length });

    override fun toString(): String = title
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class WordsDelimiter(private val title: String, val splitPattern: Regex?, val joinDelimiter: String?) {

    LINE_BREAK("Line break", Regex("\\R+"), System.lineSeparator()),
    SPACE("Whitespace", Regex("\\s+"), " "),
    COMMA("Comma", Regex(",+"), ","),
    SEMICOLON("Semicolon", Regex(";+"), ";"),
    DASH("Dash", Regex("-+"), "-"),
    UNDERSCORE("Underscore", Regex("_+"), "_"),
    INDIVIDUAL("Individual", null, null);

    override fun toString(): String = title
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<TextSortingTransformer> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "Text Sorting",
      contentTitle = "Text Sorting"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> TextSortingTransformer) = { configuration ->
      TextSortingTransformer(context, configuration, parentDisposable, project)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val EXAMPLE_INPUT = "b\nc\na"
  }
}