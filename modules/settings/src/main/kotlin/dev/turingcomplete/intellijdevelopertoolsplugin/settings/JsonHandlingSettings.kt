package dev.turingcomplete.intellijdevelopertoolsplugin.settings

import dev.turingcomplete.intellijdevelopertoolsplugin.settings.JsonHandlingSettings.Companion.READ_CGROUP_ID
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.JsonHandlingSettings.Companion.WRITE_GROUP_ID
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.BooleanSettingProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.BooleanValue
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.IntSettingProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.IntValue
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.Setting
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.Settings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingsGroup

@SettingsGroup(id = WRITE_GROUP_ID, titleBundleKey = "json-handling-settings.write-group.title")
@SettingsGroup(id = READ_CGROUP_ID, titleBundleKey = "json-handling-settings.read-group.title")
interface JsonHandlingSettings : Settings {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  @Setting(titleBundleKey = "json-handling-settings.write-quote-field-names.title", groupId = WRITE_GROUP_ID)
  @BooleanValue(defaultValue = false)
  val writeQuoteFieldNames: BooleanSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.write-nan-as-strings.title", groupId = WRITE_GROUP_ID)
  @BooleanValue(defaultValue = false)
  val writeNanAsStrings: BooleanSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.write-numbers-as-strings.title", groupId = WRITE_GROUP_ID)
  @BooleanValue(defaultValue = false)
  val writeNumbersAsStrings: BooleanSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.write-escape-non-ascii.title", groupId = WRITE_GROUP_ID)
  @BooleanValue(defaultValue = false)
  val writeEscapeNonAscii: BooleanSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.write-hex-uppercase.title", groupId = WRITE_GROUP_ID)
  @BooleanValue(defaultValue = false)
  val writeHexUpperCase: BooleanSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.write-intention-spaces.title", groupId = WRITE_GROUP_ID)
  @IntValue(defaultValue = 2, min = 0, max = 10)
  val writeIntentionSpaces: IntSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.read-allow-java-comments.title", groupId = READ_CGROUP_ID)
  @BooleanValue(defaultValue = false)
  val readAllowJavaComments: BooleanSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.read-allow-yaml-comments.title", groupId = READ_CGROUP_ID)
  @BooleanValue(defaultValue = false)
  val readAllowYamlComments: BooleanSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.read-allow-single-quotes.title", groupId = READ_CGROUP_ID)
  @BooleanValue(defaultValue = false)
  val readAllowSingleQuotes: BooleanSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.read-allow-unquoted-field-names.title", groupId = READ_CGROUP_ID)
  @BooleanValue(defaultValue = false)
  val readAllowUnquotedFieldNames: BooleanSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.read-allow-unescaped-control-characters.title", groupId = READ_CGROUP_ID)
  @BooleanValue(defaultValue = false)
  val readAllowUnescapedControlChars: BooleanSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.read-allow-backslash-escaping-any-character.title", groupId = READ_CGROUP_ID)
  @BooleanValue(defaultValue = false)
  val readAllowBackslashEscapingAnyCharacter: BooleanSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.read-allow-leading-zeros-for-numbers.title", groupId = READ_CGROUP_ID)
  @BooleanValue(defaultValue = false)
  val readAllowLeadingZerosForNumbers: BooleanSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.read-allow-leading-plus-sign-for-numbers.title", groupId = READ_CGROUP_ID)
  @BooleanValue(defaultValue = false)
  val readAllowLeadingPlusSignForNumbers: BooleanSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.read-allow-leading-decimal-point-for-numbers.title", groupId = READ_CGROUP_ID)
  @BooleanValue(defaultValue = false)
  val readAllowLeadingDecimalPointForNumbers: BooleanSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.read-allow-trailing-decimal-point-for-numbers.title", groupId = READ_CGROUP_ID)
  @BooleanValue(defaultValue = false)
  val readAllowTrailingDecimalPointForNumbers: BooleanSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.read-allow-non-numeric-numbers.title", groupId = READ_CGROUP_ID)
  @BooleanValue(defaultValue = false)
  val readAllowNonNumericNumbers: BooleanSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.read-allow-missing-values.title", groupId = READ_CGROUP_ID)
  @BooleanValue(defaultValue = false)
  val readAllowMissingValues: BooleanSettingProperty

  @Setting(titleBundleKey = "json-handling-settings.read-allow-trailing-comma.title", groupId = READ_CGROUP_ID)
  @BooleanValue(defaultValue = false)
  val readAllowTrailingComma: BooleanSettingProperty

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //
  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private const val WRITE_GROUP_ID = "write"
    private const val READ_CGROUP_ID = "read"
  }
}
