package dev.turingcomplete.intellijdevelopertoolsplugins._internal

import com.intellij.openapi.extensions.CustomLoadingExtensionPointBean
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.util.xmlb.annotations.Attribute
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory

class DeveloperToolFactoryEp<T : DeveloperToolFactory<*>> : CustomLoadingExtensionPointBean<T>() {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  @Attribute("id")
  @RequiredElement
  lateinit var id: String

  @Attribute("implementationClass")
  @RequiredElement
  lateinit var implementationClass: String

  @Attribute("preferredSelected")
  var preferredSelected: Boolean = false

  @Attribute("weight")
  var weight: Int? = Int.MAX_VALUE

  @Attribute("groupId")
  var groupId: String? = null

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun getImplementationClassName(): String = implementationClass

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    val EP_NAME: ExtensionPointName<DeveloperToolFactoryEp<out DeveloperToolFactory<*>>> =
      ExtensionPointName.create("dev.turingcomplete.intellijdevelopertoolsplugins.developerTool")
  }
}