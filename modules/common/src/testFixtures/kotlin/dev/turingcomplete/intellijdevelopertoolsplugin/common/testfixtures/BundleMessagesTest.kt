package dev.turingcomplete.intellijdevelopertoolsplugin.common.testfixtures

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.ui.UIBundle
import dev.turingcomplete.intellijdevelopertoolsplugin.common.extension
import dev.turingcomplete.intellijdevelopertoolsplugin.common.testfixtures.IoUtils.collectAllFiles
import java.io.File
import java.nio.file.Path
import java.nio.file.Paths
import java.util.Properties
import kotlin.io.path.nameWithoutExtension
import kotlin.reflect.KClass
import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest

abstract class BundleMessagesTest : IdeaTest() {
  // -- Properties ---------------------------------------------------------- //

  private val psiManager: PsiManager by lazy { PsiManager.getInstance(fixture.project) }

  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  abstract fun `test that all additional languages are containing the same message keys and parameter counts`():
    List<DynamicNode>

  protected fun `do test that all additional languages are containing the same message keys and parameter counts`():
    List<DynamicNode> =
    messagesBundles.map {
      val additionalLanguageToMessages: Map<String, Map<String, String>> =
        it.languageToMessages.filter { it.key != REFERENCE_LANGUAGE_KEY }
      dynamicContainer(
        it.bundleName,
        additionalLanguageToMessages.map { (additionalLanguageKey, additionalLanguageMessages) ->
          val testSameMessageKeys =
            dynamicTest("Same message keys") {
              assertThat(it.referenceLanguageMessages().keys)
                .containsExactlyInAnyOrderElementsOf(additionalLanguageMessages.keys)
            }
          val sameParameterCounts =
            dynamicContainer(
              "Language messages have same parameter counts",
              additionalLanguageMessages.map { (key, _) ->
                dynamicTest(key) {
                  assertThat(countUniqueParameters(additionalLanguageMessages[key]!!))
                    .isEqualTo(countUniqueParameters(it.referenceLanguageMessages()[key]!!))
                }
              },
            )
          dynamicContainer(additionalLanguageKey, listOf(testSameMessageKeys, sameParameterCounts))
        },
      )
    }

  abstract fun `test that all message(messageKey, params) calls are referencing to an existing message key and are using the correct parameters count`():
    List<DynamicNode>

  protected fun `do test that all message(messageKey, params) calls are referencing to an existing message key and are using the correct parameters count`():
    List<DynamicNode> =
    collectAllMessageBundleUsages()
      .groupBy { it.className }
      .map { (className, allMessageBundleUsagesInClass) ->
        dynamicContainer(
          className,
          allMessageBundleUsagesInClass
            .filter { !internalBundleNames.contains(it.bundleName) }
            .groupBy { it.bundleName }
            .map { (bundleName, allMessageBundleUsages) ->
              val messagesBundleReferenceLanguageMessages =
                getMessagesBundle(bundleName).referenceLanguageMessages()

              val bundleExistsTest =
                dynamicTest("Bundle `$bundleName` exists") {
                  assertThat(messagesBundles).anyMatch { it.bundleName == bundleName }
                }

              val messageKeysAndParametersCountTests =
                allMessageBundleUsages.map { messageBundleUsage ->
                  dynamicTest(messageBundleUsage.displayableText) {
                    assertThat(messagesBundleReferenceLanguageMessages)
                      .containsKey(messageBundleUsage.messageKey)
                    assertThat(messageBundleUsage.parametersCount)
                      .isEqualTo(
                        countUniqueParameters(
                          messagesBundleReferenceLanguageMessages[messageBundleUsage.messageKey]!!
                        )
                      )
                  }
                }

              dynamicContainer(
                bundleName,
                listOf(bundleExistsTest) + messageKeysAndParametersCountTests,
              )
            },
        )
      }

  abstract fun `test that all keys in the messages bundle are used`(): List<DynamicNode>

  protected fun `do test that all keys in the messages bundle are used`(): List<DynamicNode> {
    val usedMessageKeys = collectAllMessageBundleUsages().map { it.messageKey }
    return messagesBundles.map { messageBundle ->
      dynamicContainer(
        messageBundle.bundleName,
        messageBundle.referenceLanguageMessages().keys.map {
          dynamicTest(it) {
            assertThat(usedMessageKeys).describedAs("No unused messages bundle keys").contains(it)
          }
        },
      )
    }
  }

