package dev.turingcomplete.intellijdevelopertoolsplugin.settings

import kotlin.reflect.KClass

interface DeveloperToolConfigurationPropertyType<T : Any> {
  // -- Properties ---------------------------------------------------------- //

  val id: String
  val typeClass: KClass<T>
  val legacyId: String?

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  fun fromPersistent(persistentValue: String): T

  fun toPersistent(value: Any): String

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  data class SimplePropertyType<T : Any>(
    override val id: String,
    override val typeClass: KClass<T>,
    val doFromPersistent: (String) -> T,
    val doToPersistent: (T) -> String,
    override val legacyId: String? = null,
  ) : DeveloperToolConfigurationPropertyType<T> {

    override fun fromPersistent(persistentValue: String): T = doFromPersistent(persistentValue)

    @Suppress("UNCHECKED_CAST")
    override fun toPersistent(value: Any): String = doToPersistent(value as T)
  }

  // -- Companion Object ---------------------------------------------------- //
}
