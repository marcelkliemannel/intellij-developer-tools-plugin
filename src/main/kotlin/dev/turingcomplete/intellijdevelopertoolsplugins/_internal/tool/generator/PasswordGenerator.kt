package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator

import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RowLayout
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.gridLayout.HorizontalAlign
import com.intellij.ui.layout.ValidationInfoBuilder
import com.intellij.ui.layout.selected
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.bindIntTextImproved
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.validateIntValue
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator.PasswordGenerator.LettersMode.ASCII_ALPHABET
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator.PasswordGenerator.LettersMode.ASCII_ALPHABET_ONLY_LOWERCASE
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator.PasswordGenerator.LettersMode.ASCII_ALPHABET_ONLY_UPPERCASE
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator.PasswordGenerator.LettersMode.NONE
import io.ktor.util.toCharArray
import org.apache.commons.text.RandomStringGenerator
import java.security.SecureRandom
import javax.swing.JComponent

internal class PasswordGenerator(
  private val configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable
) : OneLineTextGenerator(
  presentation = DeveloperToolContext(
    menuTitle = "Password Generator",
    contentTitle = "Password Generator",
    supportsReset = true
  ),
  configuration,
  parentDisposable,
  initialGeneratedTextTitle = "Generated password:"
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var length = configuration.register("length", DEFAULT_LENGTH)
  private var lettersMode = configuration.register("lettersMode", DEFAULT_LETTERS_MODE)
  private var addDigits = configuration.register("addDigits", DEFAULT_ADD_DIGITS)
  private var addSymbols = configuration.register("addSymbols", DEFAULT_ADD_SYMBOLS)
  private var symbols = configuration.register("symbols", DEFAULT_SYMBOLS)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildConfigurationUi() {
    rowsRange {
      row {
        textField()
          .label("Length:")
          .validateIntValue(IntRange(1, 100))
          .bindIntTextImproved(length)
          .horizontalAlign(HorizontalAlign.FILL)
      }.layout(RowLayout.PARENT_GRID)

      row {
        comboBox(LettersMode.values().toList())
          .label("Letters:")
          .bindItem(lettersMode)
          .validation(validateAtLeastOneCharacter())
          .component
      }.layout(RowLayout.PARENT_GRID)

      row {
        checkBox("Add digits")
          .bindSelected(addDigits)
          .validation(validateAtLeastOneCharacter())
          .horizontalAlign(HorizontalAlign.FILL)
      }.layout(RowLayout.PARENT_GRID)

      row {
        val addSymbolsCheckBox = checkBox("Add symbols:")
          .bindSelected(addSymbols)
          .validation(validateAtLeastOneCharacter())
          .component
        textField()
          .bindText(symbols)
          .enabledIf(addSymbolsCheckBox.selected)
          .horizontalAlign(HorizontalAlign.FILL)
      }.layout(RowLayout.PARENT_GRID)

      onReset {
        configuration.bulkChange {
          length.set(DEFAULT_LENGTH)
          lettersMode.set(DEFAULT_LETTERS_MODE)
          addDigits.set(DEFAULT_ADD_DIGITS)
          addSymbols.set(DEFAULT_ADD_SYMBOLS)
          symbols.set(DEFAULT_SYMBOLS)
        }
      }
    }
  }

  override fun generate(): String {
    val characters: CharArray = collectCharacters()
    val length by length
    return RandomStringGenerator.Builder()
      .usingRandom { SECURE_RANDOM.nextInt(it) }
      .selectFrom(*characters)
      .build()
      .generate(length, length)
  }

  private fun validateAtLeastOneCharacter(): ValidationInfoBuilder.(JComponent) -> ValidationInfo? = {
    if (collectCharacters().isEmpty()) {
      ValidationInfo("At least one character must be provided")
    }
    else {
      null
    }
  }

  private fun collectCharacters(): CharArray {
    var characters = when (lettersMode.get()) {
      NONE -> ""
      ASCII_ALPHABET -> ASCII_ALPHABET_LOWERCASE + ASCII_ALPHABET_UPPERCASE
      ASCII_ALPHABET_ONLY_LOWERCASE -> ASCII_ALPHABET_LOWERCASE
      ASCII_ALPHABET_ONLY_UPPERCASE -> ASCII_ALPHABET_UPPERCASE
    }

    if (addDigits.get()) {
      characters += DIGITS
    }

    if (addSymbols.get()) {
      characters += symbols.get()
    }

    return characters.toCharArray()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class LettersMode(val title: String) {

    NONE("None"),
    ASCII_ALPHABET("ASCII alphabet"),
    ASCII_ALPHABET_ONLY_LOWERCASE("ASCII alphabet - only lowercase"),
    ASCII_ALPHABET_ONLY_UPPERCASE("ASCII alphabet - only uppercase");

    override fun toString(): String = title
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory {

    override fun createDeveloperTool(configuration: DeveloperToolConfiguration, project: Project?, parentDisposable: Disposable) =
      PasswordGenerator(configuration, parentDisposable)
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val SECURE_RANDOM = SecureRandom()

    private const val DEFAULT_LENGTH = 30
    private val DEFAULT_LETTERS_MODE = ASCII_ALPHABET
    private const val DEFAULT_SYMBOLS = "~`!@#\$%^&*()_-+={[}]|\\:;\"'<,>.?/"
    private const val DEFAULT_ADD_SYMBOLS = true
    private const val DEFAULT_ADD_DIGITS = true

    private const val ASCII_ALPHABET_LOWERCASE = "abcdefghijklmnopqrstuvqxyz"
    private const val ASCII_ALPHABET_UPPERCASE = "ACBDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val DIGITS = "0123456789"
  }
}