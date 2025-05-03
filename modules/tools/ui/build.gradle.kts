import org.gradle.internal.impldep.org.junit.experimental.categories.Categories.CategoryFilter.exclude
import org.gradle.kotlin.dsl.changelog
import org.gradle.kotlin.dsl.`java-test-fixtures`
import org.gradle.kotlin.dsl.libs
import org.gradle.kotlin.dsl.sourceSets
import org.jetbrains.changelog.Changelog

plugins {
  `java-test-fixtures`
  alias(libs.plugins.changelog)
}

dependencies {
  implementation(project(":common"))
  implementation(project(":settings"))

  implementation(libs.commons.text)
  implementation(libs.commons.codec)
  implementation(libs.commons.io)
  implementation(libs.commons.compress)
  implementation(libs.jose4j)
  implementation(libs.bundles.jackson)
  implementation(libs.named.regexp)
  implementation(libs.bundles.zxing)
  implementation(libs.sql.formatter)
  implementation(libs.csscolor4j)
  implementation(libs.jfiglet)
  implementation(libs.jsonpath)
  implementation(libs.json.schema.validator) {
    exclude("org.apache.commons", "commons-lang3")
  }
  implementation(libs.bundles.text.case.converter)
  implementation(libs.ulid.creator)
  implementation(libs.jnanoid)
  implementation(libs.uuid.generator)

  testImplementation(libs.assertj.core)
  testImplementation(libs.bundles.junit.implementation)
  testRuntimeOnly(libs.bundles.junit.runtime)
  testImplementation(testFixtures(project(":common")))

  testFixturesApi(project(":common"))
  testFixturesApi(project(":settings"))
}

changelog {
  path.set(rootProject.file("CHANGELOG.md").path)
}

val writeChangelogToFileTask =
  tasks.register("writeChangelogToFile") {
    val generatedResourcesDir = layout.buildDirectory.dir("generated-resources/changelog").get()
    outputs.dir(generatedResourcesDir)

    doLast {
      val renderResult =
        changelog.instance.get().releasedItems.joinToString("\n") {
          changelog.renderItem(it, Changelog.OutputType.HTML)
        }
      val baseDir = generatedResourcesDir.dir("dev/turingcomplete/intellijdevelopertoolsplugin")
      file(baseDir).mkdirs()
      file(baseDir.file("changelog.html")).writeText(renderResult)
    }
  }

sourceSets { main { resources { srcDir(writeChangelogToFileTask.map { it.outputs.files }) } } }

tasks.withType(org.jetbrains.changelog.tasks.InitializeChangelogTask::class).configureEach {
  enabled = false
}
tasks.withType(org.jetbrains.changelog.tasks.PatchChangelogTask::class).configureEach {
  enabled = false
}
