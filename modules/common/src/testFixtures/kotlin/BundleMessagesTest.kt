
import IoUtils.collectAllFiles
import dev.turingcomplete.intellijdevelopertoolsplugin.common.extension
import dev.turingcomplete.intellijdevelopertoolsplugin.common.nameWithoutExtension
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DynamicContainer.dynamicContainer
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*

abstract class BundleMessagesTest : IdeaTest() {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  abstract fun `test that all additional languages are containing the same message keys and parameter counts`(): List<DynamicNode>

  protected fun `do test that all additional languages are containing the same message keys and parameter counts`(): List<DynamicNode> =
    messagesBundles.map {
      val additionalLanguageToMessages: Map<String, Map<String, String>> = it.languageToMessages.filter { it.key != REFERENCE_LANGUAGE_KEY }
      dynamicContainer(it.bundleName, it.referenceLanguageMessages().map { (referenceLanguageMessageKey, referenceLanguageMessageValue) ->
        dynamicContainer(referenceLanguageMessageKey, additionalLanguageToMessages.map { (additionalLanguageKey, additionalLanguageMessages) ->
          dynamicTest(additionalLanguageKey) {
            assertThat(additionalLanguageMessages).containsKey(referenceLanguageMessageKey)
            assertThat(countUniqueParameters(additionalLanguageMessages[referenceLanguageMessageKey]!!))
              .isEqualTo(countUniqueParameters(referenceLanguageMessageValue))
          }
        })
      })
    }

  abstract fun `test that all message(messageKey, params) calls are referencing to an existing message key and are using the correct parameters count`(): List<DynamicNode>

  protected fun `do test that all message(messageKey, params) calls are referencing to an existing message key and are using the correct parameters count`(): List<DynamicNode> =
    collectAllMessageBundleUsages().groupBy { it.className }.map { (className, allMessageBundleUsagesInClass) ->
      dynamicContainer(className, allMessageBundleUsagesInClass.groupBy { it.bundleName }.map { (bundleName, allMessageBundleUsages) ->
        val messagesBundleReferenceLanguageMessages = getMessagesBundle(bundleName).referenceLanguageMessages()

        val bundleExistsTest = dynamicTest("Bundle `$bundleName` exists") { assertThat(messagesBundles).anyMatch { it.bundleName == bundleName } }

        val messageKeysAndParametersCountTests = allMessageBundleUsages.map { messageBundleUsage ->
          dynamicTest(messageBundleUsage.displayableText) {
            assertThat(messagesBundleReferenceLanguageMessages).containsKey(messageBundleUsage.messageKey)
            assertThat(messageBundleUsage.parametersCount)
              .isEqualTo(countUniqueParameters(messagesBundleReferenceLanguageMessages[messageBundleUsage.messageKey]!!))
          }
        }

        dynamicContainer(bundleName, listOf(bundleExistsTest) + messageKeysAndParametersCountTests)
      })
    }

  abstract fun `test that all keys in the messages bundle are used`(): List<DynamicNode>

  protected fun `do test that all keys in the messages bundle are used`(): List<DynamicNode> {
    val usedMessageKeys = collectAllMessageBundleUsages().map { it.messageKey }
    return messagesBundles.map { messageBundle ->
      dynamicContainer(messageBundle.bundleName, messageBundle.referenceLanguageMessages().keys.map {
        dynamicTest(it) { assertThat(usedMessageKeys).contains(it) }
      })
    }
  }

  /**
   * This base method only collects all calls to `$BundleName.message(messageKey, ...params)`,
   * if the `messageKey` is a [String]. Overridden methods may add additional
   * [MessagesBundleUsage].
   */
  protected open fun collectAllMessageBundleUsages(): List<MessagesBundleUsage> =
    moduleKotlinClassesKotlinMain.flatMap { classFile ->
      val usages = mutableListOf<MessagesBundleUsage>()

      classFile.readClass()
        .accept(MessageCallsClassVisitor(classFile.nameWithoutExtension(), usages), 0)

      return usages
    }

  protected fun Path.readClass() = ClassReader(Files.readAllBytes(this))

  protected fun getMessagesBundle(bundleName: String) =
    messagesBundles.firstOrNull { it.bundleName == bundleName }
      ?: error("Bundle `$bundleName` not found")

  // -- Private Methods ----------------------------------------------------- //

  private fun countUniqueParameters(message: String): Int =
    Regex("(?<!\\$)\\{(\\d+)}").findAll(message).map { it.groupValues[1].toInt() }.toSet().size

