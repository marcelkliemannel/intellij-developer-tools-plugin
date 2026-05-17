# AGENTS.md

This repository is an IntelliJ Platform plugin written in Kotlin. It provides a "Developer Tools" tool window, standalone dialog, editor popup actions, and intention actions for common developer utilities.

Use this file as the working map for future AI agents. It explains where the important code lives, how tools are registered, how state is persisted, and what commands to run before handing work back.

## Project Shape

- Root Gradle project: `intellij-developer-tools-plugin`
- Build system: Gradle Kotlin DSL with `org.jetbrains.intellij.platform` plugin.
- Language/runtime: Kotlin/JVM on Java 21.
- Minimum IDE build and platform are controlled from `gradle.properties`.
- The default `platform=idea` includes Java- and Kotlin-dependent modules. Non-IDEA platforms only include the platform/common modules.
- Plugin id is intentionally misspelled as `dev.turingcomplete.intellijdevelopertoolsplugins`; do not "fix" it.

Important root files:

- `build.gradle.kts`: root build, IntelliJ plugin packaging, signing, publishing, verification, common test/compiler config.
- `settings.gradle.kts`: module inclusion; conditionally includes `java-dependent` and `kotlin-dependent` when `platform == "idea"`.
- `gradle/libs.versions.toml`: dependency and plugin versions.
- `gradle.properties`: plugin metadata, platform version, bundled plugins, Kotlin stdlib opt-out.
- `src/main/resources/META-INF/plugin.xml`: main plugin descriptor and extension-point registry.
- `src/main/resources/META-INF/dev.turingcomplete.intellijdevelopertoolsplugins-withJava.xml`: optional Java plugin integrations.
- `src/main/resources/META-INF/dev.turingcomplete.intellijdevelopertoolsplugins-withKotlin.xml`: optional Kotlin plugin integrations.

## Modules

- `modules/common`: shared utilities, i18n bundle wrapper, plugin info, editor helpers, crypto/hash/text helpers, test fixtures.
- `modules/settings`: application and instance settings, settings UI abstractions, persistence and legacy migration for tool configurations.
- `modules/tools/editor`: editor popup actions and generic editor intentions for selected text.
- `modules/tools/ui`: the main UI tool framework, tool window/dialog implementation, all UI tools, shared Swing/UI DSL components, test fixtures.
- `modules/java-dependent`: Java PSI-specific editor actions and intentions. Only loaded through optional Java descriptor.
- `modules/kotlin-dependent`: Kotlin PSI-specific editor actions and intentions. Only loaded through optional Kotlin descriptor.
- `src/test`: root-level integration tests around plugin XML and cross-module tool behavior.

## Plugin Entry Points

The main descriptor is `src/main/resources/META-INF/plugin.xml`.

It declares:

- Required dependencies: platform, lang, json.
- Optional dependencies: `com.intellij.java` and `org.jetbrains.kotlin`, each with its own descriptor.
- Tool window: `MainToolWindowFactory`, id `Developer Tools`.
- Standalone dialog action: `OpenMainDialogAction`, added to `ToolsMenu`.
- Editor popup group: `DeveloperToolsActionGroup`, added to `EditorPopupMenu`.
- Main selected-text intention: `DataGeneratorIntentionAction`.
- Settings configurable: `GeneralSettingsConfigurable` with nested `JsonHandlingSettingsConfigurable`.
- Keymap extension: `ShowDeveloperUiToolKeymapExtension`.
- Custom extension points:
  - `developerUiTool`: registers tool factories.
  - `developerUiToolGroup`: registers menu groups.
  - `developerToolConfigurationEnumPropertyType`: registers enum types that can be persisted in tool settings.

The Java/Kotlin optional descriptors add language-specific intentions and editor action groups. Keep those registrations out of the main descriptor unless they do not depend on Java/Kotlin APIs.

## UI Tool Framework

Most new user-facing utilities should be implemented as a `DeveloperUiTool` in `modules/tools/ui`.

Core classes:

- `DeveloperUiTool`: base class for a tool UI. Subclasses implement `Panel.buildUi()`. It wraps UI DSL output, registers validation, supports activation/deactivation, reset, disposal, scroll wrapping, and optional `DataProvider` data.
- `DeveloperUiToolFactory<T>`: creates tools and returns `DeveloperUiToolPresentation`.
- `DeveloperUiToolFactoryEp`: extension-point bean for `developerUiTool`; reads `id`, `implementationClass`, `groupId`, `preferredSelected`, and `internalTool` from `plugin.xml`.
- `DeveloperUiToolContext`: carries the extension id and whether vertical layout should be preferred, mainly for tool-window layout.
- `DeveloperUiToolPresentation`: titles/descriptions used in menu, grouped menu, and content panel.
- `DeveloperUiToolGroup`: extension-point bean for grouping tools in the menu tree.

Registration flow:

