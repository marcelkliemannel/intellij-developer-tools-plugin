# AGENTS.md

This repository is an IntelliJ Platform plugin written in Kotlin/JVM. It provides a
"Developer Tools" tool window, standalone dialog, editor popup actions, and intention actions for
common developer utilities.

This file should stay concise and operational. Add rules here only when they help future agents make
better decisions without rediscovering repo-specific behavior. Prefer code references over long
architecture prose.

## Project Basics

- Build: Gradle Kotlin DSL with `org.jetbrains.intellij.platform`.
- Runtime: Java 21.
- Formatting: ktfmt Google style through Spotless; Kotlin indentation is 2 spaces, line limit is
  100.
- Default platform: `platform=idea`, which includes Java- and Kotlin-dependent modules. Non-IDEA
  platforms only include platform/common modules.
- Plugin id is intentionally misspelled as `dev.turingcomplete.intellijdevelopertoolsplugins`; do
  not change it.

Important files:

- `build.gradle.kts`: root build, packaging, signing, publishing, verification, common test config.
- `settings.gradle.kts`: module inclusion and IDEA-only optional modules.
- `gradle.properties`: plugin metadata, platform version, bundled plugins.
- `src/main/resources/META-INF/plugin.xml`: main descriptor and extension-point registry.
- `src/main/resources/META-INF/dev.turingcomplete.intellijdevelopertoolsplugins-withJava.xml`:
  optional Java integrations.
- `src/main/resources/META-INF/dev.turingcomplete.intellijdevelopertoolsplugins-withKotlin.xml`:
  optional Kotlin integrations.

## Modules

- `modules/common`: shared utilities, editor helpers, crypto/hash/text helpers, bundle wrapper, test
  fixtures.
- `modules/settings`: app settings, instance settings, persistence, settings UI, legacy migration.
- `modules/tools/ui`: main UI framework, tool window/dialog, all UI tools, shared Swing/UI DSL
  components, UI test fixtures.
- `modules/tools/editor`: generic editor popup actions and selected-text intentions.
- `modules/java-dependent`: Java PSI-specific actions/intentions. Keep behind the optional Java
  descriptor.
- `modules/kotlin-dependent`: Kotlin PSI-specific actions/intentions. Keep behind the optional
  Kotlin descriptor.
- `src/test`: root integration tests for plugin XML and cross-module persistence behavior.

## Start Here By Task

- New or changed UI tool: inspect the nearest tool under `modules/tools/ui/.../tool/ui`, then update
  `plugin.xml`.
- Tool menu, dialog, or tool-window behavior: start with `ContentPanelHandler`, `ToolsMenuTree`,
  `DeveloperToolNode`, `DeveloperToolContentPanel`, `MainDialog`, and `MainToolWindowFactory`.
- Tool persistence: start with `DeveloperToolConfiguration`, `DeveloperToolsInstanceSettings`,
  `DeveloperToolConfigurationEnumPropertyTypeEp`, and `DeveloperToolsInstanceSettingsLegacy`.
- App settings: start with `DeveloperToolsApplicationSettings`, `GeneralSettings`,
  `JsonHandlingSettings`, `InternalSettings`, and settings configurables.
- Editor popup actions: start with `DeveloperToolsActionGroup`, the specific `*ActionGroup`,
  `EncodersDecoders`, `EscapersUnescapers`, `DataGenerators`, and `EditorUtils`.
- Intentions: start in `modules/tools/editor/.../intention`; Java/Kotlin PSI variants stay in their
  optional modules.
- Opening/selecting a UI tool from another feature: start with `OpenDeveloperToolService`,
  `OpenDeveloperToolReference`, `OpenDeveloperToolHandler`, `MainDialogService`, and
  `MainToolWindowService`.

Use `rg`/`rg --files` first for navigation.

## Plugin Descriptor Rules