  // -- Inner Type ---------------------------------------------------------- //

  data class MessagesBundle(
    val bundleName: String,
    val languageToMessages: MutableMap<String, Map<String, String>> = mutableMapOf<String, Map<String, String>>()
  ) {

    fun referenceLanguageMessages(): Map<String, String> = languageToMessages[REFERENCE_LANGUAGE_KEY]
      ?: error("Bundle `$bundleName` is missing the reference language")
  }

  // -- Inner Type ---------------------------------------------------------- //

  data class MessagesBundleUsage(
    val className: String,
    val bundleName: String,
    val messageKey: String,
    val parametersCount: Int,

    val displayableText: String = messageKey
  )

  // -- Inner Type ---------------------------------------------------------- //

  class MessageCallsMethodVisitor(
    private val className: String,
    methodVisitor: MethodVisitor,
    private val usages: MutableList<MessagesBundleUsage>
  ) : MethodVisitor(Opcodes.ASM9, methodVisitor) {

    private var lastStringLdcInsn: String? = null
    private var lastIconstInsn: Int? = null

    override fun visitInsn(opcode: Int) {
      lastIconstInsn = when(opcode) {
        Opcodes.ICONST_0 -> 0
        Opcodes.ICONST_1 -> 1
        Opcodes.ICONST_2 -> 2
        Opcodes.ICONST_3 -> 3
        Opcodes.ICONST_4 -> 4
        Opcodes.ICONST_5 -> 4
        else -> lastIconstInsn
      }

      super.visitInsn(opcode)
    }

    override fun visitLdcInsn(value: Any?) {
      if (value is String) {
        lastStringLdcInsn = value
      }

      super.visitLdcInsn(value)
    }

    override fun visitMethodInsn(
      opcode: Int,
      owner: String,
      name: String,
      descriptor: String,
      isInterface: Boolean
    ) {
      if (name == "message") {
        val type = Type.getMethodType(descriptor)
        val argumentTypes = type.argumentTypes
        if (argumentTypes.isNotEmpty() && argumentTypes[0] == Type.getType(String::class.java)) {
          usages.add(
            MessagesBundleUsage(
              className = className,
              bundleName = owner.substringAfterLast("/"),
              messageKey = lastStringLdcInsn ?: error("No latest LDC instruction captured"),
              parametersCount = lastIconstInsn ?: error("No last ICONST instruction captured")
            )
          )
        }
      }

      super.visitMethodInsn(opcode, owner, name, descriptor, isInterface)
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  class MessageCallsClassVisitor(
    private val fileName: String,
    private val usages: MutableList<MessagesBundleUsage>
  ) : ClassVisitor(Opcodes.ASM9, ClassWriter(0)) {
    override fun visitMethod(
      access: Int,
      name: String,
      descriptor: String,
      signature: String?,
      exceptions: Array<out String>?
    ): MethodVisitor {
      val methodVisitor = super.visitMethod(access, name, descriptor, signature, exceptions)
      return MessageCallsMethodVisitor(fileName, methodVisitor, usages)
    }
  }

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    lateinit var messagesBundles: List<MessagesBundle>
      private set
    lateinit var kotlinSourceFiles: List<Path>
      private set
    lateinit var moduleKotlinClassesKotlinMain: List<Path>
      private set

    private const val REFERENCE_LANGUAGE_KEY = "en"

    @BeforeAll
    @JvmStatic
    fun setUp() {
      messagesBundles = collectAllMessageBundles(File("src/main/resources/message"))
      assertThat(messagesBundles).hasSizeGreaterThanOrEqualTo(1)

      kotlinSourceFiles = Paths.get("src/main/kotlin").collectAllFiles().filter { it.extension() == "kt" }
      assertThat(kotlinSourceFiles).hasSizeGreaterThanOrEqualTo(1)

      moduleKotlinClassesKotlinMain = Paths.get("build/classes/kotlin/main").collectAllFiles().filter { it.extension() == "class" }
      assertThat(moduleKotlinClassesKotlinMain).hasSizeGreaterThanOrEqualTo(1)
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
          val properties = Properties().apply {
            file.inputStream().use { load(it) }
          }

          val messagesBundle = result.getOrPut(bundleName) { MessagesBundle(bundleName) }
          messagesBundle.languageToMessages.put(languageKey, properties.stringPropertyNames().associate { it to properties.getProperty(it) })
        }

      return result.values.toList()
    }
  }
}
