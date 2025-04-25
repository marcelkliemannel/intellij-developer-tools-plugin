
import org.jetbrains.changelog.Changelog
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.RunIdeTask
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.INTERNAL_API_USAGES
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.INVALID_PLUGIN
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.NON_EXTENDABLE_API_USAGES
import org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.OVERRIDE_ONLY_API_USAGES
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

fun properties(key: String) = project.findProperty(key).toString()

plugins {
  java
  alias(libs.plugins.kotlin.jvm)
  alias(libs.plugins.intellij.platform)
  alias(libs.plugins.changelog)
  alias(libs.plugins.spotless)
}

subprojects { apply(plugin = "org.jetbrains.intellij.platform.module") }

val platform = properties("platform")

allprojects {
  apply(plugin = "java")
  apply(plugin = "kotlin")
  apply(plugin = "com.diffplug.spotless")

  group = properties("pluginGroup")
  version = properties("pluginVersion")

  repositories {
    mavenLocal()
    mavenCentral()

    intellijPlatform { defaultRepositories() }
  }

  dependencies {
    intellijPlatform {
      create(platform, properties("platformVersion"), false)
      bundledPlugins(properties("platformGlobalBundledPlugins").split(','))

      testFramework(TestFrameworkType.Platform)
      testFramework(TestFrameworkType.JUnit5)
    }
  }

  spotless { kotlin { ktfmt().googleStyle() } }

  java { toolchain { languageVersion.set(JavaLanguageVersion.of(21)) } }

  tasks {
    withType<KotlinCompile> {
      compilerOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict", "-opt-in=kotlin.ExperimentalStdlibApi")
        jvmTarget.set(JvmTarget.JVM_21)
      }
    }

    withType<Test> {
      useJUnitPlatform()
      systemProperty("java.awt.headless", "false")
    }

    named("check") { dependsOn("spotlessCheck") }
  }
}

dependencies {
  intellijPlatform {
    pluginVerifier()
    zipSigner()

    pluginModule(implementation(project(":common")))
    pluginModule(implementation(project(":settings")))
    pluginModule(implementation(project(":tools-editor")))
    pluginModule(implementation(project(":tools-ui")))
    if (platform == "IC") {
      pluginModule(implementation(project(":java-dependent")))
      pluginModule(implementation(project(":kotlin-dependent")))
    }
  }

  testImplementation(libs.assertj.core)
  testImplementation(libs.bundles.junit.implementation)
  testRuntimeOnly(libs.bundles.junit.runtime)
  testImplementation(libs.commons.csv)
  testImplementation(testFixtures(project(":common")))
  testImplementation(testFixtures(project(":tools-ui")))
}

intellijPlatform {
  pluginConfiguration {
    id = providers.gradleProperty("pluginId")
    version = providers.gradleProperty("pluginVersion")

    ideaVersion {
      sinceBuild = properties("pluginSinceBuild")
      untilBuild = provider { null }
    }

    changeNotes.set(
      provider {
        changelog.renderItem(changelog.get(project.version as String), Changelog.OutputType.HTML)
      }
    )
  }

  signing {
    val jetbrainsDir = File(System.getProperty("user.home"), ".jetbrains")
    certificateChain.set(
      project.provider { File(jetbrainsDir, "plugin-sign-chain.crt").readText() }
    )
    privateKey.set(
      project.provider { File(jetbrainsDir, "plugin-sign-private-key.pem").readText() }
    )
    password.set(project.provider { properties("jetbrains.sign-plugin.password") })
  }

  publishing {
    token.set(project.provider { properties("jetbrains.marketplace.token") })
    channels.set(
      listOf(properties("pluginVersion").split('-').getOrElse(1) { "default" }.split('.').first())
    )
  }

  pluginVerification {
    failureLevel.set(
      listOf(
        COMPATIBILITY_PROBLEMS,
        INTERNAL_API_USAGES,
        NON_EXTENDABLE_API_USAGES,
        OVERRIDE_ONLY_API_USAGES,
        INVALID_PLUGIN,
        // Will fail for non-IC IDEs
        // MISSING_DEPENDENCIES
      )
    )

    ides {
      recommended()

      properties("pluginVerificationAdditionalIdes").split(",").forEach { ide ->
        ide(ide, properties("platformVersion"))
      }
    }
  }
}

changelog {
  val projectVersion = project.version as String
  version.set(projectVersion)
  header.set("$projectVersion - ${org.jetbrains.changelog.date()}")
  groups.set(listOf("Added", "Changed", "Removed", "Fixed"))
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

sourceSets { main { resources { srcDir(writeChangelogToFileTask) } } }

tasks {
  named("publishPlugin") {
    dependsOn("check")

    doFirst { check(platform == "IC") { "Expected platform 'IC', but was: '$platform'" } }
  }

  named("buildSearchableOptions") { enabled = false }

  named<RunIdeTask>("runIde") {
    jvmArgumentProviders += CommandLineArgumentProvider {
      // https://kotlin.github.io/analysis-api/testing-in-k2-locally.html
      listOf("-Didea.kotlin.plugin.use.k2=true")
    }
  }
}
