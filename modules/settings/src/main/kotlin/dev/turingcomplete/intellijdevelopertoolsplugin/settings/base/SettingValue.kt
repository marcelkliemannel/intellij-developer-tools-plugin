package dev.turingcomplete.intellijdevelopertoolsplugin.settings.base

import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KClass
import kotlin.reflect.full.findAnnotations

@Target(AnnotationTarget.ANNOTATION_CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class SettingValue {

  companion object {

    val settingsAnnotations = setOf<KClass<out Annotation>>(BooleanValue::class, IntValue::class, EnumValue::class)

    fun KAnnotatedElement.findAmbiguousSettingValueAnnotation(): Annotation? {
      return settingsAnnotations.flatMap { this.findAnnotations(it) }.singleOrNull()
    }
  }
}
