package dev.turingcomplete.intellijdevelopertoolsplugin._internal.common

import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.ui.setEmptyState
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.TableSpeedSearch
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.ColumnInfo
import com.intellij.util.ui.ListTableModel
import com.intellij.util.ui.UIUtil
import dev.turingcomplete.intellijdevelopertoolsplugin.common.uncheckedCastTo
import java.awt.Dimension
import javax.swing.ListSelectionModel
import javax.swing.SortOrder
import javax.swing.table.TableRowSorter

class SimpleTable<T>(
  items: List<T>,
  columns: List<ColumnInfo<T, String>>,
  private val toCopyValue: (T) -> String,
  initialSortedColumn: Pair<Int, SortOrder> = Pair(0, SortOrder.ASCENDING)
) : JBTable(), DataProvider {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    model = ListTableModel(columns.toTypedArray(), items, initialSortedColumn.first, initialSortedColumn.second)
    setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION)
    rowSelectionAllowed = true
    columnSelectionAllowed = false
    setContextMenu(
      this::class.java.name, DefaultActionGroup(
        CopyValuesAction(valueToString = {
          @Suppress("UNCHECKED_CAST")
          toCopyValue(it as T)
        })
      )
    )
    setEmptyState("No values")
    TableSpeedSearch.installOn(this)
  }

  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun reload() {
    model.uncheckedCastTo<ListTableModel<*>>().apply {
      fireTableDataChanged()
      rowSorter.uncheckedCastTo<TableRowSorter<ListTableModel<*>>>().sort()
    }
  }

  override fun getData(dataId: String): Any? = when {
    PluginCommonDataKeys.SELECTED_VALUES.`is`(dataId) -> {
      val tableModel = model.uncheckedCastTo<ListTableModel<T>>()
      selectedRows
        .map { tableModel.getRowValue(rowSorter.convertRowIndexToModel(it)) }
        .toList()
    }

    else -> null
  }

  fun asBalloon(parentDisposable: Disposable) = JBPopupFactory.getInstance()
    .createBalloonBuilder(ScrollPaneFactory.createScrollPane(this, false).apply {
      preferredSize = Dimension(400, 300)
    })
    .setDialogMode(true)
    .setFillColor(UIUtil.getPanelBackground())
    .setBorderColor(JBColor.border())
    .setBlockClicksThroughBalloon(true)
    .setRequestFocus(true)
    .setDisposable(parentDisposable)
    .createBalloon()
    .apply {
      setAnimationEnabled(false)
    }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}