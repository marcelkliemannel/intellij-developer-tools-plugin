package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.converter.base

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.getUserData
import com.intellij.openapi.ui.putUserData
import com.intellij.openapi.util.Key
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijdevelopertoolsplugin.common.ValueProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.AdvancedEditor
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.TitledTabbedPane
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common.onSelectionChanged
import javax.swing.JComponent

class ConversionSideHandler(
  private val title: String,
  private val configuration: DeveloperToolConfiguration,
  private val project: Project?,
  private val context: DeveloperUiToolContext,
  private val parentDisposable: Disposable,
  private val conversionEnabled: ValueProperty<Boolean>,
  private val liveConversionRequested: () -> Unit,
  private val diffSupport: AdvancedEditor.DiffSupport,
) {
  // -- Properties ---------------------------------------------------------- //

  private val inputOutputHandlers = mutableListOf<InputOutputHandler>()
  lateinit var activeInputOutputHandler: ValueProperty<InputOutputHandler>

  private lateinit var component: JComponent

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun createComponent(): JComponent {
    component =
      BorderLayoutPanel().apply {
        check(inputOutputHandlers.isNotEmpty()) { "No input/output handler have been added" }

        activeInputOutputHandler = ValueProperty<InputOutputHandler>(inputOutputHandlers[0])

        //      if (inputOutputHandlers.size == 1) {
        //        addToCenter(inputOutputHandlers.first().createComponent())
        //      } else {
        val tabs =
          inputOutputHandlers.map { inputOutputHandler ->
            inputOutputHandler.title to
              inputOutputHandler.createComponent().apply {
                putUserData(inputOutputHandlerUserObjectKey, inputOutputHandler)
              }
          }
        addToCenter(
          TitledTabbedPane(title, tabs).apply {
            onSelectionChanged {
              activeInputOutputHandler.set(it.getUserData(inputOutputHandlerUserObjectKey)!!)
            }
          }
        )
        //      }
      }

    conversionEnabled.afterChange(parentDisposable) { component.isEnabled = it }

    return component
  }

  fun addTextInputOutputHandler(id: String, defaultText: String = ""): TextInputOutputHandler {
    val textInputOutputHandler =
      TextInputOutputHandler(
        id = id,
        configuration = configuration,
        context = context,
        parentDisposable = parentDisposable,
        project = project,
        focusGained = { activeInputOutputHandler.set(it) },
        liveConversionRequested = {
          activeInputOutputHandler.set(it)
          liveConversionRequested()
        },
        diffSupport = diffSupport,
        defaultText = defaultText,
      )
    inputOutputHandlers.add(textInputOutputHandler)
    return textInputOutputHandler
  }

  fun addFileInputOutputHandler(id: String): FileInputOutputHandler {
    val fileInputOutputHandler =
      FileInputOutputHandler(
        id = id,
        configuration = configuration,
        context = context,
        parentDisposable = parentDisposable,
        project = project,
      )
    inputOutputHandlers.add(fileInputOutputHandler)
    return fileInputOutputHandler
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val inputOutputHandlerUserObjectKey =
      Key.create<InputOutputHandler>("inputOutputHandlerUserObject")
  }
}
