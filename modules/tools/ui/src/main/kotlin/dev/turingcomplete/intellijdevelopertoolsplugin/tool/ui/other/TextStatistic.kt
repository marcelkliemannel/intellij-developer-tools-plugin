package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.Alarm
import dev.turingcomplete.intellijdevelopertoolsplugin.common.SimpleTable
import dev.turingcomplete.intellijdevelopertoolsplugin.common.TextStatisticUtils
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor.EditorMode
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.UiUtils.simpleColumnInfo
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolHandler
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.frame.instance.handling.OpenDeveloperToolReference
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other.TextStatistic.OpenTextStatisticContext
import org.apache.commons.text.StringEscapeUtils
import javax.swing.SortOrder


class TextStatistic(
  private val context: DeveloperUiToolContext,
  private val configuration: DeveloperToolConfiguration,
  private val project: Project?,
  parentDisposable: Disposable
) : DeveloperUiTool(parentDisposable), OpenDeveloperToolHandler<OpenTextStatisticContext> {
  // -- Properties ---------------------------------------------------------- //

  private val text = configuration.register("text", "", INPUT, TEXT_EXAMPLE)

  private lateinit var metricsTable: SimpleTable<TextMetric>
  private lateinit var uniqueWordsTable: SimpleTable<Pair<String, Int>>
  private lateinit var uniqueCharactersTable: SimpleTable<Pair<Char, Int>>

  private val charactersCounter = TextMetric("Characters")
  private val wordsCounter = TextMetric("Words")
  private val uniqueWordsCounter = TextMetric("Unique words")
  private val averageWordLengthCounter = TextMetric("Average word length")
  private val sentencesCounter = TextMetric("Sentences")
  private val averageWordsPerSentenceCounter = TextMetric("Average words per sentence")
  private val paragraphsCounter = TextMetric("Paragraphs")
  private val characterCounter = TextMetric("Characters")
  private val uniqueCharactersCounter = TextMetric("Unique characters")
  private val lettersCounter = TextMetric("Letters")
  private val digitsCounter = TextMetric("Digits")
  private val nonAsciiCharactersCounter = TextMetric("Non-ASCII characters")
  private val isoControlCharactersCounter = TextMetric("ISO control characters")
  private val whitespacesCounter = TextMetric("Whitespaces")
  private val lineBreaksCounter = TextMetric("Line breaks")
  private val uniqueCharacters = mutableListOf<Pair<Char, Int>>()
  private val uniqueWords = mutableListOf<Pair<String, Int>>()

  private val counterAlarm by lazy { Alarm(parentDisposable) }

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
      cell(
        Splitter(true, 0.75f).apply {
          firstComponent = createInputEditorComponent()
          secondComponent = createMetricsComponent()
        }
      ).align(Align.FILL).resizableColumn()
    }.resizableRow()
  }

  override fun afterBuildUi() {
    updateCounter()
  }

  override fun applyOpenDeveloperToolContext(context: OpenTextStatisticContext) {
    text.set(context.text)
    updateCounter()
  }

  // -- Private Methods ----------------------------------------------------- //

  private fun createInputEditorComponent() = AdvancedEditor(
    id = "text",
    context = context,
    configuration = configuration,
    project = project,
    title = "Text",
    editorMode = EditorMode.INPUT,
    parentDisposable = parentDisposable,
    textProperty = text,
  ).apply {
    onTextChangeFromUi {
      counterAlarm.addRequest({ updateCounter() }, 100)
    }
  }.component

  private fun createMetricsComponent() = panel {
    row {
      cell(JBTabbedPane().apply {
        metricsTable = createMetricsTable()
        addTab("Metrics", ScrollPaneFactory.createScrollPane(metricsTable, false))

        uniqueWordsTable = createUniqueWordsTable()
        addTab("Unique Words", ScrollPaneFactory.createScrollPane(uniqueWordsTable, false))

        uniqueCharactersTable = createUniqueCharactersTable()
        addTab("Unique Characters", ScrollPaneFactory.createScrollPane(uniqueCharactersTable, false))
      }).resizableColumn().align(Align.FILL)
    }.resizableRow()
  }

  private fun createMetricsTable() = SimpleTable(
    items = listOf(
      charactersCounter,
      wordsCounter,
      uniqueWordsCounter,
      averageWordLengthCounter,
      sentencesCounter,
      averageWordsPerSentenceCounter,
      paragraphsCounter,
      characterCounter,
      uniqueCharactersCounter,
      lettersCounter,
      digitsCounter,
      nonAsciiCharactersCounter,
      isoControlCharactersCounter,
      whitespacesCounter,
      lineBreaksCounter
    ),
    columns = listOf(
      simpleColumnInfo("Metric", { it.title }) { it.title },
      simpleColumnInfo("Occurrence", { it.value }) { it.value }
    ),
    toCopyValue = { "${it.title}: ${it.value}" },
    initialSortedColumn = 0 to SortOrder.UNSORTED
  )

  private fun createUniqueWordsTable(): SimpleTable<Pair<String, Int>> = SimpleTable(
    items = uniqueWords,
    columns = listOf(simpleColumnInfo("Word", { it.first }, { it.first }), simpleColumnInfo("Occurrence", { it.second.toString() }, { it.second })),
    toCopyValue = { it.first },
    initialSortedColumn = 1 to SortOrder.DESCENDING
  )

  private fun createUniqueCharactersTable(): SimpleTable<Pair<Char, Int>> {
    val characterToDisplay: (Pair<Char, Int>) -> String = { (character, _) ->
      if (Character.isISOControl(character)) {
        when (character) {
          '\b' -> "\\b"
          '\t' -> "\\t"
          '\n' -> "\\n"
          '\u000c' -> "\\f"
          '\r' -> "\\r"
          else -> "\\u" + String.format("%04x", character.code)
        }
      }
      else if (character == ' ') {
        "Whitespace"
      }
      else if (character.code > 127) {
        "$character (${StringEscapeUtils.escapeJava(character.toString())})"
      }
      else {
        character.toString()
      }
    }
    return SimpleTable(
      items = uniqueCharacters,
      columns = listOf(
        simpleColumnInfo("Character", characterToDisplay, characterToDisplay),
        simpleColumnInfo("Occurrence", { it.second.toString() }, { it.second })
      ),
      toCopyValue = { it.first.toString() },
      initialSortedColumn = 1 to SortOrder.DESCENDING
    )
  }

  private fun updateCounter() {
    with(TextStatisticUtils.gatherStatistic(text.get())) {
      charactersCounter.value = charactersCount.toString()
      wordsCounter.value = wordsCount.toString()
      uniqueWordsCounter.value = uniqueWords.size.toString()
      averageWordLengthCounter.value = "%.2f".format(averageWordLength)
      sentencesCounter.value = sentencesCount.toString()
      averageWordsPerSentenceCounter.value = "%.2f".format(averageWordsPerSentence)
      paragraphsCounter.value = paragraphsCount.toString()
      characterCounter.value = charactersCount.toString()
      uniqueCharactersCounter.value = uniqueCharacters.size.toString()
      lettersCounter.value = lettersCount.toString()
      digitsCounter.value = digitsCount.toString()
      nonAsciiCharactersCounter.value = nonAsciiCharactersCount.toString()
      isoControlCharactersCounter.value = isoControlCharactersCount.toString()
      whitespacesCounter.value = whitespacesCount.toString()
      lineBreaksCounter.value = lineBreaksCount.toString()
      this@TextStatistic.uniqueWords.clear()
      this@TextStatistic.uniqueWords.addAll(uniqueWords.toList())
      this@TextStatistic.uniqueCharacters.clear()
      this@TextStatistic.uniqueCharacters.addAll(uniqueCharacters.toList())
    }
    metricsTable.reload()
    uniqueWordsTable.reload()
    uniqueCharactersTable.reload()
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class TextMetric(val title: String, var value: String = "Unknown")

  // -- Inner Type ---------------------------------------------------------- //

  data class OpenTextStatisticContext(val text: String) : OpenDeveloperToolContext

  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<TextStatistic> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = "Text Statistic",
        contentTitle = "Text Statistic"
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> TextStatistic) =
      { configuration ->
        TextStatistic(
          context = context,
          configuration = configuration,
          project = project,
          parentDisposable = parentDisposable
        )
      }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val ID = "text-statistic"

    private val TEXT_EXAMPLE = """
Far far away, behind the word mountains, far from the countries Vokalia and Consonantia, there live the blind texts.

Separated they live in Bookmarksgrove right at the coast of the Semantics, a large language ocean.

A small river named Duden flows by their place and supplies it with the necessary regelialia. It is a paradisematic country, in which roasted parts of sentences fly into your mouth.
    """.trimIndent()

    val openTextStatisticReference = OpenDeveloperToolReference.of(ID, OpenTextStatisticContext::class)
  }
}
