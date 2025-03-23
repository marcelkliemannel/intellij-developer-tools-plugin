package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

import com.jetbrains.rd.generator.nova.GenerationSpec.Companion.nullIfEmpty
import dev.turingcomplete.intellijdevelopertoolsplugin.common.uncheckedCastTo
import dev.turingcomplete.intellijdevelopertoolsplugin.settings.base.SettingValue.Companion.findAmbiguousSettingValueAnnotation
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
      SettingsContainer(clazz)
    ) as T
  }

  fun <T : Settings> T.settingsContainer(): SettingsContainer<T> =
    Proxy.getInvocationHandler(this).uncheckedCastTo<SettingsContainer<T>>()

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //

  open class SettingsContainer<T : Settings>(
    val kclass: KClass<T>
  ) : InvocationHandler {

    val settingProperties: LinkedHashMap<String, AnySettingProperty> by lazy { collectSettingsProperties() }
    private var modificationsCounter: Int = 0

    override fun invoke(proxy: Any, method: Method, args: Array<out Any>?): Any? {
      val methodName = method.name
      val noArgs = args?.isEmpty() != false
      return when {
        methodName == "getModificationsCounter" && noArgs -> modificationsCounter

        methodName == "getSetting" && args?.size == 1 -> settingProperties[args.first()] ?: error("Setting `${args.first()}` does not exist")

        methodName.startsWith("get") && noArgs -> {
          val settingName = methodName.removePrefix("get").replaceFirstChar { it.lowercase() }
          settingProperties[settingName] ?: error("Setting `$settingName` does not exist")
        }

        methodName.startsWith("set") && args?.size == 1 -> {
          val settingName = methodName.removePrefix("set").replaceFirstChar { it.lowercase() }
          val setting = settingProperties[settingName] ?: error("Setting `$settingName` does not exist")
          val value = args.first()
          if (setting.get() != value) {
            setting.set(value)
            modificationsCounter++
            setting
          }
          null
        }

        else -> error("Unknown method: $methodName(${args?.joinToString() ?: ""})")
      }
    }

    fun derivate() = DerivatedSettingsContainer(kclass, this)

    private fun collectSettingsProperties(): LinkedHashMap<String, AnySettingProperty> {
      val settingsGroups = kclass.findAnnotations<SettingsGroup>().associateBy { it.id }

      return kclass.memberProperties
        .filter { it.name != "modificationsCounter" }
        .associateTo(LinkedHashMap()) { property ->
          val setting = property.findAnnotation<Setting>()
          val internalSetting = property.findAnnotation<InternalSetting>()
          val (title, description, settingsGroup) = if (setting != null && internalSetting == null) {
            Triple(
              SettingsBundle.message(setting.titleBundleKey),
              setting.descriptionBundleKey.nullIfEmpty()?.let { SettingsBundle.message(it) },
              setting.groupId.nullIfEmpty()
                ?.let { settingsGroups[it] ?: error("Unknown settings group ID: $it") }
            )
          }
          else if (setting == null && internalSetting != null) {
            Triple("", "", null)
          }
          else {
            error("Property `${property.name}` requires either ${Setting::class.simpleName} or ${InternalSetting::class.simpleName} annotation")
          }

          val settingValue = property.findAmbiguousSettingValueAnnotation()
            ?: error("Property `${property.name}` is missing an ambiguous settings value annotation")

          val settingsProperty: AnySettingProperty = when (settingValue) {
            is IntValue -> IntSettingProperty(
              title = title,
              description = description,
              group = settingsGroup,
              settingValue = settingValue
            )

            is BooleanValue -> BooleanSettingProperty(
              title = title,
              description = description,
              group = settingsGroup,
              settingValue = settingValue
            )

            is EnumValue<*> -> EnumSettingProperty(
              title = title,
              description = description,
              group = settingsGroup,
              settingValue = settingValue
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
    val parent: SettingsContainer<T>
  ) : SettingsContainer<T>(clazz) {

    fun isModified(): Boolean = settingProperties.any {
      parent.settingProperties[it.key]!!.get() != it.value.get()
    }

    fun apply() {
      settingProperties.forEach { parent.settingProperties[it.key]!!.set(it.value.get()) }
    }

    fun reset() {
      settingProperties.forEach { it.value.set(parent.settingProperties[it.key]!!.get()) }
    }
  }

  // -- Inner Type ---------------------------------------------------------- //
}
