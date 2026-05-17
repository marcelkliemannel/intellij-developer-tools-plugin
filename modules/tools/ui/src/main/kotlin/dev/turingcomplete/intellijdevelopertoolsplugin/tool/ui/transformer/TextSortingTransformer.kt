package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer

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
import dev.turingcomplete.intellijdevelopertoolsplugin.common.makeCaseInsensitive
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.ConversionSideHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base.UndirectionalConverter
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.TextSortingTransformer.WordsDelimiter.INDIVIDUAL
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.transformer.TextSortingTransformer.WordsDelimiter.LINE_BREAK

class TextSortingTransformer(
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
    title = UiToolsBundle.message("text-sorting-transformer.title"),
    sourceTitle = UiToolsBundle.message("text-sorting-transformer.source-title"),
    targetTitle = UiToolsBundle.message("text-sorting-transformer.target-title"),
    toTargetTitle = UiToolsBundle.message("text-sorting-transformer.sort"),
  ) {
  // -- Properties ---------------------------------------------------------- //

  private var unsortedSplitWordsDelimiter =
    configuration.register("unsortedPredefinedDelimiter", LINE_BREAK)
  private var unsortedIndividualSplitWordsDelimiter =
    configuration.register("unsortedIndividualSplitWordsDelimiter", " ")

  private var sortedJoinWordsDelimiter =
    configuration.register("sortedJoinWordsDelimiter", LINE_BREAK)
  private var sortedIndividualJoinWordsDelimiter =
    configuration.register("sortedIndividualJoinWordsDelimiter", " ")

  private var sortingOrder = configuration.register("sortingOrder", SortingOrder.LEXICOGRAPHIC)

  private var removeDuplicates = configuration.register("removeDuplicates", true)
  private var removeBlankWords = configuration.register("removeBlankWords", true)
  private var trimWords = configuration.register("trimWords", true)
  private var caseInsensitive = configuration.register("caseInsensitive", false)
  private var reverseOrder = configuration.register("reverseOrder", false)

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun ConversionSideHandler.addSourceTextInputOutputHandler() {
    addTextInputOutputHandler(
      id = defaultSourceInputOutputHandlerId,
      exampleText = EXAMPLE_SOURCE_TEXT,
    )
  }

  override fun doConvertToTarget(source: ByteArray): ByteArray {
    val unsortedSplitWordsDelimiterPattern: Regex =
      unsortedSplitWordsDelimiter.get().splitPattern
        ?: Regex("${Regex.escape(unsortedIndividualSplitWordsDelimiter.get())}+")
    val sortedJoinWordsDelimiter =
      sortedJoinWordsDelimiter.get().joinDelimiter ?: sortedIndividualJoinWordsDelimiter.get()
    var unsortedWords = String(source).split(unsortedSplitWordsDelimiterPattern)

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

    return unsortedWords.joinToString(sortedJoinWordsDelimiter).encodeToByteArray()
  }

  override fun Panel.buildSourceBottomConfigurationUi() {
    row {
      buildSplitConfigurationUi(
        UiToolsBundle.message("text-sorting-transformer.split-unsorted-words"),
        unsortedSplitWordsDelimiter,
        unsortedIndividualSplitWordsDelimiter,
      )
    }

    row {
      buildSplitConfigurationUi(
        UiToolsBundle.message("text-sorting-transformer.join-sorted-words"),
        sortedJoinWordsDelimiter,
        sortedIndividualJoinWordsDelimiter,
      )
    }

    row {
      comboBox(SortingOrder.entries)
        .label(UiToolsBundle.message("text-sorting-transformer.order"))
        .bindItem(sortingOrder)
      checkBox(UiToolsBundle.message("text-sorting-transformer.reverse")).bindSelected(reverseOrder)
      checkBox(UiToolsBundle.message("text-sorting-transformer.case-insensitive"))
        .bindSelected(caseInsensitive)
    }

    row {
      checkBox(UiToolsBundle.message("text-sorting-transformer.remove-duplicates"))
        .bindSelected(removeDuplicates)
      checkBox(UiToolsBundle.message("text-sorting-transformer.trim-words")).bindSelected(trimWords)
      checkBox(UiToolsBundle.message("text-sorting-transformer.remove-blank-words"))
        .bindSelected(removeBlankWords)
    }
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun Row.buildSplitConfigurationUi(
    title: String,
    splitWordsDelimiter: ObservableMutableProperty<WordsDelimiter>,
    individualDelimiter: ObservableMutableProperty<String>,
  ) {
    val splitWordsDelimiterComboBox =
      comboBox(WordsDelimiter.entries).label(title).bindItem(splitWordsDelimiter).component
    textField()
      .bindText(individualDelimiter)
      .visibleIf(ComboBoxPredicate(splitWordsDelimiterComboBox) { it == INDIVIDUAL })
  }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class SortingOrder(private val title: String, val comparator: Comparator<String>) {

    NATURAL(
      UiToolsBundle.message("text-sorting-transformer.sorting-order.natural"),
      NaturalComparator(),
    ),
    LEXICOGRAPHIC(
      UiToolsBundle.message("text-sorting-transformer.sorting-order.lexicographic"),
      { a, b -> a.compareTo(b) },
    ),
    WORD_LENGTH(
      UiToolsBundle.message("text-sorting-transformer.sorting-order.word-length"),
      Comparator { a, b -> a.length - b.length },
    );

    override fun toString(): String = title
  }

  // -- Inner Type ---------------------------------------------------------- //

  private enum class WordsDelimiter(
    private val title: String,
    val splitPattern: Regex?,
    val joinDelimiter: String?,
  ) {

    LINE_BREAK(
      UiToolsBundle.message("text-sorting-transformer.words-delimiter.line-break"),
      Regex("\\R+"),
      System.lineSeparator(),
    ),
    SPACE(
      UiToolsBundle.message("text-sorting-transformer.words-delimiter.whitespace"),
      Regex("\\s+"),
      " ",
    ),
    COMMA(
      UiToolsBundle.message("text-sorting-transformer.words-delimiter.comma"),
      Regex(",+"),
      ",",
    ),
    SEMICOLON(
      UiToolsBundle.message("text-sorting-transformer.words-delimiter.semicolon"),
      Regex(";+"),
      ";",
    ),
    DASH(UiToolsBundle.message("text-sorting-transformer.words-delimiter.dash"), Regex("-+"), "-"),
    UNDERSCORE(
      UiToolsBundle.message("text-sorting-transformer.words-delimiter.underscore"),
      Regex("_+"),
      "_",
    ),
    INDIVIDUAL(
      UiToolsBundle.message("text-sorting-transformer.words-delimiter.individual"),
      null,
      null,
    );

    override fun toString(): String = title
  }

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<TextSortingTransformer> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("text-sorting-transformer.menu-title"),
        contentTitle = UiToolsBundle.message("text-sorting-transformer.content-title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> TextSortingTransformer) = { configuration ->
      TextSortingTransformer(context, configuration, parentDisposable, project)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val EXAMPLE_SOURCE_TEXT = "b\nc\na"
  }
}
