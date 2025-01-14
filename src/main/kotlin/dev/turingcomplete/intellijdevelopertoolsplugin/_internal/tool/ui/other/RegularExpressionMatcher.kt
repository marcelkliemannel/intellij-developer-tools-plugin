package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.other

import com.google.code.regexp.Pattern
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.editor.colors.EditorColors.SEARCH_RESULT_ATTRIBUTES
import com.intellij.openapi.editor.colors.EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.observable.util.whenFocusLost
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Splitter
import com.intellij.openapi.ui.setEmptyState
import com.intellij.openapi.util.TextRange
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES
import com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES
import com.intellij.ui.SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.JBViewport
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.CopyValuesAction
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.PluginCommonDataKeys
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.regex.RegexTextField
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.regex.SelectRegexOptionsAction
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.setContextMenu
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.message.UiToolsBundle
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.other.RegularExpressionMatcher.MatchResultType.MATCH
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.other.RegularExpressionMatcher.MatchResultType.NAMED_GROUP
import dev.turingcomplete.intellijdevelopertoolsplugin.common.getOrNull
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolPresentation
import java.awt.Dimension
import javax.swing.BorderFactory
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionListener
import javax.swing.table.AbstractTableModel

class RegularExpressionMatcher(
  private val context: DeveloperUiToolContext,
  private val configuration: DeveloperToolConfiguration,
  private val project: Project?,
  parentDisposable: Disposable
) : DeveloperUiTool(parentDisposable) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var selectedRegexOptionFlag = configuration.register("regexOption", 0)

  private val regexPattern = configuration.register("regexText", "", INPUT, EXAMPLE_REGEX)
  private val inputText = configuration.register("inputText", "", INPUT, EXAMPLE_INPUT_TEXT)
  private val substitutionPattern = configuration.register("substitutionPattern", "", INPUT, EXAMPLE_SUBSTITUTION_PATTERN)
  private val extractionPattern = configuration.register("extractionPattern", "", INPUT, EXAMPLE_EXTRACTION_PATTERN)

  private val substitutionResult = ValueProperty("")
  private val extractionResult = ValueProperty("")
  private lateinit var inputEditor: DeveloperToolEditor

  private val regexMatchingAttributes by lazy { EditorColorsManager.getInstance().globalScheme.getAttributes(SEARCH_RESULT_ATTRIBUTES) }
  private val selectedMatchResultHighlightingAttributes by lazy { EditorColorsManager.getInstance().globalScheme.getAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES) }
  private lateinit var matchResultsTableModel: MatchResultsTableModel

  private val regexInputErrorHolder = ErrorHolder()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildUi() {
    row {
      cell(
        Splitter(true, 0.7f).apply {
          firstComponent = createInputComponent()
          secondComponent = createResultComponent()
        }
      ).align(Align.FILL).resizableColumn()
    }.resizableRow()
  }

  override fun afterBuildUi() {
    sync()
  }

  override fun reset() {
    sync()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun createInputComponent() = panel {
    row {
      val regexTextField = RegexTextField(project, parentDisposable, regexPattern)
        .onTextChangeFromUi { sync() }
      cell(regexTextField)
        .label(UiToolsBundle.message("regular-expression-matcher.regex-input"), LabelPosition.TOP)
        .validationOnApply(regexInputErrorHolder.asValidation())
        .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
        .align(Align.FILL)
        .resizableColumn()
        .gap(RightGap.SMALL)
      cell(SelectRegexOptionsAction.createActionButton(selectedRegexOptionFlag))
    }.topGap(TopGap.NONE)

    row {
      inputEditor = DeveloperToolEditor(
        id = "input",
        context = context,
        configuration = configuration,
        project = project,
        title = UiToolsBundle.message("regular-expression-matcher.text-input-title"),
        editorMode = DeveloperToolEditor.EditorMode.INPUT,
        parentDisposable = parentDisposable,
        textProperty = inputText
      ).onTextChangeFromUi { sync() }
      cell(inputEditor.component).align(Align.FILL)
    }.resizableRow().topGap(TopGap.SMALL)
  }

  private fun createResultComponent() = panel {
    row {
      cell(JBTabbedPane().apply {
        addTab(
          UiToolsBundle.message("regular-expression-matcher.matches-title"),
          createMatchesTableComponent()
        )

        addTab(
          UiToolsBundle.message("regular-expression-matcher.substitution-title"),
          createSubstitutionComponent()
        )

        addTab(
          UiToolsBundle.message("regular-expression-matcher.extraction-title"),
          createExtractionComponent()
        )
      }).resizableColumn().align(Align.FILL)
    }.resizableRow()
  }

  private fun createMatchesTableComponent() = panel {
    row {
      val highlightSelectedMatchResults: (List<TextRange>) -> Unit = { textRanges ->
        inputEditor.removeTextRangeHighlighters(SELECTED_MATCH_RESULT_HIGHLIGHTING_GROUP_ID)
        textRanges.forEach { textRange ->
          inputEditor.highlightTextRange(
            textRange,
            REGEX_MATCH_SELECTED_HIGHLIGHT_LAYER,
            selectedMatchResultHighlightingAttributes,
            SELECTED_MATCH_RESULT_HIGHLIGHTING_GROUP_ID
          )
        }
      }
      matchResultsTableModel = MatchResultsTableModel()
      val matchResultsTable = MatchResultsTable(matchResultsTableModel, highlightSelectedMatchResults).apply {
        whenFocusLost(parentDisposable) {
          inputEditor.removeTextRangeHighlighters(SELECTED_MATCH_RESULT_HIGHLIGHTING_GROUP_ID)
        }
      }
      cell(
        ScrollPaneFactory.createScrollPane(matchResultsTable).apply {
          minimumSize = Dimension(minimumSize.width, 150)
          preferredSize = Dimension(preferredSize.width, 150)
        }
      ).align(Align.FILL)
    }.resizableRow()
  }

  @Suppress("UnstableApiUsage")
  private fun createSubstitutionComponent() = panel {
    row {
      expandableTextField()
        .bindText(substitutionPattern)
        .align(Align.FILL)
        .resizableColumn()
        .whenTextChangedFromUi { substitute() }
        .gap(RightGap.SMALL)
      contextHelp(UiToolsBundle.message("regular-expression-matcher.replace-pattern-context-help"))
    }

    row {
      cell(
        DeveloperToolEditor(
          id = "substitution-result",
          context = context,
          configuration = configuration,
          project = project,
          editorMode = DeveloperToolEditor.EditorMode.OUTPUT,
          parentDisposable = parentDisposable,
          textProperty = substitutionResult
        ).component
      ).align(Align.FILL)
    }.resizableRow().topGap(TopGap.SMALL)
  }

  @Suppress("UnstableApiUsage")
  private fun createExtractionComponent() = panel {
    row {
      expandableTextField()
        .bindText(extractionPattern)
        .align(Align.FILL)
        .resizableColumn()
        .whenTextChangedFromUi { extract() }
        .gap(RightGap.SMALL)
      contextHelp(UiToolsBundle.message("regular-expression-matcher.replace-pattern-context-help"))
    }

    row {
      cell(
        DeveloperToolEditor(
          id = "extraction-result",
          context = context,
          configuration = configuration,
          project = project,
          editorMode = DeveloperToolEditor.EditorMode.OUTPUT,
          parentDisposable = parentDisposable,
          textProperty = extractionResult
        ).component
      ).align(Align.FILL)
    }.resizableRow().topGap(TopGap.SMALL)
  }

  private fun sync() {
    match()
    substitute()
    extract()
  }

  /**
   * Kotlin and Java do not support the retrieval of all named groups yet:
   * [KT-51671](https://youtrack.jetbrains.com/issue/KT-51671). Therefore, we
   * are using Google's [com.google.code.regexp.Pattern] for now.
   */
  private fun match() {
    inputEditor.removeAllTextRangeHighlighters()
    regexInputErrorHolder.clear()
    matchResultsTableModel.setMatches(emptyList())

    val regex = regexPattern.get()
    if (regex.isEmpty()) {
      return
    }

    try {
      val pattern = Pattern.compile(regex, selectedRegexOptionFlag.get())
      val matcher = pattern.matcher(inputEditor.text)

      val namedGroups = matcher.namedGroupsList()
      val results = mutableListOf<Match>()
      var i = 0
      while (matcher.find()) {
        val textRange = TextRange(matcher.start(), matcher.end())
        inputEditor.highlightTextRange(textRange, REGEX_MATCH_HIGHLIGHT_LAYER, regexMatchingAttributes)
        results.add(Match(i, "${i + 1}", textRange, matcher.group(), MATCH))
        if (namedGroups.isNotEmpty()) {
          namedGroups[i].filter { it.value != null }.forEach {
            results.add(Match(i, it.key, TextRange(matcher.start(it.key), matcher.end(it.key)), it.value, NAMED_GROUP))
          }
        }
        i++
      }

      matchResultsTableModel.setMatches(results)
    } catch (e: Exception) {
      regexInputErrorHolder.add(e)
    }

    // The `validate` in this class is not used as a validation mechanism. We
    // make use of its text field error UI to display the `regexInputErrorHolder`.
    validate()
  }

  private fun substitute() {
    val regex = regexPattern.get()
    if (regex.isEmpty()) {
      return
    }

    try {
      val result = Regex(regex).replace(inputText.get()) { matchResult ->
        substitutionPattern.get().replace(Regex("""\$(\d+)|\$\{(\d+)}""")) { groupMatch ->
          val groupIndex = groupMatch.groups[1]?.value?.toInt()
            ?: groupMatch.groups[2]?.value?.toInt()
            ?: -1
          matchResult.groups.getOrNull(groupIndex)?.value ?: "\$$groupIndex"
        }
      }
      substitutionResult.set(result)
    }
    catch (_: Exception) {
      // An invalid pattern will be handled by `match`.
    }
  }

  private fun extract() {
    val regex = regexPattern.get()
    if (regex.isEmpty()) {
      return
    }

    try {
      val result = Regex(regex).findAll(inputText.get()).map { matchResult ->
        extractionPattern.get().replace(Regex("""\$(\d+)|\$\{(\d+)}""")) { groupMatch ->
          val groupIndex = groupMatch.groups[1]?.value?.toInt()
            ?: groupMatch.groups[2]?.value?.toInt()
            ?: -1
          matchResult.groups.getOrNull(groupIndex)?.value ?: "\$$groupIndex"
        }
      }.joinToString(separator = "")
      extractionResult.set(result)
    }
    catch (_: Exception) {
      // An invalid pattern will be handled by `match`.
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class MatchResultsTable(
    private val model: MatchResultsTableModel,
    private val selectedMatchResultHighlight: (List<TextRange>) -> Unit
  ) : JBTable(model), DataProvider {

    init {
      columnModel.apply {
        getColumn(0).preferredWidth = 150
        getColumn(1).preferredWidth = 300
      }
      visibleRowCount = 4
      putClientProperty(JBViewport.FORCE_VISIBLE_ROW_COUNT_KEY, true)
      autoResizeMode = AUTO_RESIZE_LAST_COLUMN
      setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
      rowSelectionAllowed = true
      columnSelectionAllowed = false
      setDefaultRenderer(Object::class.java, MatchResultsTableCellRenderer(model))
      selectionModel.addListSelectionListener(createSelectionListener())
      setContextMenu(this::class.java.name, DefaultActionGroup(CopyValuesAction()))
      setEmptyState(UiToolsBundle.message("regular-expression-matcher.matches-no-matches"))
      TableSpeedSearch.installOn(this) { value, cell ->
        if (cell.column == 0 || cell.column == 1) value as String else null
      }
    }

    override fun getData(dataId: String): Any? = when {
      PluginCommonDataKeys.SELECTED_VALUES.`is`(dataId) -> selectedRows.map { model.getValueAt(it, 1) as String }.toList()
      else -> null
    }

    @Suppress("UNCHECKED_CAST")
    private fun createSelectionListener() = ListSelectionListener { e ->
      if (!e.valueIsAdjusting) {
        val selectedTextRanges = this@MatchResultsTable.selectedRows.map { (model.getValueAt(it, 0) as Pair<String, TextRange>).second }.toList()
        selectedMatchResultHighlight(selectedTextRanges)
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private data class Match(
    val groupIndex: Int,
    val title: String,
    val textRange: TextRange,
    val value: String,
    val matchResultType: MatchResultType
  )

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class MatchResultsTableModel : AbstractTableModel() {

    private var matches = listOf<Match>()

    fun setMatches(matches: List<Match>) {
      this.matches = matches
      fireTableDataChanged()
    }

    override fun getRowCount(): Int = matches.size

    override fun getColumnCount(): Int = 2

    override fun getColumnName(column: Int): String = when (column) {
      0 -> UiToolsBundle.message("regular-expression-matcher.matches-group")
      1 -> UiToolsBundle.message("regular-expression-matcher.matches-value")
      else -> error("Unknown column: $column")
    }

    override fun getValueAt(row: Int, column: Int): Any = when(column) {
      0, 1 -> matches[row]
      else -> error("Unknown column: $column")
    }

    fun getRowMatchResultType(row: Int): MatchResultType = matches[row].matchResultType
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class MatchResultsTableCellRenderer(private val model: MatchResultsTableModel) : ColoredTableCellRenderer() {

    @Suppress("UNCHECKED_CAST")
    override fun customizeCellRenderer(table: JTable, match: Any?, selected: Boolean, hasFocus: Boolean, row: Int, column: Int) {
      check(match is Match)

      val matchResultType = model.getRowMatchResultType(row)

      when (column) {
        0 -> {
          val prefix = when (matchResultType) {
            MATCH -> UiToolsBundle.message("regular-expression-matcher.matches-match-prefix")
            NAMED_GROUP -> UiToolsBundle.message("regular-expression-matcher.matches-group-prefix")
          }
          append("$prefix ", REGULAR_ATTRIBUTES)
          append("${match.title} ", REGULAR_BOLD_ATTRIBUTES)
          append("(${match.textRange.startOffset} to ${match.textRange.endOffset})", GRAY_SMALL_ATTRIBUTES)

        }
        1 -> append(match.value)
        else -> error("Unknown column: $column")
      }

      border = if (row > 0 && matchResultType == MATCH) {
        BorderFactory.createCompoundBorder(matchResultAfterFirstMatchBorder, border)
      }
      else if (column == 0 && matchResultType == NAMED_GROUP) {
        BorderFactory.createCompoundBorder(matchResultGroupBorder, border)
      }
      else {
        border
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class MatchResultType {

    MATCH,
    NAMED_GROUP
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<RegularExpressionMatcher> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = UiToolsBundle.message("regular-expression-matcher.menu-title"),
      contentTitle = UiToolsBundle.message("regular-expression-matcher.content-title")
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> RegularExpressionMatcher) =
      { configuration -> RegularExpressionMatcher(context, configuration, project, parentDisposable) }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val EXAMPLE_REGEX = "mid(?<postfix>[a-zA-Z]+)"
    private const val EXAMPLE_INPUT_TEXT = "aurora midsummer midnight earth"
    private const val EXAMPLE_SUBSTITUTION_PATTERN = "deep \$1"
    private const val EXAMPLE_EXTRACTION_PATTERN = "\${1}s "

    private const val REGEX_MATCH_HIGHLIGHT_LAYER = HighlighterLayer.SELECTION - 2
    private const val REGEX_MATCH_SELECTED_HIGHLIGHT_LAYER = HighlighterLayer.SELECTION - 1

    private const val SELECTED_MATCH_RESULT_HIGHLIGHTING_GROUP_ID = "matchResultHighlighting"

    private val matchResultAfterFirstMatchBorder = JBUI.Borders.customLineTop(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground())
    private val matchResultGroupBorder = JBUI.Borders.emptyLeft(5)
  }
}