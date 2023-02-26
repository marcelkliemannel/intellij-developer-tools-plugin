@file:Suppress("UnstableApiUsage")

package dev.turingcomplete.intellijdevelopertoolsplugins.developertool.transformer

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.NaturalComparator
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.text
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.ui.layout.ComboBoxPredicate
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.common.GeneralDeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.developertool.transformer.TextSortingTransformer.WordsDelimiter.*
import dev.turingcomplete.intellijdevelopertoolsplugins.makeCaseInsensitive
import dev.turingcomplete.intellijdevelopertoolsplugins.onChanged
import dev.turingcomplete.intellijdevelopertoolsplugins.onSelectionChanged
import io.ktor.util.*

class TextSortingTransformer : TextTransformer(
        id = "text-sorting",
        title = "Text Sorting",
        transformActionTitle = "Sort",
        sourceTitle = "Unsorted",
        resultTitle = "Sorted"
), GeneralDeveloperTool {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var unsortedSplitWordsDelimiter: WordsDelimiter by createProperty("unsortedPredefinedDelimiter", SPACE)
  private var unsortedIndividualSplitWordsDelimiter: String by createProperty("unsortedIndividualSplitWordsDelimiter", " ")

  private var sortedJoinWordsDelimiter: WordsDelimiter by createProperty("sortedJoinWordsDelimiter", SPACE)
  private var sortedIndividualJoinWordsDelimiter: String by createProperty("sortedIndividualJoinWordsDelimiter", " ")

  private var sortingOrder: SortingOrder by createProperty("sortingOrder", SortingOrder.LEXICOGRAPHIC)

  private var removeDuplicates: Boolean by createProperty("removeDuplicates", true)
  private var removeBlankWords: Boolean by createProperty("removeBlankWords", true)
  private var trimWords: Boolean by createProperty("trimWords", true)
  private var caseInsensitive: Boolean by createProperty("caseInsensitive", false)
  private var reverseOrder: Boolean by createProperty("reverseOrder", false)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun transform(text: String): String {
    val unsortedSplitWordsDelimiterPattern: Regex = when (unsortedSplitWordsDelimiter) {
      LINE_BREAK, SPACE, DASH, UNDERSCORE, COMMA, SEMICOLON -> unsortedSplitWordsDelimiter.splitPattern!!
      INDIVIDUAL -> Regex("${Regex.escape(unsortedIndividualSplitWordsDelimiter)}+")
    }
    val sortedJoinWordsDelimiter: String = when (sortedJoinWordsDelimiter) {
      LINE_BREAK, SPACE, DASH, UNDERSCORE, COMMA, SEMICOLON -> sortedJoinWordsDelimiter.joinDelimiter!!
      INDIVIDUAL -> sortedIndividualJoinWordsDelimiter
    }

    var unsortedWords = text.split(unsortedSplitWordsDelimiterPattern)

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

    return unsortedWords.joinToString(sortedJoinWordsDelimiter)
  }

  override fun Panel.buildConfigurationUi(project: Project?, parentDisposable: Disposable) {
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
              .applyToComponent {
                selectedItem = sortingOrder
                onChanged { sortingOrder = it }
              }
      checkBox("Reverse").applyToComponent {
        isSelected = reverseOrder
        onSelectionChanged { reverseOrder = it }
      }
      checkBox("Case insensitive").applyToComponent {
        isSelected = caseInsensitive
        onSelectionChanged { caseInsensitive = it }
      }
    }

    row {
      checkBox("Remove duplicates").applyToComponent {
        isSelected = removeDuplicates
        onSelectionChanged { removeDuplicates = it }
      }
      checkBox("Trim words").applyToComponent {
        isSelected = trimWords
        onSelectionChanged { trimWords = it }
      }
      checkBox("Remove blank words").applyToComponent {
        isSelected = removeBlankWords
        onSelectionChanged { removeBlankWords = it }
      }
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun Row.buildSplitConfigurationUi(
          title: String,
          initialSplitWordsDelimiter: WordsDelimiter,
          initialIndividualDelimiter: String,
          onSplitWordsDelimiterChanged: (WordsDelimiter) -> Unit,
          onIndividualDelimiterChanged: (String) -> Unit
  ) {
    val splitWordsDelimiterComboBox = comboBox(WordsDelimiter.values().toList())
            .label(title)
            .applyToComponent {
              selectedItem = initialSplitWordsDelimiter
              onChanged { onSplitWordsDelimiterChanged(it) }
            }.component
    textField().text(initialIndividualDelimiter)
            .whenTextChangedFromUi { onIndividualDelimiterChanged(it) }
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

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}