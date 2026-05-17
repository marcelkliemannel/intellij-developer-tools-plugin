package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

interface Settings {
  // -- Properties ---------------------------------------------------------- //

  val modificationsCounter: Int

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  fun <S : Any, T : Annotation, U : SettingProperty<S, T>> getSetting(settingsName: String): U

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
