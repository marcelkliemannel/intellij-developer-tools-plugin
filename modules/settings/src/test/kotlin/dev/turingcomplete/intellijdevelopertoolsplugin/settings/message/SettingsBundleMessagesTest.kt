package dev.turingcomplete.intellijdevelopertoolsplugin.settings.message

import com.intellij.testFramework.junit5.RunMethodInEdt
import dev.turingcomplete.intellijdevelopertoolsplugin.common.nameWithoutExtension
import dev.turingcomplete.intellijdevelopertoolsplugin.common.testfixtures.BundleMessagesTest
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.Setting
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingsGroup
import java.nio.file.Path
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtProperty
import org.junit.jupiter.api.DynamicNode
import org.junit.jupiter.api.TestFactory

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

  override fun kotlinSourceFilesToScanForMessagesBundleUsages(): List<Path> {
    return super.kotlinSourceFilesToScanForMessagesBundleUsages().filter {
      // Will falsely see `modificationsCounter` LDC instruction as `message` argument
      it.fileName.toString() != "SettingsHandler\$SettingsContainer.class"
    }
  }

  override fun collectAllMessageBundleUsages(): List<MessagesBundleUsage> {
    val allMessageBundleUsages = super.collectAllMessageBundleUsages().toMutableList()

    val settingsRelatedAnnotations =
      setOf(Setting::class.simpleName!!, SettingsGroup::class.simpleName!!)

    kotlinSourceFilesToScanForMessagesBundleUsages().forEach { kotlinSourceFile ->
      val toKtFile = kotlinSourceFile.toKtFile()
      toKtFile
        .traverseElement(KtProperty::class)
        .plus(toKtFile.traverseElement(KtClassOrObject::class))
        .flatMap { it.annotationEntries }
        .filter { settingsRelatedAnnotations.contains(it.shortName!!.asString()) }
        .map {
          val args = it.valueArguments.associateBy { it.getArgumentName()?.asName?.asString() }

          listOfNotNull(
              "titleBundleKey" to args["titleBundleKey"],
              "descriptionBundleKey" to args["descriptionBundleKey"],
            )
            .map { it.first to it.second?.getArgumentExpression()?.text?.trim('"') }
            .filter { (_, messageKey) -> messageKey != null }
            .map { (type, messageKey) ->
              MessagesBundleUsage(
                className = kotlinSourceFile.nameWithoutExtension(),
                bundleName = SettingsBundle::class.simpleName!!,
                messageKey = messageKey!!,
                parametersCount = 0,
                displayableText = "$messageKey ($type)",
              )
            }
            .forEach { allMessageBundleUsages.add(it) }
        }
    }

    return allMessageBundleUsages
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
  // -- Companion Object ---------------------------------------------------- //
}
