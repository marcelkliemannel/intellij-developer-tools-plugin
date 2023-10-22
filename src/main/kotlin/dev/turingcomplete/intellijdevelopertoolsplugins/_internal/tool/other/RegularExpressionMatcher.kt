package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.other

import com.google.code.regexp.Pattern
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionToolbar.DEFAULT_MINIMUM_BUTTON_SIZE
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.impl.ActionButton
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.EditorColors.SEARCH_RESULT_ATTRIBUTES
import com.intellij.openapi.editor.colors.EditorColors.TEXT_SEARCH_RESULT_ATTRIBUTES
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.observable.util.whenFocusLost
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.setEmptyState
import com.intellij.openapi.util.TextRange
import com.intellij.ui.ColoredTableCellRenderer
import com.intellij.ui.EditorTextField
import com.intellij.ui.LanguageTextField
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes.GRAYED_ATTRIBUTES
import com.intellij.ui.SimpleTextAttributes.REGULAR_ATTRIBUTES
import com.intellij.ui.SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBViewport
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.TopGap
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.whenStateChangedFromUi
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijbytecodeplugin._ui.CopyValuesAction
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperTool
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.CommonsDataKeys
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.DeveloperToolEditor
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.allowUiDslLabel
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.setContextMenu
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.other.RegularExpressionMatcher.MatchResultType.MATCH
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.other.RegularExpressionMatcher.MatchResultType.NAMED_GROUP
import dev.turingcomplete.intellijdevelopertoolsplugins.common.ValueProperty
import org.intellij.lang.regexp.RegExpLanguage
import org.intellij.lang.regexp.intention.CheckRegExpForm
import java.awt.Dimension
import java.lang.Boolean.TRUE
import javax.swing.BorderFactory
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.event.ListSelectionListener
import javax.swing.table.AbstractTableModel
import java.util.regex.Pattern as JavaPattern

