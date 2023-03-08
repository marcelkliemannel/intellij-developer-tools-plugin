@file:Suppress("UnstableApiUsage")

package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.NaturalComparator
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ComboBoxPredicate
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.makeCaseInsensitive
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer.TextSortingTransformer.WordsDelimiter.*
import io.ktor.util.*

internal class TextSortingTransformer(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextTransformer(
    presentation = DeveloperToolPresentation("Text Sorting", "Text Sorting"),
    transformActionTitle = "Sort",
    sourceTitle = "Unsorted",
    resultTitle = "Sorted",
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var unsortedSplitWordsDelimiter: WordsDelimiter by configuration.register("unsortedPredefinedDelimiter", LINE_BREAK)
  private var unsortedIndividualSplitWordsDelimiter: String by configuration.register("unsortedIndividualSplitWordsDelimiter", " ")

  private var sortedJoinWordsDelimiter: WordsDelimiter by configuration.register("sortedJoinWordsDelimiter", LINE_BREAK)
  private var sortedIndividualJoinWordsDelimiter: String by configuration.register("sortedIndividualJoinWordsDelimiter", " ")

  private var sortingOrder: SortingOrder by configuration.register("sortingOrder", SortingOrder.LEXICOGRAPHIC)

  private var removeDuplicates: Boolean by configuration.register("removeDuplicates", true)
  private var removeBlankWords: Boolean by configuration.register("removeBlankWords", true)
  private var trimWords: Boolean by configuration.register("trimWords", true)
  private var caseInsensitive: Boolean by configuration.register("caseInsensitive", false)
  private var reverseOrder: Boolean by configuration.register("reverseOrder", false)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun doTransform() {
    val unsortedSplitWordsDelimiterPattern: Regex = when (unsortedSplitWordsDelimiter) {
      LINE_BREAK, SPACE, DASH, UNDERSCORE, COMMA, SEMICOLON -> unsortedSplitWordsDelimiter.splitPattern!!
      INDIVIDUAL -> Regex("${Regex.escape(unsortedIndividualSplitWordsDelimiter)}+")
    }
    val sortedJoinWordsDelimiter: String = when (sortedJoinWordsDelimiter) {
      LINE_BREAK, SPACE, DASH, UNDERSCORE, COMMA, SEMICOLON -> sortedJoinWordsDelimiter.joinDelimiter!!
      INDIVIDUAL -> sortedIndividualJoinWordsDelimiter
    }

    var unsortedWords = sourceText.split(unsortedSplitWordsDelimiterPattern)

    if (trimWords) {
      unsortedWords = unsortedWords.map { it.trim() }
    }
    if (removeDuplicates) {
      unsortedWords = unsortedWords.distinct()
    }
    if (removeBlankWords) {
      unsortedWords = unsortedWords.filter { it.isNotBlank() }
    }
    if (reverseOrder) {
      unsortedWords = unsortedWords.reversed()
    }

    var comparator = sortingOrder.comparator
    if (caseInsensitive) {
      comparator = comparator.makeCaseInsensitive()
    }
    unsortedWords = unsortedWords.sortedWith(comparator)

    resultText = unsortedWords.joinToString(sortedJoinWordsDelimiter)
  }

  override fun Panel.buildMiddleConfigurationUi() {
    row {
      buildSplitConfigurationUi(
              "Split unsorted words by:",
              unsortedSplitWordsDelimiter,
              unsortedIndividualSplitWordsDelimiter,
              { unsortedSplitWordsDelimiter = it },
              { unsortedIndividualSplitWordsDelimiter = it }
      )
    }

    row {
      buildSplitConfigurationUi(
              "Join sorted words by:",
              sortedJoinWordsDelimiter,
              sortedIndividualJoinWordsDelimiter,
              { sortedJoinWordsDelimiter = it },
              { sortedIndividualJoinWordsDelimiter = it }
      )
    }

    row {
      comboBox(SortingOrder.values().toList())
              .label("Order:")
              .applyToComponent { selectedItem = sortingOrder }
              .whenItemSelectedFromUi { sortingOrder = it }
      checkBox("Reverse")
              .applyToComponent { isSelected = reverseOrder }
              .whenStateChangedFromUi { reverseOrder = it }
      checkBox("Case insensitive")
              .applyToComponent { isSelected = caseInsensitive }
              .whenStateChangedFromUi { caseInsensitive = it }
    }

    row {
      checkBox("Remove duplicates")
              .applyToComponent { isSelected = removeDuplicates }
              .whenStateChangedFromUi { removeDuplicates = it }
      checkBox("Trim words")
              .applyToComponent { isSelected = trimWords }
              .whenStateChangedFromUi { trimWords = it }
      checkBox("Remove blank words")
              .applyToComponent { isSelected = removeBlankWords }
              .whenStateChangedFromUi { removeBlankWords = it }
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun Row.buildSplitConfigurationUi(
          title: String,
          initialSplitWordsDelimiter: WordsDelimiter,
          initialIndividualDelimiter: String,
          setSplitWordsDelimiter: (WordsDelimiter) -> Unit,
          setIndividualDelimiter: (String) -> Unit
  ) {
    val splitWordsDelimiterComboBox = comboBox(WordsDelimiter.values().toList())
            .label(title)
            .applyToComponent { selectedItem = initialSplitWordsDelimiter }
            .whenItemSelectedFromUi { setSplitWordsDelimiter(it) }
            .component
    textField().text(initialIndividualDelimiter)
            .whenTextChangedFromUi { setIndividualDelimiter(it) }
            .visibleIf(ComboBoxPredicate(splitWordsDelimiterComboBox) { it == INDIVIDUAL })
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class SortingOrder(private val title: String, val comparator: Comparator<String>) {
    NATURAL("Natural", NaturalComparator()),
    LEXICOGRAPHIC("Lexicographic", { a, b -> a.compareTo(b) }),
    WORD_LENGTH("Word length", Comparator<String> { a, b -> a.length - b.length });

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

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable): DeveloperTool {
      return TextSortingTransformer(configuration, parentDisposable)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}