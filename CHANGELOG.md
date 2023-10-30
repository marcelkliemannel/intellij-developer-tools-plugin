# Changelog

## Unreleased

### Added

### Changed

### Removed

### Fixed

## [2.0.1] - 2023-10-30

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
