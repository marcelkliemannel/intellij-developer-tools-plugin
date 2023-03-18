@file:Suppress("UnstableApiUsage")

package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.NaturalComparator
import com.intellij.ui.dsl.builder.*
import com.intellij.ui.layout.ComboBoxPredicate
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.makeCaseInsensitive
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.transformer.TextSortingTransformer.WordsDelimiter.*
import io.ktor.util.*

internal class TextSortingTransformer(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  TextTransformer(
    presentation = DeveloperToolContext(
      menuTitle = "Text Sorting",
      contentTitle = "Text Sorting"
    ),
    context = Context(
      transformActionTitle = "Sort",
      sourceTitle = "Unsorted",
      resultTitle = "Sorted"
    ),
    configuration = configuration,
    parentDisposable = parentDisposable
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
    var unsortedWords = sourceText.split(unsortedSplitWordsDelimiterPattern)

    if (trimWords.get()) {
      unsortedWords = unsortedWords.map { it.trim() }
    }
    if (removeDuplicates.get()) {
      unsortedWords = unsortedWords.distinct()
    }
    if (removeBlankWords.get()) {
      unsortedWords = unsortedWords.filter { it.isNotBlank() }
    }
    if (reverseOrder.get()) {
      unsortedWords = unsortedWords.reversed()
    }

    var comparator = sortingOrder.get().comparator
    if (caseInsensitive.get()) {
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
      comboBox(SortingOrder.values().toList())
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
    val splitWordsDelimiterComboBox = comboBox(WordsDelimiter.values().toList())
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