package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.base

import com.intellij.openapi.extensions.CustomLoadingExtensionPointBean
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.util.xmlb.annotations.Attribute

class DeveloperUiToolFactoryEp<T : DeveloperUiToolFactory<*>> : CustomLoadingExtensionPointBean<T>() {
  // -- Properties ---------------------------------------------------------- //

  @Attribute("id")
  @RequiredElement
  lateinit var id: String

  @Attribute("implementationClass")
  @RequiredElement
  lateinit var implementationClass: String

  @Attribute("preferredSelected")
  var preferredSelected: Boolean = false

  @Attribute("groupId")
  var groupId: String? = null

  @Attribute("internalTool")
  var internalTool: Boolean = false

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  override fun getImplementationClassName(): String = implementationClass

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //

  companion object {

    val EP_NAME: ExtensionPointName<DeveloperUiToolFactoryEp<out DeveloperUiToolFactory<*>>> =
      ExtensionPointName.create("dev.turingcomplete.intellijdevelopertoolsplugins.developerUiTool")
  }
}
