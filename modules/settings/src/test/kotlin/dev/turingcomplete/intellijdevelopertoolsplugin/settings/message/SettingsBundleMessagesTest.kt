package dev.turingcomplete.intellijdevelopertoolsplugin.settings.message

import com.intellij.testFramework.junit5.RunMethodInEdt
import dev.turingcomplete.intellijdevelopertoolsplugin.common.nameWithoutExtension
import dev.turingcomplete.intellijdevelopertoolsplugin.common.testfixtures.BundleMessagesTest
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.Setting
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingsGroup
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.nio.file.Path

class SettingsBundleMessagesTest : BundleMessagesTest() {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  @TestFactory
  override fun `test that all additional languages are containing the same message keys and parameter counts`():
    List<DynamicNode> =
    `do test that all additional languages are containing the same message keys and parameter counts`()

  @TestFactory
  @RunMethodInEdt(writeIntent = RunMethodInEdt.WriteIntentMode.True)
  override fun `test that all message(messageKey, params) calls are referencing to an existing message key and are using the correct parameters count`():
    List<DynamicNode> =
    `do test that all message(messageKey, params) calls are referencing to an existing message key and are using the correct parameters count`()

  @TestFactory
  @RunMethodInEdt(writeIntent = RunMethodInEdt.WriteIntentMode.True)
  override fun `test that all keys in the messages bundle are used`(): List<DynamicNode> =
    `do test that all keys in the messages bundle are used`()

  override fun classFilesToScanForMessagesBundleUsages(): List<Path> {
    return super.classFilesToScanForMessagesBundleUsages().filter {
      // Will falsely see `modificationsCounter` LDC instruction as `message` argument
      it.fileName.toString() != "SettingsHandler\$SettingsContainer.class"
    }
  }

  override fun collectAllMessageBundleUsages(): List<MessagesBundleUsage> {
    val allMessageBundleUsages = super.collectAllMessageBundleUsages().toMutableList()

    classFilesToScanForMessagesBundleUsages().forEach { classFile ->
      val settingsRelatedAnnotationUsages = mutableListOf<SettingsRelatedAnnotationUsage>()

      classFile
        .readClass()
        .accept(SettingsRelatedUsageClassVisitor(settingsRelatedAnnotationUsages), 0)

      settingsRelatedAnnotationUsages.forEach {
        listOfNotNull(
            "titleBundleKey" to it.titleBundleKey,
            "descriptionBundleKey" to it.descriptionBundleKey,
          )
          .filter { (_, messageKey) -> messageKey != null }
          .map { (type, messageKey) ->
            MessagesBundleUsage(
              className = classFile.nameWithoutExtension(),
              bundleName = SettingsBundle::class.simpleName!!,
              messageKey = messageKey!!,
              parametersCount = 0,
              displayableText = "$messageKey ($type property for group ID: ${it.id})",
            )
          }
          .forEach { allMessageBundleUsages.add(it) }
      }
    }

    return allMessageBundleUsages
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  private class SettingsRelatedUsageClassVisitor(
    val settingsRelatedAnnotationUsages: MutableList<SettingsRelatedAnnotationUsage>
  ) : ClassVisitor(Opcodes.ASM9, ClassWriter(0)) {

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
      if (settingsRelatedAnnotationsInternalNames.any { descriptor.contains(it) }) {
        return SettingsGroupUsageAnnotationVisitor(settingsRelatedAnnotationUsages)
      }
      return super.visitAnnotation(descriptor, visible)
    }

    /**
     * The annotations on a properties in a Kotlin source file will be added as getter methods to a
     * separated nested class with the name `DefaultImpls`.
     */
    override fun visitMethod(
      access: Int,
      name: String?,
      descriptor: String?,
      signature: String?,
      exceptions: Array<out String?>?,
    ): MethodVisitor? {
      return SettingsRelatedUsageMethodVisitor(settingsRelatedAnnotationUsages)
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class SettingsRelatedUsageMethodVisitor(
    val settingsRelatedAnnotationUsages: MutableList<SettingsRelatedAnnotationUsage>
  ) : MethodVisitor(Opcodes.ASM9) {

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
      if (settingsRelatedAnnotationsInternalNames.any { descriptor.contains(it) }) {
        return SettingsGroupUsageAnnotationVisitor(settingsRelatedAnnotationUsages)
      }
      return super.visitAnnotation(descriptor, visible)
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class SettingsGroupUsageAnnotationVisitor(
    val settingsRelatedAnnotationUsages: MutableList<SettingsRelatedAnnotationUsage>
  ) : AnnotationVisitor(Opcodes.ASM9) {

    private var id: String? = null
    private var titleBundleKey: String? = null
    private var descriptionBundleKey: String? = null

    override fun visitAnnotation(name: String?, descriptor: String?): AnnotationVisitor? {
      return SettingsGroupUsageAnnotationVisitor(settingsRelatedAnnotationUsages)
    }

    override fun visitArray(name: String?): AnnotationVisitor? {
      return SettingsGroupUsageAnnotationVisitor(settingsRelatedAnnotationUsages)
    }

    override fun visitEnd() {
      if (titleBundleKey != null) {
        val settingsRelatedAnnotationUsage =
          SettingsRelatedAnnotationUsage(
            id = id,
            titleBundleKey = titleBundleKey!!,
            descriptionBundleKey = descriptionBundleKey,
          )
        settingsRelatedAnnotationUsages.add(settingsRelatedAnnotationUsage)

        id = null
        titleBundleKey = null
        descriptionBundleKey = null
      }

      super.visitEnd()
    }

    override fun visit(name: String, value: Any?) {
      when (name) {
        "id" -> id = value as String
        "titleBundleKey" -> titleBundleKey = value as String
        "descriptionBundleKey" -> descriptionBundleKey = value as String
      }
      super.visit(name, value)
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private data class SettingsRelatedAnnotationUsage(
    val id: String?,
    val titleBundleKey: String,
    val descriptionBundleKey: String?,
  )

  // -- Companion Object ---------------------------------------------------- //

  companion object {

    private val settingsRelatedAnnotationsInternalNames: List<String> =
      listOf(SettingsGroup::class, Setting::class).map { it.qualifiedName!!.replace(".", "/") }
  }
}
