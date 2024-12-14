package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.editor.intention

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.lang.reflect.Modifier
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

class DescriptionTest {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  @ParameterizedTest
  @MethodSource("intentionActionSimpleClassNames")
  fun testIntentionActionDescriptionHtmlFileExist(intentionActionSimpleClassName: String) {
    val classLoader = Thread.currentThread().contextClassLoader
    val descriptionHtmlResourcePath = "intentionDescriptions/$intentionActionSimpleClassName/description.html"
    val packageResource = classLoader.getResource(descriptionHtmlResourcePath)
    assertThat(packageResource).describedAs(descriptionHtmlResourcePath).isNotNull()
  }

  @ParameterizedTest
  @MethodSource("intentionActionSimpleClassNames")
  fun testIntentionActionBeforeTemplateFileExist(intentionActionSimpleClassName: String) {
    val classLoader = Thread.currentThread().contextClassLoader
    val languageInfix = if (intentionActionSimpleClassName.contains("Kotlin")) "kt" else "java"
    val descriptionHtmlResourcePath = "intentionDescriptions/$intentionActionSimpleClassName/before.$languageInfix.template"
    val packageResource = classLoader.getResource(descriptionHtmlResourcePath)
    assertThat(packageResource).describedAs(descriptionHtmlResourcePath).isNotNull()
  }

  @ParameterizedTest
  @MethodSource("intentionActionSimpleClassNames")
  fun testIntentionActionAfterTemplateFileExist(intentionActionSimpleClassName: String) {
    val classLoader = Thread.currentThread().contextClassLoader
    val languageInfix = if (intentionActionSimpleClassName.contains("Kotlin")) "kt" else "java"
    val descriptionHtmlResourcePath = "intentionDescriptions/$intentionActionSimpleClassName/after.$languageInfix.template"
    val packageResource = classLoader.getResource(descriptionHtmlResourcePath)
    assertThat(packageResource).describedAs(descriptionHtmlResourcePath).isNotNull()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {
    
    private const val INTENTION_CLASSES_PACKAGE = "dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.editor.intention"

    @JvmStatic
    fun intentionActionSimpleClassNames(): List<Arguments> {
      val intentionActionSimpleClassName = Paths.get("src/main/kotlin").resolve(Paths.get(INTENTION_CLASSES_PACKAGE.replace(".", "/")))
        .let { println(it); it }
        .listDirectoryEntries()
        .filter { it.isRegularFile() && !it.fileName.toString().contains("$") }
        .map { Class.forName("${INTENTION_CLASSES_PACKAGE}.${it.nameWithoutExtension}") }
        .filter { !Modifier.isAbstract(it.modifiers) }
        .map { it.simpleName }
      assertThat(intentionActionSimpleClassName).hasSizeGreaterThan(1)
      return intentionActionSimpleClassName.map { arguments(it) }
    }
  }
}