1. `plugin.xml` registers a `<developerUiTool id="..." implementationClass="...$Factory"/>`.
2. `ToolsMenuTree` reads `DeveloperUiToolFactoryEp.EP_NAME`.
3. Each factory is instantiated and asked for a tool creator.
4. `DeveloperToolNode` restores or creates `DeveloperToolConfiguration` workbenches.
5. `DeveloperToolContentPanel` creates tabs, calls `DeveloperUiTool.createComponent()`, and drives `activated()`/`deactivated()`.

If a factory returns `null` from `getDeveloperUiToolCreator`, the tool is hidden for that context. This is useful for project-dependent tools.

## Dialog And Tool Window

Two instances expose the same tool framework with different persistence scopes:

- Dialog:
  - `OpenMainDialogAction` opens `MainDialogService`.
  - `MainDialog` uses `ContentPanelHandler` and `DeveloperToolsDialogSettings`.
  - Dialog inputs/configuration are application-level.

- Tool window:
  - `MainToolWindowFactory` creates async tool-window content.
  - `MainToolWindowService` stores and opens/selects tool-window content.
  - `ToolWindowContentPanelHandler` uses `DeveloperToolsToolWindowSettings`.
  - Tool-window inputs/configuration are project-level.

`ContentPanelHandler` is the shared coordinator. It owns the menu tree, switches group/tool panels, caches panels depending on `generalSettings.toolWindowUiCacheUi`, and implements `openTool()`/`showTool()`.

## Settings And Persistence

Application settings:

- `DeveloperToolsApplicationSettings` is an app service persisted to `developer-tools.xml`.
- It contains `GeneralSettings`, `InternalSettings`, and `JsonHandlingSettings`.
- Settings interfaces use annotations from `modules/settings/.../base`.

Tool instance settings:

- `DeveloperToolsInstanceSettings` is the base `PersistentStateComponent`.
- `DeveloperToolsDialogSettings` stores dialog state at app level.
- `DeveloperToolsToolWindowSettings` stores tool-window state at project level.
- Each UI tool workbench has a `DeveloperToolConfiguration`.
- Tool properties are registered by calling `configuration.register("key", defaultValue, propertyType, example)`.

Persistence details to respect:

- Only changed properties are persisted.
- `PropertyType.CONFIGURATION`, `INPUT`, and `SENSITIVE` are filtered by general settings.
- Sensitive values are not saved unless `saveSensitiveInputs` is enabled.
- Supported built-in persisted value types are in `DeveloperToolsInstanceSettings.builtInConfigurationPropertyTypes`.
- Enum configuration values must be registered in `plugin.xml` via `<developerToolConfigurationEnumPropertyType ...>`.
- If renaming property keys or moving enum classes, update `DeveloperToolsInstanceSettingsLegacy` and the legacy test resources under `src/test/resources/.../instancesettings`.

## Tool Implementation Patterns

Common base classes in `modules/tools/ui/src/main/kotlin/.../tool/ui`:

- `converter/base/Converter`: two-pane conversion base with source/target handlers, live conversion, validation, diff support, file/text handlers, and background conversion for heavy work.
- `converter/base/UndirectionalConverter`: one-way converter UI.
- `EncoderDecoder`: bidirectional converter base for encoding/decoding tools.
- `OneLineTextGenerator`: generator base for UUID/password/NanoID/etc.; handles generated value, copy, regenerate, and bulk generation.
- `MultiLineTextGenerator`: generator base for multi-line output.
- `AdvancedEditor`: shared editor component with input/output modes and diff support.
- `AsyncTaskExecutor`: debounced async UI/background task helper.
- `FileHandling`, `ErrorHolder`, validation helpers, regex helpers, and copy actions in `tool/ui/common`.

Tool categories and representative files:

- Converters: `tool/ui/converter`, including Base32/Base64, URL, ASCII, text format, date/time, CLI command, JWT, units.
- Transformers: `tool/ui/transformer`, including hashing/HMAC, text case, sorting/filtering, JSON Path, SQL and code formatting.
- Generators: `tool/ui/generator`, including UUID/ULID/NanoID/password/lorem/barcode.
- Other tools: `tool/ui/other`, including regex matcher, JSON schema validator, unarchiver, color picker, server certificates, HTTP server, notes, text diff/statistics, cron, ASCII art.

When adding a new UI tool:

1. Choose the closest base class instead of starting from `DeveloperUiTool` directly.
2. Put it in the matching package under `modules/tools/ui`.
3. Add a nested `Factory : DeveloperUiToolFactory<YourTool>`.
4. Register the factory in `plugin.xml` with a stable `id`.
5. Add enum property type registrations for any persisted enum defaults.
6. Use `configuration.register(...)` for all persisted controls. Pick stable keys.
7. Add bundle entries if the surrounding package uses message bundles.
8. Add or update tests when persistence, plugin XML registration, or conversion behavior changes.

## Editor Actions And Intentions

Generic selected-text actions live in `modules/tools/editor`.

