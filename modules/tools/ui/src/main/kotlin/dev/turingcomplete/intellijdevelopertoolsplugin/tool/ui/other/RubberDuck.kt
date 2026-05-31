package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.other

import com.intellij.ide.BrowserUtil
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.util.ui.components.BorderLayoutPanel
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiTool
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base.DeveloperUiToolPresentation
import dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.message.UiToolsBundle
import java.awt.Image.SCALE_SMOOTH
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import javax.swing.JLabel

class RubberDuck(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) : DeveloperUiTool(parentDisposable) {
  // -- Properties ---------------------------------------------------------- //

  private val duckName = configuration.register(
    key = "duckName",
    defaultValue = "Rubber Duck",
    propertyType = DeveloperToolConfiguration.PropertyType.INPUT
  )

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun Panel.buildUi() {
    row { cell(JBLabel(UiToolsBundle.message("rubber-duck.description"))) }

    row("Duck Name:") {
      textField()
        .bindText(duckName)
        .align(Align.FILL)
    }

    row {
        cell(
            BorderLayoutPanel().apply {
              RubberDuck::class
                .java
                .getResourceAsStream(
                  "/dev/turingcomplete/intellijdevelopertoolsplugin/rubber-duck-yellow.png"
                )
                ?.use {
                  val read = ImageIO.read(it)
                  val scaledInstance =
                    read.getScaledInstance(read.width.div(2), read.height.div(2), SCALE_SMOOTH)

                  val clickableDuck = JLabel(ImageIcon(scaledInstance)).apply {
                    toolTipText = "Quack me!"
                    addMouseListener(object : java.awt.event.MouseAdapter() {
                      override fun mouseClicked(e: java.awt.event.MouseEvent?) {
                        Messages.showInfoMessage(
                          "Hello, I'm ${duckName.get()} !",
                          "🐤 Quack!"
                        )
                      }
                    })
                  }

                  addToCenter(clickableDuck)
                }
            }
          )
          .align(Align.CENTER)
      }
      .resizableRow()

    row {
      comment(UiToolsBundle.message("rubber-duck.image-attribution")) { BrowserUtil.browse(it.url) }
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<RubberDuck> {

    override fun getDeveloperUiToolPresentation() =
      DeveloperUiToolPresentation(
        menuTitle = UiToolsBundle.message("rubber-duck.menu-title"),
        contentTitle = UiToolsBundle.message("rubber-duck.content-title"),
      )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext,
    ): ((DeveloperToolConfiguration) -> RubberDuck) = { configuration  -> RubberDuck(configuration , parentDisposable) }
  }

  // -- Companion Object ---------------------------------------------------- //
}
