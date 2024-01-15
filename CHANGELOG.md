# Changelog

## Unreleased

### Added
- Add time conversion tool
- Add 'What's New' overview to the main menu

### Changed

### Removed

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
