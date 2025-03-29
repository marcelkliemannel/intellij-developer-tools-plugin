package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

import com.jetbrains.rd.generator.nova.GenerationSpec.Companion.nullIfEmpty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.uncheckedCastTo
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingValue.Companion.findAmbiguousSettingValueAnnotation
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingsGroup.Companion.defaultSettingsGroup
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.message.SettingsBundle
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.findAnnotations
import kotlin.reflect.full.memberProperties

object SettingsHandler {
  // -- Properties ---------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exposed Methods ----------------------------------------------------- //

  fun <T : Settings> create(clazz: KClass<T>): T {
    @Suppress("UNCHECKED_CAST")
    return Proxy.newProxyInstance(
      clazz.java.classLoader,
      arrayOf(clazz.java),
      SettingsContainer(clazz),
    ) as T
  }

  fun <T : Settings> T.settingsContainer(): SettingsContainer<T> =
    Proxy.getInvocationHandler(this).uncheckedCastTo<SettingsContainer<T>>()

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  open class SettingsContainer<T : Settings>(val kclass: KClass<T>) : InvocationHandler {

    val settingProperties: LinkedHashMap<String, AnySettingProperty> by lazy {
      collectSettingsProperties()
    }

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
      val methodName = method.name
      val noArgs = args?.isEmpty() != false
      return when {
        methodName == "getModificationsCounter" && noArgs ->
          settingProperties.map { it.value.modificationsCounter }.sum()

        methodName == "getSetting" && args?.size == 1 ->
          settingProperties[args.first()] ?: error("Setting `${args.first()}` does not exist")

        methodName.startsWith("get") && noArgs -> {
          val settingName = methodName.removePrefix("get").replaceFirstChar { it.lowercase() }
          settingProperties[settingName] ?: error("Setting `$settingName` does not exist")
        }

        else -> error("Unknown method: $methodName(${args?.joinToString() ?: ""})")
      }
    }

    fun derivate() = DerivatedSettingsContainer(kclass, this)

    private fun collectSettingsProperties(): LinkedHashMap<String, AnySettingProperty> {
      val settingsGroups = kclass.findAnnotations<SettingsGroup>().associateBy { it.id }

      fun String.getSettingsGroup(): SettingsGroup =
        this.nullIfEmpty()?.let { settingsGroups[it] ?: error("Unknown settings group ID: $it") }
          ?: defaultSettingsGroup

      return kclass.memberProperties
        .filter { it.name != "modificationsCounter" }
        .associateTo(LinkedHashMap()) { property ->
          val setting = property.findAnnotation<Setting>()
          val internalSetting = property.findAnnotation<InternalSetting>()
          val (descriptor, settingsGroup) =
            if (setting != null && internalSetting == null) {
              Pair(
                SettingProperty.Descriptor(
                  title = SettingsBundle.message(setting.titleBundleKey),
                  description =
                    setting.descriptionBundleKey.nullIfEmpty()?.let { SettingsBundle.message(it) },
                  order = setting.order,
                ),
                setting.groupId.getSettingsGroup(),
              )
            } else if (setting == null && internalSetting != null) {
              Pair(null, internalSetting.groupId.getSettingsGroup())
            } else {
              error(
                "Property `${property.name}` requires either ${Setting::class.simpleName} or ${InternalSetting::class.simpleName} annotation"
              )
            }

          val settingValue =
            property.findAmbiguousSettingValueAnnotation()
              ?: error(
                "Property `${property.name}` is missing an ambiguous settings value annotation"
              )

          val settingsProperty: AnySettingProperty =
            when (settingValue) {
              is IntValue ->
                IntSettingProperty(
                  descriptor = descriptor,
                  group = settingsGroup,
                  settingValue = settingValue,
                )

              is BooleanValue ->
                BooleanSettingProperty(
                  descriptor = descriptor,
                  group = settingsGroup,
                  settingValue = settingValue,
                )

              is EnumValue<*> ->
                EnumSettingProperty(
                  descriptor = descriptor,
                  group = settingsGroup,
                  settingValue = settingValue,
                )

              else -> error("Unknown setting value annotation: $settingValue")
            }.uncheckedCastTo()

          assert((property.returnType.classifier as KClass<*>) == settingsProperty::class) {
            "Property `${property.name}` is not of type ${settingsProperty::class}"
          }

          property.name to settingsProperty
        }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //

  class DerivatedSettingsContainer<T : Settings>(
    clazz: KClass<T>,
    val parent: SettingsContainer<T>,
  ) : SettingsContainer<T>(clazz) {

    fun isModified(): Boolean =
      settingProperties.any { parent.settingProperties[it.key]!!.get() != it.value.get() }

    fun apply() {
      settingProperties.forEach { parent.settingProperties[it.key]!!.set(it.value.get()) }
    }

    fun reset() {
      settingProperties.forEach { it.value.set(parent.settingProperties[it.key]!!.get()) }
    }
  }
}