`plugin.xml` declares required dependencies, optional Java/Kotlin dependencies, the tool window,
dialog action, editor popup group, settings configurables, keymap extension, and custom extension
points.

Custom extension points:

- `developerUiTool`: registers UI tool factories.
- `developerUiToolGroup`: registers menu groups.
- `developerToolConfigurationEnumPropertyType`: registers enum classes that may be persisted in
  tool configuration.

Keep Java/Kotlin PSI registrations out of the main descriptor unless they do not depend on those
APIs. If descriptors change, run `PluginXmlTest` or `verifyPluginStructure`.

## UI Tool Flow

Most user-facing utilities are `DeveloperUiTool`s in `modules/tools/ui`.

Runtime flow:

1. `plugin.xml` registers `<developerUiTool id="..." implementationClass="...$Factory"/>`.
2. `ToolsMenuTree.createTreeNodes()` reads `DeveloperUiToolFactoryEp.EP_NAME`.
3. Each factory receives `DeveloperUiToolContext(id, prioritizeVerticalLayout)` and may return a
   creator from `getDeveloperUiToolCreator(...)`.
4. Returning `null` hides the tool in that context.
5. `DeveloperToolNode` restores or creates per-tool workbenches through
   `DeveloperToolConfiguration`.
6. `DeveloperToolContentPanel` creates tabs, calls `DeveloperUiTool.createComponent()`, and drives
   `activated()`/`deactivated()`.

When adding a UI tool:

- Use the closest base class instead of starting from `DeveloperUiTool` directly.
- Add a nested `Factory : DeveloperUiToolFactory<YourTool>`.
- Register the factory in `plugin.xml` with a stable id.
- Register persisted controls with `configuration.register(...)` using stable keys.
- Register enum property types in `plugin.xml` when persisted defaults are enum values.
- Add bundle entries if neighboring tools use bundles.
- Add tests when conversion behavior, persistence, descriptors, or editor integration changes.

Useful bases:

- `converter/base/Converter`: two-pane conversion with live conversion, validation, diff support,
  text/file handlers, and background conversion.
- `converter/base/BidirectionalConverter`, `converter/base/UndirectionalConverter`,
  `converter/EncoderDecoder`: converter variants.
- `generator/OneLineTextGenerator` and `generator/MultiLineTextGenerator`: generator UIs.
- `common/AdvancedEditor`, `common/AsyncTaskExecutor`, `ConversionSideHandler`,
  `InputOutputHandler`: shared UI/conversion infrastructure.

Lifecycle rules:

- Build UI in `Panel.buildUi()`.
- Use `afterBuildUi()` for initial sync that needs built components.
- Register listeners/work in `activated()` and remove them in `deactivated()` or `doDispose()`.
- Register disposables with `parentDisposable`.
- Keep long or blocking work off EDT; use existing pooled/background helpers.
- Prefer `ValueProperty` or IntelliJ observable bindings over manual component syncing.

## Dialog, Tool Window, And Open-Tool Flow

Dialog:

- `OpenMainDialogAction` calls app-level `MainDialogService`.
- `MainDialog` uses `ContentPanelHandler` with `DeveloperToolsDialogSettings`.
- Dialog state is application-level.

Tool window:

- `MainToolWindowFactory` installs a loader, creates `DeveloperToolsToolWindowSettings` on a pooled
  thread, then installs content on EDT.
- `ToolWindowContentPanelHandler` uses `groupNodeSelectionEnabled = false` and
  `prioritizeVerticalLayout = true`.
- Tool-window state is project-level.

Shared behavior:

- `ContentPanelHandler` owns `ToolsMenuTree`, selected node, and cached panels.
- `ToolsMenuTree` persists `lastSelectedContentNodeId` and expanded group ids.
- Selecting a `DeveloperToolNode` shows a `DeveloperToolContentPanel`; tab selection activates and
  deactivates individual tool instances.

Open-tool integration:

- Call project service `OpenDeveloperToolService`.
- It chooses tool window vs dialog from general action-handling settings.
- `MainToolWindowService` defers one open/show task if tool-window content is not ready.
- `ContentPanelHandler.openTool()` selects the registered tool id.
- `DeveloperToolContentPanel.openTool()` calls `OpenDeveloperToolHandler<T>.applyOpenDeveloperToolContext`.

For a new openable tool, define an `OpenDeveloperToolContext`, implement
`OpenDeveloperToolHandler<T>`, and expose `OpenDeveloperToolReference.of(ID, Context::class)`.
Examples: `TextStatistic`, `Unarchiver`.

## Settings And Persistence

Application settings:

- `DeveloperToolsApplicationSettings` persists to `developer-tools.xml`.
- It lazily creates `GeneralSettings`, `InternalSettings`, and `JsonHandlingSettings`.
- Only modified app settings are written.

Tool instance settings:

- `DeveloperToolsDialogSettings`: app-level dialog tool state.
- `DeveloperToolsToolWindowSettings`: project-level tool-window state.
- `DeveloperToolsInstanceSettings.getState()` persists only changed tool properties.
- `PropertyType.CONFIGURATION`, `INPUT`, and `SENSITIVE` are filtered by the corresponding general
  save settings.
- Sensitive values are saved only when `saveSensitiveInputs` is enabled.

Persistable property types:

- Built-ins live in `DeveloperToolsInstanceSettings.builtInConfigurationPropertyTypes`.
- Enum values require `<developerToolConfigurationEnumPropertyType ...>` in `plugin.xml`.
- Moving/renaming enum classes may require `legacyId`.

When renaming persisted property keys or moving persisted enum types:

- Update `DeveloperToolsInstanceSettingsLegacy`.
- Update legacy resources under `src/test/resources/.../instancesettings` if compatibility changes.
- Run `DeveloperToolsInstanceSettingsTest`.

## Editor Actions And Intentions

- Generic selected-text logic belongs in `modules/tools/editor`.
- Java/Kotlin PSI-specific logic belongs in optional modules only.
- Editor mutations should go through `EditorUtils.executeWriteCommand`.
- Generic action operation lists live in `EncodersDecoders`, `EscapersUnescapers`, and
  `DataGenerators`.
- Add/update `resources/intentionDescriptions/<ActionName>/` for intentions.

## External Processes

The HTTP Server tool downloads and runs WireMock standalone:

- Implementation: `HttpServer.kt`.
- Process tracking: `ExternalSystemProcessRegistry.kt`.
- Download URL/version constants are at the bottom of `HttpServer.kt`.

Lifecycle changes here are risky: processes must be unregistered and stopped on disposal. There is a
focused `ExternalSystemProcessRegistryTest`.

## Commands

Common:

```bash
./gradlew test
./gradlew check
./gradlew verifyPlugin
```

Focused:

```bash
./gradlew :tools-ui:test
./gradlew :tools-editor:test
./gradlew :settings:test
./gradlew test --tests '*PluginXmlTest'
./gradlew test --tests '*DeveloperToolsInstanceSettingsTest'
./gradlew :tools-ui:test --tests '*ExternalSystemProcessRegistryTest'
```

Descriptor changes:

```bash
./gradlew verifyPluginStructure
```

CI uses:

```bash
./gradlew check --stacktrace
./gradlew verifyPlugin --stacktrace
```

Some IntelliJ tests are slow and may need Xvfb on Linux/CI.

## Agent Workflow

Before editing:

- Run `git status --short`.
- Do not overwrite unrelated user changes.
- Read the closest existing implementation before adding a pattern.
- Inspect `plugin.xml` early for descriptor-driven behavior.

Before final response:

- Run the narrowest meaningful Gradle test for the touched area.
- Run `PluginXmlTest` or `verifyPluginStructure` after descriptor changes.
- Run persistence tests after settings/property changes.
- Mention tests that could not be run.