- `DeveloperToolsActionGroup` is the root editor popup group.
- `EncodeDecodeActionGroup`, `EscapeUnescapeActionGroup`, `TextCaseConverterActionGroup`, `DataGeneratorActionGroup`, and `EditorTextStatisticAction` operate mostly on selected text.
- Shared operation lists are in `EncodersDecoders`, `EscapersUnescapers`, and `DataGenerators`.
- Editor mutations should go through `EditorUtils.executeWriteCommand`.
- Error dialogs are shown via IntelliJ APIs and failures are logged.

Generic intentions live in `modules/tools/editor/src/main/.../intention`.

Java/Kotlin language-specific variants live in:

- `modules/java-dependent/.../PsiJavaUtils.kt`
- `modules/java-dependent/.../tool/editor/action`
- `modules/java-dependent/.../tool/editor/intention`
- `modules/kotlin-dependent/.../PsiKotlinUtils.kt`
- `modules/kotlin-dependent/.../tool/editor/action`
- `modules/kotlin-dependent/.../tool/editor/intention`

Those modules are optional and must stay behind their optional plugin descriptors.

## Testing

Common commands:

```bash
./gradlew test
./gradlew check
./gradlew verifyPlugin

# For changes to the `plugin.xml`
./gradlew verifyPluginStructur
```

Do NOT run any Spotless code style checks. Code style is automatically checked and enforced during the CI.

CI runs:

```bash
./gradlew check --stacktrace
./gradlew verifyPlugin --stacktrace
```

Focused examples:

```bash
./gradlew :tools-ui:test
./gradlew :settings:test
./gradlew test --tests '*PluginXmlTest'
./gradlew test --tests '*DeveloperToolsInstanceSettingsTest'
```

Test infrastructure:

- `IdeaTest` sets up IntelliJ test application/project fixtures and Bouncy Castle.
- `PluginXmlTest` validates descriptor class/file references.
- `DeveloperUiToolsInstances` instantiates all registered UI tools from the extension point for integration tests.
- `DeveloperUiToolUnderTest` randomizes and resets registered tool properties.
- Intention description tests validate bundled intention descriptions/templates.

Some tests need IntelliJ test infrastructure and can be slow. On Linux/CI, run under Xvfb as the workflow does.

## Style And Conventions

- Kotlin formatting is ktfmt Google style through Spotless.
- `.editorconfig` uses 2-space Kotlin indentation and 100 character max line length.
- The code often uses section comments like `// -- Properties --`; keep them when editing nearby code.
- Prefer IntelliJ Platform APIs and the local helper classes over new abstractions.
- Use IntelliJ UI DSL builder patterns already present in neighboring tools.
- Register disposables with the passed `parentDisposable`.
- Keep long or blocking work off the EDT. Existing code uses pooled threads, `Task.Backgroundable`, `AsyncTaskExecutor`, and `Alarm`.
- Many UI components rely on `ValueProperty` or IntelliJ observable properties; bind controls rather than manually syncing when possible.
- Do not change plugin ids, extension ids, or persisted property keys without migration/test updates.

## External Processes And Downloads

The HTTP Server tool downloads and runs WireMock standalone:

- Implementation: `HttpServer.kt`.
- Process tracking: `ExternalSystemProcessRegistry.kt`.
- Download URL/version constants are at the bottom of `HttpServer.kt`.
- The tool supports built-in and custom server modes and registers/stops process handlers.

Be careful with lifecycle changes here: processes must be unregistered and stopped on disposal.

## Release And Verification Notes

- `publishPlugin` depends on `check` and requires `platform == "idea"`.
- Signing reads certificate/private key from `~/.jetbrains` and a Gradle property password.
- Marketplace token is read from Gradle properties.
- `buildSearchableOptions` is disabled.
- Plugin verification uses recommended IDEs plus additional IDEs from `pluginVerificationAdditionalIdes`.
- Changelog rendering comes from `CHANGELOG.md`; `tools-ui` also generates a bundled `changelog.html`.

## Agent Workflow

Before editing:

- Run `git status --short` and do not overwrite unrelated user changes.
- Use `rg`/`rg --files` for navigation.
- Read the closest existing implementation before adding a new pattern.

For UI tools:

- Start from the nearest tool in the same package.
- Wire configuration through `DeveloperToolConfiguration`.
- Register tool and enum property types in `plugin.xml`.
- Run at least `./gradlew :tools-ui:test` or the smallest relevant module test, plus `PluginXmlTest` if descriptors changed.

For settings persistence:

- Check `DeveloperToolsInstanceSettings`, `DeveloperToolConfiguration`, and legacy migration tests.
- Add enum registrations and migration entries as needed.
- Run `DeveloperToolsInstanceSettingsTest`.

For editor actions/intentions:

- Keep generic selected-text logic in `tools-editor`.
- Keep Java/Kotlin PSI logic in optional modules.
- Add/update intention descriptions under `resources/intentionDescriptions`.
- Run the relevant intention description tests.

Before final response:

- Run the narrowest meaningful Gradle tests.
- If descriptor or broad registration changed, run `PluginXmlTest`.
- If formatting changed, run `./gradlew spotlessApply` or `./gradlew spotlessCheck`.
- Mention any tests that could not be run.
