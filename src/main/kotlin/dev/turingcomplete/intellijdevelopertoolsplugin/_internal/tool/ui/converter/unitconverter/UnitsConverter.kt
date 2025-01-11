package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.converter.unitconverter

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.TabbedPaneWrapper
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.BottomGap
import com.intellij.ui.dsl.builder.Panel
import com.intellij.util.ui.JBUI.Borders
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ScrollPaneBuilder
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration.ResetListener
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolPresentation
import javax.swing.ScrollPaneConstants
import javax.swing.event.ChangeEvent
import javax.swing.event.ChangeListener

class UnitsConverter(
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : DeveloperUiTool(parentDisposable), ResetListener, ChangeListener {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var lastSelectedUnitConverterIndex by configuration.register("lastSelectedUnitConverterIndex", DEFAULT_LAST_SELECTED_UNIT_CONVERTER_INDEX)

  private val unitConverters: List<UnitConverter> = listOf(
    TimeConverter(configuration, parentDisposable),
    DataSizeConverter(configuration, parentDisposable),
    TransferRateConverter(configuration, parentDisposable),
    BaseConverter(configuration, parentDisposable)
  )
  private var selectedUnitConverter: UnitConverter = unitConverters[0]

  private lateinit var unitConvertersTabbedPanel: TabbedPaneWrapper

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    wrapComponentInScrollPane = false
  }

  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun Panel.buildUi() {
    unitConvertersTabbedPanel = TabbedPaneWrapper(parentDisposable).apply {
      val tabBorder = Borders.emptyTop(12)
      unitConverters.forEach { converter ->
        val component = converter.createComponent().apply {
          border = tabBorder
        }
        addTab(
          converter.title,
          ScrollPaneBuilder(component)
            .horizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
            .build()
        )
      }

      val initialSelectedUnitConverterIndex = if (lastSelectedUnitConverterIndex < this.tabCount) {
        lastSelectedUnitConverterIndex
      }
      else {
        DEFAULT_LAST_SELECTED_UNIT_CONVERTER_INDEX
      }
      this.selectedIndex = initialSelectedUnitConverterIndex
    }
    row {
      cell(BorderLayoutPanel().apply { addToCenter(unitConvertersTabbedPanel.component) })
        .resizableColumn()
        .align(Align.FILL)
    }.bottomGap(BottomGap.SMALL).resizableRow()
  }

  override fun activated() {
    selectedUnitConverter.activate()

    configuration.addResetListener(parentDisposable, this)

    unitConvertersTabbedPanel.addChangeListener(this)
  }

  override fun deactivated() {
    selectedUnitConverter.deactivate()

    configuration.removeResetListener(this)

    unitConvertersTabbedPanel.removeChangeListener(this)
  }

  override fun stateChanged(e: ChangeEvent?) {
    val oldSelectedUnitConverter = selectedUnitConverter
    val newSelectedUnitConverter = unitConverters[unitConvertersTabbedPanel.selectedIndex]
    if (oldSelectedUnitConverter != newSelectedUnitConverter) {
      oldSelectedUnitConverter.deactivate()
      newSelectedUnitConverter.activate()
      selectedUnitConverter = newSelectedUnitConverter
      lastSelectedUnitConverterIndex = unitConverters.indexOf(newSelectedUnitConverter)
    }
  }

  override fun configurationReset() {
    sync()
  }

  override fun afterBuildUi() {
    sync()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun sync() {
    unitConverters.forEach { it.sync() }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<UnitsConverter> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "Units Converter",
      contentTitle = "Units Converter"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> UnitsConverter) =
      { configuration -> UnitsConverter(configuration, parentDisposable) }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val DEFAULT_LAST_SELECTED_UNIT_CONVERTER_INDEX = 0
  }
}