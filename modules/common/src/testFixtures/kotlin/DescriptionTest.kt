import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import java.lang.reflect.Modifier
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension

abstract class DescriptionTest {
  // -- Properties -------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  abstract fun testIntentionActionDescriptionHtmlFileExist(intentionActionSimpleClassName: String)

  fun doTestIntentionActionDescriptionHtmlFileExist(intentionActionSimpleClassName: String) {
    val classLoader = Thread.currentThread().contextClassLoader
    val descriptionHtmlResourcePath = "intentionDescriptions/$intentionActionSimpleClassName/description.html"
    val packageResource = classLoader.getResource(descriptionHtmlResourcePath)
    assertThat(packageResource).describedAs(descriptionHtmlResourcePath).isNotNull()
  }

  abstract fun testIntentionActionBeforeTemplateFileExist(intentionActionSimpleClassName: String)

  fun doTestIntentionActionBeforeTemplateFileExist(intentionActionSimpleClassName: String) {
    val classLoader = Thread.currentThread().contextClassLoader
    val languageInfix = if (intentionActionSimpleClassName.contains("Kotlin")) "kt" else "java"
    val descriptionHtmlResourcePath = "intentionDescriptions/$intentionActionSimpleClassName/before.$languageInfix.template"
    val packageResource = classLoader.getResource(descriptionHtmlResourcePath)
    assertThat(packageResource).describedAs(descriptionHtmlResourcePath).isNotNull()
  }

  abstract fun testIntentionActionAfterTemplateFileExist(intentionActionSimpleClassName: String)

  fun doTestIntentionActionAfterTemplateFileExist(intentionActionSimpleClassName: String) {
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

    fun intentionActionSimpleClassNames(intentionClassesPackage: String): List<Arguments> {
      val intentionActionSimpleClassName = Paths.get("src/main/kotlin").resolve(Paths.get(intentionClassesPackage.replace(".", "/")))
        .let { println(it); it }
        .listDirectoryEntries()
        .filter { it.isRegularFile() && !it.fileName.toString().contains("$") }
        .map { Class.forName("$intentionClassesPackage.${it.nameWithoutExtension}") }
        .filter { !Modifier.isAbstract(it.modifiers) }
        .map { it.simpleName }
      assertThat(intentionActionSimpleClassName).hasSizeGreaterThanOrEqualTo(1)
      return intentionActionSimpleClassName.map { arguments(it) }
    }
  }
}