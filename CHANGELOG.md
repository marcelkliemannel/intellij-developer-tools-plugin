# Changelog

## Unreleased

### Added

- New tool "Units Converter" that supports data sizes and transfer rates conversion.

### Changed

- Tool "Time Conversion" moved into the new "Units Converter" tool

### Removed

### Fixed

## 5.0.0 - 2024-05-21

### Added

- Compatibility with IntelliJ 2024.2 EAP

## 4.3.0 - 2024-05-20

### Added

- Add keymap actions to show a developer tool
- Add optional strict secret/key requirements check to the "JSON Web Token (JWT) Decoder/Encoder" tool.
- Add gutter icon with readable a readable date/time format for UNIX timestamps in the "JSON Web Token (JWT) Decoder/Encoder" tool.
- The "JSON Web Token (JWT) Decoder/Encoder" tool interface now includes sliders to flexibly change the size of the editors.
- Add new tool "Text Filter".

### Changed

- The tool "JWT Decoder/Encoder" renamed to "JSON Web Token (JWT) Decoder/Encoder".
- Context menu action "Text Statistic..." was renamed to "Show Text Statistic of Document...".

### Removed

- The input of a public key for the JWT signature configuration was removed from the "JSON Web Token (JWT) Decoder/Encoder" tool.

## 4.2.0 - 2024-04-08

### Changed

- Lower IntelliJ compatibility to 2023.2 to support the latest Android Studio.
- In the tool window, the tools menu is now available through a separated action button.
- The workbench tabs are now hidden by default when there is only one tab. This behaviour can be changed in the settings. Creating a new workbench is now also available from the tools actions popup.

## 4.1.1 - 2024-03-28

### Fixed

- Fix incompatibility problems with IntelliJ 2024.1

## 4.1.0 - 2024-03-24

### Added

- Add automatic input text case detection to the text case converter 
- Add escape/unescape as editor actions and code intentions
- Add new tool: Text Statistic
- Add support for the "Dot Text Case"
- Add common hashing algorithms to the encoding editor action and code intention
- Add common SHA3 algorithms to the random data generator editor action

### Fixed

- Editor actions on Java Strings will now preserve the outer String quotations

## 4.0.0 - 2024-03-10

### Added

- Some tools (data generators, encoders/decoders and text case conversion) are now also available in the Editor menu or code intentions. Some of these actions are only available if a text is selected, or the current caret position is on a Java/Kotlin string or identifier.
- Extend the ULID generator for monotonic ULIDs
- New tool "IntelliJ Internals"

## 3.5.0 - 2024-02-28

### Added

- New tool 'Unarchiver' to analyze and extract archive files

### Changed

- Improve UI of the Regular Expression Matcher tool

### Fixed

- Lorem Ipsum text was regenerated each time the tool window was opened

## 3.4.0 - 2024-01-21

### Added

- Settings option to enable or disable the grouping of tools
- Settings option to sort the tools menu alphabetically

### Changed

- By default, the tools menu is a flat alphabetical list. The old behavior (e.g., grouping of nodes) can be restored through the settings.
- Removed the setting to hide the tool window menu on a tool selection. The selection mechanism now distinguishes between an automatic search result selection (the menu remains visible) and an user selection (a menu is hidden).
- Renamed tool "Java Text Escape/Unescape" to "Java String Escape/Unescape"
- Renamed tool "Code Formatting Converter" to "Text Format Converter"
- Text related tools moved to new group "text"

### Fixed

- Tool "Code Format Converter" wasn't working correctly

## 3.3.0 - 2024-01-15

### Added

- Add time conversion tool
- Add 'What's New' overview to the main menu

### Fixed

- Inputs and configurations for tools that were not opened were lost the next time the settings were saved.
- Loading the tool window settings causes an error if one of the tools has a saved secret.
- In the JWT Encoder/Decoder the "Public Key" field was visible for the HMAC signature algorithm.

## 3.2.0 - 2024-01-06

### Added

- Add ULID generator tool
- Add Nano ID generator tool
- Add color picker tool
- Add an action to open the Developer Tools section in IntelliJ's settings
- Improve the layout of the tools in the tool window

### Changed

- The UUID generator was moved to the "Cryptography" group

## 3.1.0 - 2023-12-29

### Added

- A new option in the settings to add the 'Developer Tools' action to the main toolbar during startup

### Changed

- The tool window is not activated on startup anymore if it was previously open, to avoid negatively impacting IntelliJ's startup time.

### Fixed

- Opening IntelliJ's settings window will reset the configuration to the default values.

## 3.0.0 - 2023-12-28

### Added

- The tools are now available through a tool window.

### Changed

- The Open Dialog action is still available but no longer automatically added to the main menu to favor the new tool window. To restore the old behavior, the action can be added again via **Customize Toolbar... | Add Actions... | Developer Tools**.
- Settings have been moved to IntelliJ's settings window

### Fixed

- Configuration reset does not reset default editor settings
- Setting "Remember configurations" wasn't persisted

## 2.0.1 - 2023-10-30

### Removed

- Remove not fully implemented JSON path library switch

## 2.0.0 - 2023-10-28

### Added

- Add CLI Command line breaks converter
- Add a "Set to Now" button to the date time converter
- Add capability to remember editor settings
- Add "Show Special Characters" setting to the editor
- Add "Show Whitespaces" setting to the editor
- Add default editor settings to the configuration
- Add an icon to indicate the current live conversion in text converters
- Add Base32/Base64 encoding capability for HMAC and JWT secrets ([GitHub Issue #16](https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues/16))
- Add automatic formatting option for the JSON patch result ([GitHub Issue #15](https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues/15))
- Add expand option to some text fields

### Fixed

- Fix date time converter ignores selected time zone ([GitHub Issue #11](https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues/11))

## 1.1.0 - 2023-08-14

### Added

- Add "Expand Editor" action to editors
- Add more details of a date in the date time converter
- Add Base64 secret key handling for the HMAC transformer ([GitHub Issue 5](https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues/5))

### Fixed

- Fix wrong naming of encoders/decodes input/output text areas ([GitHub Issue #4](https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues/4))
- Fix invalid date time format prevents usage of a standard format in the date time converter
- Fix individual date time format is not restored in the date time converter ([GitHub Issue #8](https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues/8))

### Changed

- Improve editor sizes in the JWT Encoder/Decoder
- Remove dependency on code from the JsonPath plugin ([GitHub Issue #9](https://github.com/marcelkliemannel/intellij-developer-tools-plugin/issues/9))

## 1.0.1 - 2023-05-29

### Fixed

- IntelliJ SDK compatibility

## 1.0.0 - 2023-05-29

### Added

- Initial release
