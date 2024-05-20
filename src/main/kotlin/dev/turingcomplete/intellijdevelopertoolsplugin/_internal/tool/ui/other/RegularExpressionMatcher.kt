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
import com.intellij.openapi.ui.setEmptyState
import com.intellij.openapi.util.TextRange
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes.GRAY_SMALL_ATTRIBUTES
import com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES
import com.intellij.ui.SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.components.JBViewport
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.CopyValuesAction
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.PluginCommonDataKeys
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.allowUiDslLabel
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.regex.RegexTextField
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.regex.SelectRegexOptionsAction
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.setContextMenu
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.other.RegularExpressionMatcher.MatchResultType.MATCH
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.other.RegularExpressionMatcher.MatchResultType.NAMED_GROUP
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
) : DeveloperUiTool(parentDisposable), DeveloperToolConfiguration.ChangeListener {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var selectedRegexOptionFlag = configuration.register("regexOption", 0)

  private val regexText = configuration.register("regexText", "", INPUT, EXAMPLE_REGEX)
  private val inputText = configuration.register("inputText", "", INPUT, EXAMPLE_INPUT_TEXT)

  private val inputEditor: DeveloperToolEditor by lazy { createInputEditor() }

  private val regexMatchingAttributes by lazy { EditorColorsManager.getInstance().globalScheme.getAttributes(SEARCH_RESULT_ATTRIBUTES) }
  private val selectedMatchResultHighlightingAttributes by lazy { EditorColorsManager.getInstance().globalScheme.getAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES) }
  private val matchResultsTableModel by lazy { MatchResultsTableModel() }

  private val regexInputErrorHolder by lazy { ErrorHolder() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun configurationChanged(property: ValueProperty<out Any>) {
    match()
  }

  override fun Panel.buildUi() {
    row {
      cell(RegexTextField(project, parentDisposable, regexText))
        .label("Regular expression:", LabelPosition.TOP)
        .validationOnApply(regexInputErrorHolder.asValidation())
        .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
        .align(Align.FILL)
        .resizableColumn()
        .gap(RightGap.SMALL)
      cell(SelectRegexOptionsAction.createActionButton(selectedRegexOptionFlag))
    }.topGap(TopGap.NONE)

    row {
      cell(inputEditor.component).align(Align.FILL)
    }.resizableRow().topGap(TopGap.SMALL)

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
      val matchResultsTable = MatchResultsTable(matchResultsTableModel, highlightSelectedMatchResults).apply {
        whenFocusLost(parentDisposable) {
          inputEditor.removeTextRangeHighlighters(SELECTED_MATCH_RESULT_HIGHLIGHTING_GROUP_ID)
        }
      }
      val matchResultsTableWrapper = ScrollPaneFactory.createScrollPane(matchResultsTable).apply {
        minimumSize = Dimension(minimumSize.width, 150)
        preferredSize = Dimension(preferredSize.width, 150)
        allowUiDslLabel(this)
      }
      cell(matchResultsTableWrapper)
        .label("Matches:", LabelPosition.TOP)
        .align(Align.FILL)
    }.topGap(TopGap.SMALL)
  }

  override fun afterBuildUi() {
    match()
  }

  override fun activated() {
    match()
    configuration.addChangeListener(parentDisposable, this)
  }

  override fun deactivated() {
    configuration.removeChangeListener(this)
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  /**
   * Kotlin and Java do not support the retrieval of all named groups yet:
   * [KT-51671](https://youtrack.jetbrains.com/issue/KT-51671). Therefore, we
   * are using Google's [com.google.code.regexp.Pattern] for now.
   */
  private fun match() {
    inputEditor.removeAllTextRangeHighlighters()
    regexInputErrorHolder.clear()
    matchResultsTableModel.setMatches(emptyList())

    val regex = regexText.get()
    if (regex.isEmpty()) {
      return
    }

    try {
      val pattern = Pattern.compile(regex, selectedRegexOptionFlag.get())
      val matcher = pattern.matcher(inputEditor.text)

      val namedGroups = matcher.namedGroupsList()
      val results = mutableListOf<List<Any>>()
      var i = 0
      while (matcher.find()) {
        val textRange = TextRange(matcher.start(), matcher.end())
        inputEditor.highlightTextRange(textRange, REGEX_MATCH_HIGHLIGHT_LAYER, regexMatchingAttributes)

        results.add(listOf("${i + 1}" to textRange, matcher.group(), MATCH))
        if (namedGroups.isNotEmpty()) {
          namedGroups[i].filter { it.value != null }.forEach {
            results.add(listOf(it.key to TextRange(matcher.start(it.key), matcher.end(it.key)), it.value, NAMED_GROUP))
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

  private fun createInputEditor() =
    DeveloperToolEditor("input", context, configuration, project, "Text", DeveloperToolEditor.EditorMode.INPUT, parentDisposable, inputText)
      .onTextChangeFromUi { match() }

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
      autoResizeMode = JTable.AUTO_RESIZE_LAST_COLUMN
      setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
      rowSelectionAllowed = true
      columnSelectionAllowed = false
      setDefaultRenderer(Object::class.java, MatchResultsTableCellRenderer(model))
      selectionModel.addListSelectionListener(createSelectionListener())
      setContextMenu(this::class.java.name, DefaultActionGroup(CopyValuesAction()))
      setEmptyState("No matches")
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

  private class MatchResultsTableModel : AbstractTableModel() {

    private var matches = listOf<List<Any>>()

    fun setMatches(matches: List<List<Any>>) {
      this.matches = matches
      fireTableDataChanged()
    }

    override fun getRowCount(): Int = matches.size

    override fun getColumnCount(): Int = 2

    override fun getColumnName(column: Int): String = when (column) {
      0 -> "Group"
      1 -> "Value"
      else -> error("Unknown column: $column")
    }

    override fun getValueAt(row: Int, column: Int): Any = matches[row][column]

    fun getMatchResultType(row: Int): MatchResultType = matches[row][2] as MatchResultType
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class MatchResultsTableCellRenderer(private val model: MatchResultsTableModel) : ColoredTableCellRenderer() {

    @Suppress("UNCHECKED_CAST")
    override fun customizeCellRenderer(table: JTable, value: Any?, selected: Boolean, hasFocus: Boolean, row: Int, column: Int) {
      val matchResultType = model.getMatchResultType(row)

      when (column) {
        0 -> {
          val prefix = when (matchResultType) {
            MATCH -> "Match: "
            NAMED_GROUP -> "Group: "
          }
         val (group, textRange) = (value as Pair<String, TextRange>)
          append(prefix, REGULAR_ATTRIBUTES)
          append("$group ", REGULAR_BOLD_ATTRIBUTES)
          append("(${textRange.startOffset} to ${textRange.endOffset})", GRAY_SMALL_ATTRIBUTES)

        }

        1 -> append(value as String)

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
      menuTitle = "Regular Expression",
      contentTitle = "Regular Expression Matcher"
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

    private const val REGEX_MATCH_HIGHLIGHT_LAYER = HighlighterLayer.SELECTION - 2
    private const val REGEX_MATCH_SELECTED_HIGHLIGHT_LAYER = HighlighterLayer.SELECTION - 1

    private const val SELECTED_MATCH_RESULT_HIGHLIGHTING_GROUP_ID = "matchResultHighlighting"

    private val matchResultAfterFirstMatchBorder = JBUI.Borders.customLineTop(JBUI.CurrentTheme.CustomFrameDecorations.separatorForeground())
    private val matchResultGroupBorder = JBUI.Borders.emptyLeft(5)
  }
}