package dev.turingcomplete.intellijdevelopertoolsplugin.settings.message

import BundleMessagesTest
import com.intellij.testFramework.junit5.RunMethodInEdt
import dev.turingcomplete.intellijdevelopertoolsplugin.common.nameWithoutExtension
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingsGroup
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory
import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes

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

  override fun collectAllMessageBundleUsages(): List<MessagesBundleUsage> {
    val allMessageBundleUsages = super.collectAllMessageBundleUsages().toMutableList()

    moduleKotlinClassesKotlinMain.forEach { classFile ->
      val settingsGroupAnnotationUsages = mutableListOf<SettingsGroupAnnotationUsage>()

      classFile.readClass().accept(SettingsGroupUsageClassVisitor(settingsGroupAnnotationUsages), 0)

      settingsGroupAnnotationUsages.forEach {
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

  private class SettingsGroupUsageClassVisitor(
    val settingsGroupAnnotationUsages: MutableList<SettingsGroupAnnotationUsage>
  ) : ClassVisitor(Opcodes.ASM9, ClassWriter(0)) {

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
      if (descriptor.contains(SettingsGroup::class.qualifiedName!!.replace(".", "/"))) {
        return SettingsGroupUsageAnnotationVisitor(settingsGroupAnnotationUsages)
      }
      return super.visitAnnotation(descriptor, visible)
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private class SettingsGroupUsageAnnotationVisitor(
    val settingsGroupAnnotationUsages: MutableList<SettingsGroupAnnotationUsage>
  ) : AnnotationVisitor(Opcodes.ASM9) {

    private var id: String? = null
    private var titleBundleKey: String? = null
    private var descriptionBundleKey: String? = null

    override fun visitAnnotation(name: String?, descriptor: String?): AnnotationVisitor? {
      return SettingsGroupUsageAnnotationVisitor(settingsGroupAnnotationUsages)
    }

    override fun visitArray(name: String?): AnnotationVisitor? {
      return SettingsGroupUsageAnnotationVisitor(settingsGroupAnnotationUsages)
    }

    override fun visitEnd() {
      if (id != null) {
        val settingsGroupAnnotationUsage =
          SettingsGroupAnnotationUsage(
            id = id!!,
            titleBundleKey = titleBundleKey!!,
            descriptionBundleKey = descriptionBundleKey,
          )
        settingsGroupAnnotationUsages.add(settingsGroupAnnotationUsage)

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
        else -> error("Unexpected annotation property: $name")
      }
      super.visit(name, value)
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  private data class SettingsGroupAnnotationUsage(
    val id: String,
    val titleBundleKey: String,
    val descriptionBundleKey: String?,
  )

  // -- Companion Object ---------------------------------------------------- //
}
