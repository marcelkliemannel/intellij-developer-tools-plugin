package dev.turingcomplete.intellijdevelopertoolsplugin._internal.message

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.junit5.RunInEdt
import com.intellij.testFramework.junit5.RunMethodInEdt
import com.intellij.testFramework.junit5.TestApplication
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.util.*

@RunInEdt(allMethods = false)
@TestApplication
class BundlesTest {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val fixture = IdeaTestFixtureFactory.getFixtureFactory().createFixtureBuilder("test").fixture
  private val disposable = Disposer.newDisposable()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  @BeforeEach
  fun beforeEach() {
    fixture.setUp()
  }

  @AfterEach
  @RunMethodInEdt(writeIntent = RunMethodInEdt.WriteIntentMode.True)
  fun afterEach() {
    Disposer.dispose(disposable)
    fixture.tearDown()
  }

  @ParameterizedTest(name="{0}")
  @MethodSource("testVectors_allKotlinSourceFiles")
  @RunMethodInEdt(writeIntent = RunMethodInEdt.WriteIntentMode.True)
  fun testMessageKeyUsages(
    @Suppress("unused") kotlinSourceFileName: String,
    kotlinSourceFile: File
  ) {
    val psiManager = PsiManager.getInstance(fixture.project)
    val virtualFile = LocalFileSystem.getInstance().findFileByPath(kotlinSourceFile.absolutePath)!!
    val ktFile = ApplicationManager.getApplication().runReadAction<PsiFile?> { psiManager.findFile(virtualFile) } as KtFile
    ktFile.allKtDotQualifiedExpression().forEach { ktDotQualifiedExpression ->
      val receiver = ktDotQualifiedExpression.receiverExpression
      if (receiver !is KtNameReferenceExpression) {
        return@forEach
      }

      val className = receiver.getReferencedName()
      val callExpression = ktDotQualifiedExpression.selectorExpression as? KtCallExpression
      val methodName = callExpression?.calleeExpression?.text
      if (methodName != "message") {
        return@forEach
      }

      val parameters = callExpression.valueArguments
      val messageKey = parameters[0].text.replace("\"", "")
      println("Check message: $messageKey")
      val message = findMessage(className, messageKey)
      assertThat(message).describedAs("Message key: $messageKey").isNotNull
      assertThat(parameters.size - 1).describedAs("Message parameters of key: $messageKey").isEqualTo(countUniqueParameters(message!!))
    }
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("testVectors_allMessagesBundles")
  fun checkAllMessagesBundlesContainTheSameKeys(
    @Suppress("unused") bundleName: String,
    messagesForLanguageKey: Map<String, Map<String, String>>
  ) {
      assertThat(messagesForLanguageKey.keys)
        .describedAs("Required language keys")
        .containsExactlyInAnyOrderElementsOf(setOf(REFERENCE_LANGUAGE_KEY, "de"))

    val referenceMessages = messagesForLanguageKey[REFERENCE_LANGUAGE_KEY]!!
    messagesForLanguageKey.forEach { (languageKey, messages) ->
      if (languageKey == REFERENCE_LANGUAGE_KEY) {
        return@forEach
      }

      // Check same keys
      assertThat(referenceMessages.keys)
        .describedAs("Same message keys")
        .containsExactlyInAnyOrderElementsOf(referenceMessages.keys)

      // Check messages contain the same parameter counts
      referenceMessages.keys.forEach { referenceMessageKey ->
        assertThat(countUniqueParameters(messages[referenceMessageKey]!!))
          .isEqualTo(countUniqueParameters(referenceMessages[referenceMessageKey]!!))
      }
    }
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun countUniqueParameters(message: String): Int =
    Regex("(?<!\\\$)\\{(\\d+)}").findAll(message).map { it.groupValues[1].toInt() }.toSet().size

  private fun KtFile.allKtDotQualifiedExpression(): List<KtDotQualifiedExpression> {
    val methodCalls = mutableListOf<KtDotQualifiedExpression>()

    fun traverseElement(element: KtElement) {
      if (element is KtDotQualifiedExpression) {
        methodCalls.add(element)
      }
      else {
        element.children
          .filter { it is KtElement }
          .forEach { traverseElement(it as KtElement) }
      }
    }

    traverseElement(this)

    return methodCalls
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private lateinit var messagesBundles: Map<String, Map<String, Map<String, String>>>

    private const val REFERENCE_LANGUAGE_KEY = "en"

    @BeforeAll
    @JvmStatic
    fun setUp() {
      messagesBundles = loadMessageBundles(File("src/main/resources/messages"))
      assertThat(messagesBundles.size).isGreaterThan(1)
    }

    fun findMessage(bundleName: String, key: String, languageKey: String = REFERENCE_LANGUAGE_KEY): String? =
      messagesBundles[bundleName]!![languageKey]!![key]

    @JvmStatic
    fun testVectors_allKotlinSourceFiles(): Collection<Arguments> =
      getAllFiles(File("src/main/kotlin"))
        .filter { it.extension == "kt" }
        .map { Arguments.of(it.name, it) }

    @JvmStatic
    fun testVectors_allMessagesBundles(): Collection<Arguments> =
      messagesBundles.map { Arguments.of(it.key, it.value) }

    private fun loadMessageBundles(messagesBundlesDir: File): Map<String, Map<String, Map<String, String>>> {
      check(messagesBundlesDir.isDirectory)

      val result = mutableMapOf<String, MutableMap<String, Map<String, String>>>()

      messagesBundlesDir
        .listFiles { file -> file.extension == "properties" }
        .forEach { file ->
          val filename = file.nameWithoutExtension
          val parts = filename.split("_")

          val bundleName = if (parts.size > 1) parts[0] else filename
          val languageKey = if (parts.size > 1) parts[1] else REFERENCE_LANGUAGE_KEY

          val properties = Properties().apply {
            file.inputStream().use { load(it) }
          }

          val messagesBundle = result.getOrPut(bundleName) { mutableMapOf() }
          messagesBundle.put(languageKey, properties.stringPropertyNames().associate { it to properties.getProperty(it) })
        }

      return result
    }

    private fun getAllFiles(directory: File): List<File> {
      check(directory.isDirectory)

      val files = mutableListOf<File>()

      directory.listFiles()?.forEach { file ->
        if (file.isDirectory) {
          files.addAll(getAllFiles(file))
        }
        else {
          files.add(file)
        }
      }

      return files
    }
  }
}