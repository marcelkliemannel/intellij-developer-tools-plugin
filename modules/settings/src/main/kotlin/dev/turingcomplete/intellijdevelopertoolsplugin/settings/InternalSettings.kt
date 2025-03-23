package dev.turingcomplete.intellijdevelopertoolsplugin.settings

import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.BooleanSettingProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.BooleanValue
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.InternalSetting
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.Settings

interface InternalSettings : Settings {
  // -- Properties ---------------------------------------------------------- //

  @InternalSetting
  @BooleanValue(defaultValue = true)
  val promoteAddOpenMainDialogActionToMainToolbar: BooleanSettingProperty

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