class RegularExpressionMatcher(
  private val context: DeveloperToolContext,
  private val configuration: DeveloperToolConfiguration,
  private val project: Project?,
  parentDisposable: Disposable
) : DeveloperTool(parentDisposable), DeveloperToolConfiguration.ChangeListener {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var selectedRegexOptionFlag = configuration.register("regexOption", 0)

  private val regexText = configuration.register("regexText", "", INPUT, EXAMPLE_REGEX)
  private val inputText = configuration.register("inputText", "", INPUT, EXAMPLE_INPUT_TEXT)

  private val regexInputEditor: EditorTextField by lazy { createRegexInputEditor(project) }
  private val inputEditor: DeveloperToolEditor by lazy { createInputEditor() }

  private val regexMatchingAttributes by lazy { EditorColorsManager.getInstance().globalScheme.getAttributes(SEARCH_RESULT_ATTRIBUTES) }
  private val selectedMatchResultHighlightingAttributes by lazy { EditorColorsManager.getInstance().globalScheme.getAttributes(TEXT_SEARCH_RESULT_ATTRIBUTES) }
  private val matchResultsTableModel by lazy { MatchResultsTableModel() }

  private val regexInputErrorHolder by lazy { ErrorHolder() }

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun configurationChanged(key: String, property: ValueProperty<out Any>) {
    match()
  }

  override fun Panel.buildUi() {
    row {
      cell(regexInputEditor)
        .label("Regular expression:", LabelPosition.TOP)
        .validationOnApply(regexInputErrorHolder.asValidation())
        .align(Align.FILL)
        .resizableColumn()
        .gap(RightGap.SMALL)
      cell(createSelectRegexOptionsButton())
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
   * Kotlin and Java does not support the retrieval of all named groups yet:
   * [KT-51671](https://youtrack.jetbrains.com/issue/KT-51671). Therefore, we
   * are using Google's [com.google.code.regexp.Pattern] for now.
   */
  private fun match() {
    inputEditor.removeAllTextRangeHighlighters()
    regexInputErrorHolder.clear()
    matchResultsTableModel.setMatches(emptyList())

    val regex = regexInputEditor.text
    if (regex.isEmpty()) {
      return
    }

    try {
      val pattern = Pattern.compile(regex, selectedRegexOptionFlag.get())
      val matcher = pattern.matcher(inputEditor.text)

      val namedGroups = matcher.namedGroups()
      val results = mutableListOf<List<Any>>()
      var i = 0
      while (matcher.find()) {
        val textRange = TextRange(matcher.start(), matcher.end())
        inputEditor.highlightTextRange(textRange, REGEX_MATCH_HIGHLIGHT_LAYER, regexMatchingAttributes)

        results.add(listOf("${i + 1}", textRange, matcher.group(), MATCH))
        if (namedGroups.isNotEmpty()) {
          namedGroups[i].filter { it.value != null }.forEach {
            results.add(listOf(it.key, TextRange(matcher.start(it.key), matcher.end(it.key)), it.value, NAMED_GROUP))
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

  private fun createRegexInputEditor(project: Project?): EditorTextField =
    object : LanguageTextField(RegExpLanguage.INSTANCE, project, EXAMPLE_REGEX, true) {

      init {
        text = regexText.get()
        addDocumentListener(object : DocumentListener {
          override fun documentChanged(event: DocumentEvent) {
            if (!isDisposed) {
              regexText.set(event.document.text, "fromRegexInputEditor")
              match()
            }
          }
        })
        allowUiDslLabel(this.component)

        regexText.afterChangeConsumeEvent(parentDisposable) { event ->
          if (event.newValue != event.oldValue && event.id != "fromRegexInputEditor") {
            text = event.newValue
          }
        }
      }

      override fun onEditorAdded(editor: Editor) {
        editor.putUserData(CheckRegExpForm.Keys.CHECK_REG_EXP_EDITOR, TRUE)
      }
    }

  private fun createSelectRegexOptionsButton(): ActionButton =
    ActionButton(
      SelectRegexOptionsAction(regexInputEditor, selectedRegexOptionFlag),
      null,
      RegularExpressionMatcher::class.java.name,
      DEFAULT_MINIMUM_BUTTON_SIZE
    )

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class SelectRegexOptionsAction(
    val regexInputEditor: EditorTextField,
    val selectedRegexOptionFlag: ObservableMutableProperty<Int>
  ) : DumbAwareAction("Regular Expression Options", null, AllIcons.General.GearPlain) {

    override fun actionPerformed(e: AnActionEvent) {
      JBPopupFactory.getInstance().createBalloonBuilder(createRegexOptionPanel())
        .setDialogMode(true)
        .setFillColor(UIUtil.getPanelBackground())
        .setBlockClicksThroughBalloon(true)
        .setRequestFocus(true)
        .createBalloon()
        .apply {
          setAnimationEnabled(false)
          show(RelativePoint.getSouthOf(regexInputEditor), Balloon.Position.below)
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT

    @Suppress("UnstableApiUsage")
    private fun createRegexOptionPanel() = panel {
      val regexOptionCheckBox = mutableMapOf<RegexOption, JBCheckBox>()

      val setSelectedRegexOptionFlag: () -> Unit = {
        selectedRegexOptionFlag.set(regexOptionCheckBox.filter { it.value.isSelected }.map { it.key.patternFlag }.sum())
      }

      val selectedRegexOptionFlag = selectedRegexOptionFlag.get()
      RegexOption.values().forEach { regexOption ->
        row {
          regexOptionCheckBox[regexOption] = checkBox(regexOption.title)
            .comment(regexOption.description)
            .applyToComponent { isSelected = regexOption.isSelected(selectedRegexOptionFlag) }
            .whenStateChangedFromUi { setSelectedRegexOptionFlag() }
            .component
        }
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class MatchResultsTable(
    private val model: MatchResultsTableModel,
    private val selectedMatchResultHighlight: (List<TextRange>) -> Unit
  ) : JBTable(model), DataProvider {

    init {
      columnModel.apply {
        getColumn(0).preferredWidth = 120
        getColumn(1).preferredWidth = 50
        getColumn(2).preferredWidth = 300
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
      CommonsDataKeys.SELECTED_VALUES.`is`(dataId) -> selectedRows.map { model.getValueAt(it, 2) as String }.toList()
      else -> null
    }

    private fun createSelectionListener() = ListSelectionListener { e ->
      if (!e.valueIsAdjusting) {
        val selectedTextRanges = this@MatchResultsTable.selectedRows.map { model.getValueAt(it, 1) as TextRange }.toList()
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

    override fun getColumnCount(): Int = 3

    override fun getColumnName(column: Int): String = when (column) {
      0 -> "Group"
      1 -> "Range"
      2 -> "Value"
      else -> error("Unknown column: $column")
    }

    override fun getValueAt(row: Int, column: Int): Any = matches[row][column]

    fun getMatchResultType(row: Int): MatchResultType = matches[row][3] as MatchResultType
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class MatchResultsTableCellRenderer(private val model: MatchResultsTableModel) : ColoredTableCellRenderer() {

    override fun customizeCellRenderer(table: JTable, value: Any?, selected: Boolean, hasFocus: Boolean, row: Int, column: Int) {
      val matchResultType = model.getMatchResultType(row)

      when (column) {
        0 -> {
          val prefix = when (matchResultType) {
            MATCH -> "Match: "
            NAMED_GROUP -> "Group: "
          }
          append(prefix, GRAYED_ATTRIBUTES)
          append(value as String, REGULAR_BOLD_ATTRIBUTES)
        }

        1 -> {
          val textRange = value as TextRange
          append("${textRange.startOffset} to ${textRange.endOffset}", REGULAR_ATTRIBUTES)
        }

        2 -> append(value as String)

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

  private enum class RegexOption(val patternFlag: Int, val title: String, val description: String? = null) {

    CASE_INSENSITIVE(
      JavaPattern.CASE_INSENSITIVE,
      "Case-insensitive",
      "Case-insensitive matching will use characters for the US-ASCII charset for matching."
    ),
    UNICODE_CASE(
      JavaPattern.UNICODE_CASE,
      "Unicode-aware",
      "The <code>case insensitive</code> option will use the Unicode standard."
    ),
    MULTILINE(
      JavaPattern.MULTILINE,
      "Multiline",
      "The expressions <code>^</code> and <code>\$</code> match just after or just before, respectively, a line terminator or the end of the input sequence."
    ),
    DOTALL(
      JavaPattern.DOTALL,
      "Dotall",
      "The expression <code>.</code> will also match line terminators."
    ),
    CANON_EQ(
      JavaPattern.CANON_EQ,
      "Canonical equivalence",
      "Two characters will be considered to match if, and only if, their full canonical decompositions match."
    ),
    UNIX_LINES(
      JavaPattern.UNIX_LINES,
      "Unix line endings",
      "Only the <code>\\n</code> line terminator is recognized in the behavior of <code>.</code>, <code>^</code>, and <code>\$</code>."
    ),
    LITERAL(
      JavaPattern.LITERAL,
      "Literal parsing of the pattern",
      "The input string that specifies the pattern will be treated as a sequence of literal characters."
    ),
    COMMENTS(
      JavaPattern.COMMENTS,
      "Permit whitespace and comments in pattern",
      "Whitespace will be ignored, and embedded comments starting with <code>#</code> are ignored until the end of a line."
    );

    fun isSelected(regexOptionFlag: Int) = regexOptionFlag.and(patternFlag) != 0
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory<RegularExpressionMatcher> {

    override fun getDeveloperToolPresentation() = DeveloperToolPresentation(
      menuTitle = "Regular Expression",
      contentTitle = "Regular Expression Matcher"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperToolContext
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