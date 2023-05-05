@file:Suppress("UnstableApiUsage")

package dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator

import ai.grazie.utils.capitalize
import com.intellij.openapi.Disposable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.dsl.builder.COLUMNS_TINY
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.bindItem
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.columns
import com.intellij.ui.layout.ComboBoxPredicate
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolContext
import dev.turingcomplete.intellijdevelopertoolsplugins.DeveloperToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.ValidateMinIntValueSide.MAX
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.ValidateMinIntValueSide.MIN
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.bindIntTextImproved
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.validateLongValue
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.common.validateMinMaxValueRelation
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator.LoremIpsumGenerator.TextMode.BULLETS
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator.LoremIpsumGenerator.TextMode.PARAGRAPHS
import dev.turingcomplete.intellijdevelopertoolsplugins._internal.tool.generator.LoremIpsumGenerator.TextMode.WORDS
import java.security.SecureRandom
import kotlin.math.max
import kotlin.math.min

class LoremIpsumGenerator(configuration: DeveloperToolConfiguration, parentDisposable: Disposable) :
  MultiLineTextGenerator(
    generatedTextTitle = "Generated lorem ipsum",
    configuration = configuration,
    parentDisposable = parentDisposable
  ) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private var textMode = configuration.register("generatedTextKind", PARAGRAPHS)
  private var numberOfValues = configuration.register("numberOfValues", 9)
  private var minWordsInParagraph = configuration.register("minWordsInParagraph", DEFAULT_MIN_PARAGRAPH_WORDS)
  private var maxWordsInParagraph = configuration.register("maxWordsInParagraph", DEFAULT_MAX_PARAGRAPH_WORDS)
  private var minWordsInBullet = configuration.register("minWordsInBullet", DEFAULT_MIN_BULLET_WORDS)
  private var maxWordsInBullet = configuration.register("maxWordsInBullet", DEFAULT_MAX_BULLET_WORDS)
  private var startWithLoremIpsum = configuration.register("startWithLoremIpsum", true)

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun generate(): String = when (textMode.get()) {
    WORDS -> generateWords()
    PARAGRAPHS -> generateParagraphs()
    BULLETS -> generateBullets()
  }

  override fun Panel.buildConfigurationUi() {
    lateinit var textModeComboBox: ComboBox<TextMode>
    row {
      textField()
        .bindIntTextImproved(numberOfValues)
        .validateLongValue(LongRange(1, 999))
        .columns(COLUMNS_TINY)
        .gap(RightGap.SMALL)
      textModeComboBox = comboBox(TextMode.values().toList())
        .bindItem(textMode)
        .component
    }

    row {
      textField()
        .label("Minimum words in paragraph:")
        .bindIntTextImproved(minWordsInParagraph)
        .validateLongValue(LongRange(1, 999))
        .columns(COLUMNS_TINY)
        .validateMinMaxValueRelation(MIN) { maxWordsInParagraph.get() }
        .gap(RightGap.SMALL)
      textField()
        .label("Maximum:")
        .bindIntTextImproved(maxWordsInParagraph)
        .validateLongValue(LongRange(1, 999))
        .columns(COLUMNS_TINY)
        .validateMinMaxValueRelation(MAX) { minWordsInParagraph.get() }
    }.visibleIf(ComboBoxPredicate<TextMode>(textModeComboBox) { it == PARAGRAPHS })

    row {
      textField()
        .label("Minimum words in bullet:")
        .bindIntTextImproved(minWordsInBullet)
        .validateLongValue(LongRange(1, 999))
        .columns(COLUMNS_TINY)
        .validateMinMaxValueRelation(MIN) { maxWordsInBullet.get() }
        .gap(RightGap.SMALL)
      textField()
        .label("Maximum:")
        .bindIntTextImproved(maxWordsInBullet)
        .validateLongValue(LongRange(1, 999))
        .columns(COLUMNS_TINY)
        .validateMinMaxValueRelation(MAX) { minWordsInBullet.get() }
    }.visibleIf(ComboBoxPredicate<TextMode>(textModeComboBox) { it == BULLETS })

    row {
      checkBox("<html>Start with iconic <i>Lorem ipsum dolor sit amet…</i></html>")
        .bindSelected(startWithLoremIpsum)
    }
  }

  fun generateIconicText(atMostWords: Int, isSentence: Boolean): List<String> {
    val words = ICONIC_LOREM_IPSUM_SENTENCE.subList(0, min(ICONIC_LOREM_IPSUM_SENTENCE.size, atMostWords)).toMutableList()

    if (isSentence) {
      val wordsSize = words.size
      // Comma
      if (wordsSize > ICONIC_LOREM_IPSUM_SENTENCE_COMMA_INDEX + 1) {
        words[ICONIC_LOREM_IPSUM_SENTENCE_COMMA_INDEX] = "${words[ICONIC_LOREM_IPSUM_SENTENCE_COMMA_INDEX]},"
      }
      if (wordsSize >= 1) {
        // Capitalize first character
        words[0] = words[0].capitalize()
        // Full stop
        words[wordsSize - 1] = "${words[wordsSize - 1]}."
      }
    }

    return words
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //

  private fun generateParagraphs() = IntRange(0, numberOfValues.get() - 1).joinToString(PARAGRAPH_SEPARATOR) { paragraphIndex ->

    val totalWordsInParagraph = SECURE_RANDOM.nextInt(minWordsInParagraph.get(), maxWordsInParagraph.get() + 1)

    val initialWords = if (paragraphIndex == 0 && startWithLoremIpsum.get()) {
      generateIconicText(totalWordsInParagraph, true)
    }
    else {
      emptyList()
    }

    createSentences(initialWords, totalWordsInParagraph).joinToString(WORDS_SEPARATOR)
  }

  private fun generateWords(): String {
    val words = mutableListOf<String>()

    if (startWithLoremIpsum.get()) {
      words.addAll(generateIconicText(numberOfValues.get(), false))
    }

    if (words.size < numberOfValues.get()) {
      words.addAll(getRandomWords(numberOfValues.get() - words.size))
    }

    return words.joinToString(WORDS_SEPARATOR)
  }

  private fun generateBullets() = IntRange(0, numberOfValues.get() - 1).joinToString(BULLET_SEPARATOR) { bulletIndex ->
    val words = mutableListOf<String>()

    val totalWordsInBullet = SECURE_RANDOM.nextInt(minWordsInBullet.get(), maxWordsInBullet.get())

    if (bulletIndex == 0 && startWithLoremIpsum.get()) {
      words.addAll(generateIconicText(totalWordsInBullet, true))
    }

    // Avoid a single word sentence.
    words.addAll(createSentences(words, totalWordsInBullet))

    words[0] = "$BULLET_SYMBOL ${words[0]}"

    words.joinToString(WORDS_SEPARATOR)
  }

  private fun getRandomWords(words: Int): List<String> = IntRange(0, words - 1).asSequence()
    .map { getRandomWord() }
    .toList()

  private fun getRandomWord() = LOREM_IPSUM_WORDS[SECURE_RANDOM.nextInt(LOREM_IPSUM_WORDS.size)]

  private fun createSentence(words: List<String>): List<String> {
    // Divide sentence in n-1 fragments (the last fragment does not get a comma).
    val fragments = Math.floorDiv(words.size, TEXT_FRAGMENT_LENGTH) - 1
    val indiciesOfWordsWithCommas = IntRange(0, fragments - 1)
      // Randomly decide with a 2/3 change to put comma in fragment
      .filter { SECURE_RANDOM.nextInt(1, 4) != 3 }
      // Randomly decide index after the first word
      .map { fragmentIndex ->
        val commaIndexInFragment = SECURE_RANDOM.nextInt(1, TEXT_FRAGMENT_LENGTH)
        (TEXT_FRAGMENT_LENGTH * fragmentIndex) + commaIndexInFragment
      }
      .toSet()

    return words.mapIndexed { i: Int, rawWord: String ->
      var word = rawWord
      if (i == 0) {
        word = word.capitalize()
      }
      if (i == words.size - 1) {
        word = "$word."
      }
      if (indiciesOfWordsWithCommas.contains(i)) {
        word = "$word,"
      }
      word
    }
  }

  private fun createSentences(initialWords: List<String>, totalWords: Int): List<String> {
    val words = initialWords.toMutableList()

    while (words.size < totalWords) {
      val remainingWords = totalWords - words.size
      // With the max we ensue that there are at least `MIN_SENTENCE_WORDS` words.
      val minSentenceWords = max(min(MIN_SENTENCE_WORDS, remainingWords), MIN_SENTENCE_WORDS)
      val maxSentenceWords = max(min(MAX_SENTENCE_WORDS, remainingWords), MIN_SENTENCE_WORDS)
      val sentenceWords = if (minSentenceWords == maxSentenceWords) minSentenceWords else SECURE_RANDOM.nextInt(minSentenceWords, maxSentenceWords)
      val elements = createSentence(getRandomWords(sentenceWords))
      words.addAll(elements)
    }

    return words
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private enum class TextMode(val title: String) {

    PARAGRAPHS("Paragraphs"),
    WORDS("Words"),
    BULLETS("Bullets");

    override fun toString(): String = title
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperToolFactory<LoremIpsumGenerator> {

    override fun getDeveloperToolContext() = DeveloperToolContext(
      menuTitle = "Lorem Ipsum",
      contentTitle = "Lorem Ipsum Generator"
    )

    override fun getDeveloperToolCreator(
      project: Project?,
      parentDisposable: Disposable
    ): ((DeveloperToolConfiguration) -> LoremIpsumGenerator) = { configuration ->
      LoremIpsumGenerator(configuration, parentDisposable)
    }
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val SECURE_RANDOM = SecureRandom()
    private val ICONIC_LOREM_IPSUM_SENTENCE = listOf("lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit")
    private const val ICONIC_LOREM_IPSUM_SENTENCE_COMMA_INDEX = 4
    private val LOREM_IPSUM_WORDS: List<String> by lazy {
      LoremIpsumGenerator::class.java.getResource("/dev/turingcomplete/intellijdevelopertoolsplugin/lorem-ipsum.txt")!!.readText().lines()
    }
    private const val DEFAULT_MIN_PARAGRAPH_WORDS = 20
    private const val DEFAULT_MAX_PARAGRAPH_WORDS = 100
    private const val DEFAULT_MIN_BULLET_WORDS = 10
    private const val DEFAULT_MAX_BULLET_WORDS = 30
    private const val MIN_SENTENCE_WORDS = 3
    private const val MAX_SENTENCE_WORDS = 40
    private const val TEXT_FRAGMENT_LENGTH = 8
    private val PARAGRAPH_SEPARATOR = System.lineSeparator().repeat(2)
    private const val WORDS_SEPARATOR = " "
    private const val BULLET_SYMBOL = "-"
    private val BULLET_SEPARATOR = System.lineSeparator().repeat(2)
  }
}
