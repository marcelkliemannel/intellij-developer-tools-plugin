package dev.turingcomplete.intellijdevelopertoolsplugin.settings

import com.fasterxml.jackson.core.util.Separators
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.JsonHandlingSettings.Companion.READ_CGROUP_ID
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.JsonHandlingSettings.Companion.WRITE_GROUP_ID
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.BooleanSettingProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.BooleanValue
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.EnumSettingProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.EnumValue
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.IntSettingProperty
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.IntValue
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.Setting
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.Settings
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingsGroup
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.message.SettingsBundle

@SettingsGroup(
  id = WRITE_GROUP_ID,
  titleBundleKey = "json-handling-settings.write-group.title",
  order = 0,
)
@SettingsGroup(
  id = READ_CGROUP_ID,
  titleBundleKey = "json-handling-settings.read-group.title",
  order = 1,
)
interface JsonHandlingSettings : Settings {
  // -- Properties ---------------------------------------------------------- //

  @Setting(
    titleBundleKey = "json-handling-settings.write-quote-field-names.title",
    groupId = WRITE_GROUP_ID,
    order = 0,
  )
  @BooleanValue(defaultValue = true)
  val writeQuoteFieldNames: BooleanSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.write-nan-as-strings.title",
    groupId = WRITE_GROUP_ID,
    order = 1,
  )
  @BooleanValue(defaultValue = true)
  val writeNanAsStrings: BooleanSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.write-numbers-as-strings.title",
    groupId = WRITE_GROUP_ID,
    order = 2,
  )
  @BooleanValue(defaultValue = false)
  val writeNumbersAsStrings: BooleanSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.write-escape-non-ascii.title",
    groupId = WRITE_GROUP_ID,
    order = 3,
  )
  @BooleanValue(defaultValue = false)
  val writeEscapeNonAscii: BooleanSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.write-hex-uppercase.title",
    groupId = WRITE_GROUP_ID,
    order = 4,
  )
  @BooleanValue(defaultValue = true)
  val writeHexUpperCase: BooleanSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.write-intention-spaces.title",
    groupId = WRITE_GROUP_ID,
    order = 5,
  )
  @IntValue(defaultValue = 2, min = 0, max = 10)
  val writeIntentionSpaces: IntSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.write-spacing-object-field-value.title",
    groupId = WRITE_GROUP_ID,
    order = 6,
  )
  @EnumValue<Separators.Spacing>(
    enumClass = Separators.Spacing::class,
    defaultValueName = "AFTER",
    displayTextProvider = SeparatorsSpacingEnumDisplayTextProvider::class,
  )
  val writeSpacingObjectFieldValue: EnumSettingProperty<Separators.Spacing>

  @Setting(
    titleBundleKey = "json-handling-settings.write-spacing-object-entry.title",
    groupId = WRITE_GROUP_ID,
    order = 7,
  )
  @EnumValue<Separators.Spacing>(
    enumClass = Separators.Spacing::class,
    defaultValueName = "NONE",
    displayTextProvider = SeparatorsSpacingEnumDisplayTextProvider::class,
  )
  val writeSpacingObjectEntry: EnumSettingProperty<Separators.Spacing>

  @Setting(
    titleBundleKey = "json-handling-settings.write-spacing-array-value.title",
    groupId = WRITE_GROUP_ID,
    order = 8,
  )
  @EnumValue<Separators.Spacing>(
    enumClass = Separators.Spacing::class,
    defaultValueName = "NONE",
    displayTextProvider = SeparatorsSpacingEnumDisplayTextProvider::class,
  )
  val writeSpacingArrayValue: EnumSettingProperty<Separators.Spacing>

  @Setting(
    titleBundleKey = "json-handling-settings.read-allow-java-comments.title",
    groupId = READ_CGROUP_ID,
    order = 9,
  )
  @BooleanValue(defaultValue = false)
  val readAllowJavaComments: BooleanSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.read-allow-yaml-comments.title",
    groupId = READ_CGROUP_ID,
    order = 10,
  )
  @BooleanValue(defaultValue = false)
  val readAllowYamlComments: BooleanSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.read-allow-single-quotes.title",
    groupId = READ_CGROUP_ID,
    order = 11,
  )
  @BooleanValue(defaultValue = false)
  val readAllowSingleQuotes: BooleanSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.read-allow-unquoted-field-names.title",
    groupId = READ_CGROUP_ID,
    order = 12,
  )
  @BooleanValue(defaultValue = false)
  val readAllowUnquotedFieldNames: BooleanSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.read-allow-unescaped-control-characters.title",
    groupId = READ_CGROUP_ID,
    order = 13,
  )
  @BooleanValue(defaultValue = false)
  val readAllowUnescapedControlChars: BooleanSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.read-allow-backslash-escaping-any-character.title",
    groupId = READ_CGROUP_ID,
    order = 14,
  )
  @BooleanValue(defaultValue = false)
  val readAllowBackslashEscapingAnyCharacter: BooleanSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.read-allow-leading-zeros-for-numbers.title",
    groupId = READ_CGROUP_ID,
    order = 15,
  )
  @BooleanValue(defaultValue = false)
  val readAllowLeadingZerosForNumbers: BooleanSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.read-allow-leading-plus-sign-for-numbers.title",
    groupId = READ_CGROUP_ID,
    order = 16,
  )
  @BooleanValue(defaultValue = false)
  val readAllowLeadingPlusSignForNumbers: BooleanSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.read-allow-leading-decimal-point-for-numbers.title",
    groupId = READ_CGROUP_ID,
    order = 17,
  )
  @BooleanValue(defaultValue = false)
  val readAllowLeadingDecimalPointForNumbers: BooleanSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.read-allow-trailing-decimal-point-for-numbers.title",
    groupId = READ_CGROUP_ID,
    order = 18,
  )
  @BooleanValue(defaultValue = false)
  val readAllowTrailingDecimalPointForNumbers: BooleanSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.read-allow-non-numeric-numbers.title",
    groupId = READ_CGROUP_ID,
    order = 19,
  )
  @BooleanValue(defaultValue = false)
  val readAllowNonNumericNumbers: BooleanSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.read-allow-missing-values.title",
    groupId = READ_CGROUP_ID,
    order = 20,
  )
  @BooleanValue(defaultValue = false)
  val readAllowMissingValues: BooleanSettingProperty

  @Setting(
    titleBundleKey = "json-handling-settings.read-allow-trailing-comma.title",
    groupId = READ_CGROUP_ID,
    order = 21,
  )
  @BooleanValue(defaultValue = false)
  val readAllowTrailingComma: BooleanSettingProperty

  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //
  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  class SeparatorsSpacingEnumDisplayTextProvider :
    EnumValue.DisplayTextProvider<Separators.Spacing> {

    override fun toDisplayText(value: Separators.Spacing): String =
      when (value) {
        Separators.Spacing.NONE ->
          SettingsBundle.message("json-handling-settings.write-separators-spacing.none")
        Separators.Spacing.BEFORE ->
          SettingsBundle.message("json-handling-settings.write-separators-spacing.before")
        Separators.Spacing.AFTER ->
          SettingsBundle.message("json-handling-settings.write-separators-spacing.after")
        Separators.Spacing.BOTH ->
          SettingsBundle.message("json-handling-settings.write-separators-spacing.both")
      }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private const val WRITE_GROUP_ID = "write"
    private const val READ_CGROUP_ID = "read"
  }
}