  /**
   * This base method only collects all calls to `$BundleName.message(messageKey, ...params)`, if
   * the `messageKey` is a [String]. Overridden methods may add additional [MessagesBundleUsage].
   */
  protected open fun collectAllMessageBundleUsages(): List<MessagesBundleUsage> =
    kotlinSourceFilesToScanForMessagesBundleUsages().flatMap { kotlinSourceFile ->
      kotlinSourceFile.toKtFile().traverseElement(KtDotQualifiedExpression::class).mapNotNull {
        ktDotQualifiedExpression ->
        val receiver = ktDotQualifiedExpression.receiverExpression
        if (receiver !is KtNameReferenceExpression) return@mapNotNull null

        val referenceName = receiver.getReferencedName()
        val callExpression = ktDotQualifiedExpression.selectorExpression as? KtCallExpression
        val methodName = callExpression?.calleeExpression?.text
        if (methodName != "message") {
          return@mapNotNull null
        }

        val parameters = callExpression.valueArguments
        val firstArgExpression =
          parameters.getOrNull(0)?.getArgumentExpression() as? KtStringTemplateExpression
            ?: return@mapNotNull null

        // Reject if the string contains interpolation
        if (firstArgExpression.hasInterpolation()) {
          return@mapNotNull null
        }

        val messageKey = firstArgExpression.entries.joinToString("") { it.text }

        return@mapNotNull MessagesBundleUsage(
          className = kotlinSourceFile.nameWithoutExtension,
          bundleName = referenceName,
          messageKey = messageKey,
          parametersCount = parameters.size - 1,
        )
      }
    }

  protected fun Path.toKtFile(): KtFile {
    val virtualFile = LocalFileSystem.getInstance().findFileByNioFile(this)!!
    return ApplicationManager.getApplication().runReadAction<PsiFile?> {
      psiManager.findFile(virtualFile)
    } as KtFile
  }

  protected fun getMessagesBundle(bundleName: String) =
    messagesBundles.firstOrNull { it.bundleName == bundleName }
      ?: error("Bundle `$bundleName` not found")

  protected open fun kotlinSourceFilesToScanForMessagesBundleUsages(): List<Path> {
    val classFiles =
      Paths.get("src/main/kotlin").collectAllFiles().filter {
        setOf("kt", "kts").contains(it.extension())
      }
    assertThat(classFiles).hasSizeGreaterThanOrEqualTo(1)
    return classFiles
  }

  protected fun <T : KtElement> KtElement.traverseElement(type: KClass<T>): List<T> =
    PsiTreeUtil.collectElementsOfType(this, type.java).toList()

  // -- Private Methods ----------------------------------------------------- //

  private fun countUniqueParameters(message: String): Int =
    Regex("(?<!\\$)\\{(\\d+)}").findAll(message).map { it.groupValues[1].toInt() }.toSet().size

  // -- Inner Type ---------------------------------------------------------- //

  data class MessagesBundle(
    val bundleName: String,
    val languageToMessages: MutableMap<String, Map<String, String>> =
      mutableMapOf<String, Map<String, String>>(),
  ) {

    fun referenceLanguageMessages(): Map<String, String> =
      languageToMessages[REFERENCE_LANGUAGE_KEY]
        ?: error("Bundle `$bundleName` is missing the reference language")
  }

  // -- Inner Type ---------------------------------------------------------- //

  data class MessagesBundleUsage(
    val className: String,
    val bundleName: String,
    val messageKey: String,
    val parametersCount: Int,
    val displayableText: String = messageKey,
  )

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    lateinit var messagesBundles: List<MessagesBundle>
      private set

    lateinit var kotlinSourceFiles: List<Path>
      private set

    private const val REFERENCE_LANGUAGE_KEY = "en"

    private val internalBundleNames = setOf(UIBundle::class.simpleName!!)

    @BeforeAll
    @JvmStatic
    fun setUp() {
      messagesBundles = collectAllMessageBundles(File("src/main/resources/message"))
      assertThat(messagesBundles).hasSizeGreaterThanOrEqualTo(1)

      kotlinSourceFiles =
        Paths.get("src/main/kotlin").collectAllFiles().filter { it.extension() == "kt" }
      assertThat(kotlinSourceFiles).hasSizeGreaterThanOrEqualTo(1)
    }

    private fun collectAllMessageBundles(messagesBundlesDir: File): List<MessagesBundle> {
      check(messagesBundlesDir.isDirectory)

      val result = mutableMapOf<String, MessagesBundle>()

      messagesBundlesDir
        .listFiles { file -> file.extension == "properties" }
        .forEach { file ->
          val filename = file.nameWithoutExtension
          val parts = filename.split("_")

          val bundleName = if (parts.size > 1) parts[0] else filename
          val languageKey = if (parts.size > 1) parts[1] else REFERENCE_LANGUAGE_KEY
          val properties = Properties().apply { file.inputStream().use { load(it) } }

          val messagesBundle = result.getOrPut(bundleName) { MessagesBundle(bundleName) }
          messagesBundle.languageToMessages.put(
            languageKey,
            properties.stringPropertyNames().associate { it to properties.getProperty(it) },
          )
        }

      return result.values.toList()
    }
  }
}
