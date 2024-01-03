package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.generator

import com.github.f4b6a3.ulid.Ulid
import com.github.f4b6a3.ulid.UlidCreator
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindItem
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.DeveloperUiToolPresentation

class UlidGenerator(
  project: Project?,
  context: DeveloperUiToolContext,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : OneLineTextGenerator(
  context = context,
  configuration = configuration,
  parentDisposable = parentDisposable,
  project = project,
  initialGeneratedTextTitle = "Generated ULID"
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val generateMonotonicUlid by configuration.register("generateMonotonicUlid", false)
  private val ulidFormat = configuration.register("ulidFormat", UlidFormat.NONE)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  override fun Panel.buildConfigurationUi() {
    row {
      comboBox(UlidFormat.entries)
        .label("Format:")
        .bindItem(ulidFormat)
    }
  }

  override fun generate(): String {
    val ulid: Ulid = if (generateMonotonicUlid) UlidCreator.getUlid() else UlidCreator.getMonotonicUlid()
    return when (ulidFormat.get()) {
      UlidFormat.NONE -> ulid.toString()
      UlidFormat.TO_LOWERCASE -> ulid.toLowerCase()
      UlidFormat.UUID -> ulid.toUuid().toString()
      UlidFormat.RFC_4122_UUID -> ulid.toRfc4122().toUuid().toString()
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class UlidFormat(val title: String) {

    NONE("None"),
    TO_LOWERCASE("Lowercase"),
    UUID("UUID"),
    RFC_4122_UUID("RFC-4122 UUIDv4");

    override fun toString(): String = title
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<UlidGenerator> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "ULID",
      contentTitle = "ULID Generator"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> UlidGenerator) = { configuration ->
      UlidGenerator(project, context, configuration, parentDisposable)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //
}