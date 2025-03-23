package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

interface Settings {
  // -- Properties ---------------------------------------------------------- //

  val modificationsCounter: Int

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  fun <S, T, U: SettingProperty<S, T>> getSetting(settingsName: String): U

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
