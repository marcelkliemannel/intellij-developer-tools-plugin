package dev.turingcomplete.intellijdevelopertoolsplugin.common.testfixtures

import com.intellij.codeInsight.intention.IntentionAction
import dev.turingcomplete.intellijdevelopertoolsplugin.common.testfixtures.IoUtils.collectAllFiles
import java.io.File
import java.lang.reflect.Modifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest

abstract class IntentionDescriptionTest {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  abstract fun `test intention action description HTML file exists`(): List<DynamicNode>

  fun `do test intention action description HTML file exists`(): List<DynamicNode> =
    getAllIntentionActionClasses()
      .map { "/intentionDescriptions/${it.simpleName}/description.html" }
      .map {
        DynamicTest.dynamicTest(it) {
          val descriptionHtml = this::class.java.getResource(it)
          assertThat(descriptionHtml)
            .describedAs("Intention action description HTML file `${it}` exists")
            .isNotNull()
        }
      }

  abstract fun `test intention action before template file exists`(): List<DynamicNode>

  fun `do test intention action before template file exists`(): List<DynamicNode> =
    getAllIntentionActionClasses()
      .map { getDescriptionTemplateRelativePath(it.simpleName, "before") }
      .map { DynamicTest.dynamicTest(it) { testDescriptionTemplateExists(it) } }

  abstract fun `test intention action after template file exists`(): List<DynamicNode>

  fun `do test intention action after template file exists`(): List<DynamicNode> =
    getAllIntentionActionClasses()
      .map { getDescriptionTemplateRelativePath(it.simpleName, "after") }
      .map { DynamicTest.dynamicTest(it) { testDescriptionTemplateExists(it) } }

  // -- Private Methods ----------------------------------------------------- //

  private fun getDescriptionTemplateRelativePath(
    intentionActionSimpleClassName: String,
    templatePosition: String,
  ): String {
    val languageInfix = if (intentionActionSimpleClassName.contains("Kotlin")) "kt" else "java"
    return "/intentionDescriptions/$intentionActionSimpleClassName/$templatePosition.$languageInfix.template"
  }

  private fun testDescriptionTemplateExists(descriptionTemplateRelativePath: String) {
    val descriptionTemplate = this::class.java.getResource(descriptionTemplateRelativePath)
    assertThat(descriptionTemplate)
      .describedAs("Description template `$descriptionTemplateRelativePath` exists")
      .isNotNull()
  }

  private fun getAllIntentionActionClasses(): List<Class<*>> {
    val srcMainKotlinDir = File("src/main/kotlin")
    val intentionActions =
      srcMainKotlinDir
        .collectAllFiles()
        .filter { it.extension == "kt" }
        .map {
          // To fully qualified class name
          it.absolutePath
            .removePrefix("${srcMainKotlinDir.absolutePath}/")
            .removeSuffix(".kt")
            .replace('/', '.')
        }
        .map {
          // Fix nested class name
          it.replace('$', '.')
        }
        .map { Class.forName(it) }
        .filter { !Modifier.isAbstract(it.modifiers) }
        .filter { IntentionAction::class.java.isAssignableFrom(it) }
        .toList()
    assertThat(intentionActions).hasSizeGreaterThanOrEqualTo(1)
    return intentionActions
  }

  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
