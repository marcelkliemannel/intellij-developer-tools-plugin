package dev.turingcomplete.intellijdevelopertoolsplugin.settings

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.extensions.RequiredElement
import com.intellij.util.xmlb.annotations.Attribute
import dev.turingcomplete.intellijdevelopertoolsplugin.common.uncheckedCastTo
import kotlin.reflect.KClass

class DeveloperToolConfigurationEnumPropertyTypeEp<T: Enum<T>>: DeveloperToolConfigurationPropertyType<T> {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  @Attribute("id")
  @RequiredElement
  override lateinit var id: String

  @Attribute("type")
  @RequiredElement
  lateinit var type: String

  @Attribute("legacyId")
  override var legacyId: String? = null

  override val typeClass: KClass<T>
    get() = Class.forName(type).kotlin.uncheckedCastTo<KClass<T>>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun fromPersistent(persistentValue: String): T {
    return java.lang.Enum.valueOf(typeClass.java, persistentValue)
  }

  override fun toPersistent(value: Any): String =
    value.uncheckedCastTo<Enum<*>>().name

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    val epName: ExtensionPointName<DeveloperToolConfigurationEnumPropertyTypeEp<*>> =
      ExtensionPointName.create("dev.turingcomplete.intellijdevelopertoolsplugins.developerToolConfigurationEnumPropertyType")
  }
}