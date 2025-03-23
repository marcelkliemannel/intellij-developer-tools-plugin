package dev.turingcomplete.intellijdevelopertoolsplugin.common

object TextStatisticUtils {
  // -- Variables --------------------------------------------------------------------------------------------------- //
  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exported Methods -------------------------------------------------------------------------------------------- //

  fun gatherStatistic(text: String): TextStatistic {
    var characterCount = 0
    var nonAsciiCharacterCount = 0
    var letterCount = 0
    var digitCount = 0
    var whitespaceCount = 0
    var lineBreakCount = 0
    var wordsCount = 0
    var sentencesCount = 0
    var isoControlCharactersCount = 0

    val uniqueCharacters = mutableMapOf<Char, Int>()
    val uniqueWords = mutableMapOf<String, Int>()

    var wordLengthSum = 0
    var wordsPerSentenceSum = 0

    val wordBuffer = StringBuilder()
    val sentenceBuffer = StringBuilder()

    text.forEachIndexed { index, character ->
      characterCount++
      if (character.code > 127) {
        nonAsciiCharacterCount++
      }
      if (Character.isISOControl(character)) {
        isoControlCharactersCount++
      }

      when {
        character.isLetter() -> {
          letterCount++
          wordBuffer.append(character)
          sentenceBuffer.append(character)
        }
        character.isDigit() -> {
          digitCount++
          wordBuffer.append(character)
          sentenceBuffer.append(character)
        }
        character.isWhitespace() -> {
          whitespaceCount++
          if (character == '\n' || (character == '\r' && index + 1 < text.length && text[index + 1] == '\n'))
            lineBreakCount++
          if (wordBuffer.isNotEmpty()) {
            wordsCount++
            val word = wordBuffer.toString()
            wordLengthSum += word.length
            uniqueWords[word] = uniqueWords.getOrDefault(word, 0) + 1
            wordBuffer.clear()
          }
          sentenceBuffer.append(character)
        }
        character == '.' -> {
          if (index + 1 < text.length && text[index + 1] != '.') {
            sentenceBuffer.append(character)
            sentencesCount++
            val sentence = sentenceBuffer.trim().toString()
            if (sentence.isNotEmpty()) {
              wordsPerSentenceSum += sentence.split("\\s+".toRegex()).size
            }
            sentenceBuffer.clear()
          }
        }
        else -> {
          sentenceBuffer.append(character)
        }
      }

      uniqueCharacters[character] = uniqueCharacters.getOrDefault(character, 0) + 1
    }

    if (wordBuffer.isNotEmpty()) {
      wordsCount++
      val word = wordBuffer.toString()
      wordLengthSum += word.length
      uniqueWords[word] = uniqueWords.getOrDefault(word, 0) + 1
    }
    val averageWordLength = if (wordsCount > 0) wordLengthSum.toDouble() / wordsCount else 0.0

    if (sentenceBuffer.isNotEmpty()) {
      val sentence = sentenceBuffer.trim().toString()
      if (sentence.isNotEmpty()) {
        sentencesCount++
        wordsPerSentenceSum += sentence.split("\\s+".toRegex()).size
      }
    }
    val averageWordsPerSentence = if (sentencesCount > 0) wordsPerSentenceSum.toDouble() / sentencesCount else 0.0

    val paragraphsCount = text.split("\\n\\r?\\n".toRegex()).size

    return TextStatistic(
      charactersCount = characterCount,
      lettersCount = letterCount,
      digitsCount = digitCount,
      nonAsciiCharactersCount = nonAsciiCharacterCount,
      whitespacesCount = whitespaceCount,
      lineBreaksCount = lineBreakCount,
      wordsCount = wordsCount,
      sentencesCount = sentencesCount,
      uniqueCharacters = uniqueCharacters,
      uniqueWords = uniqueWords,
      averageWordLength = averageWordLength,
      averageWordsPerSentence = averageWordsPerSentence,
      paragraphsCount = paragraphsCount,
      isoControlCharactersCount = isoControlCharactersCount
    )
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  data class TextStatistic(
    val charactersCount: Int = 0,
    val lettersCount: Int = 0,
    val digitsCount: Int = 0,
    val nonAsciiCharactersCount: Int = 0,
    val whitespacesCount: Int = 0,
    val lineBreaksCount: Int = 0,
    val wordsCount: Int = 0,
    val sentencesCount: Int = 0,
    val uniqueCharacters: Map<Char, Int>,
    val uniqueWords: Map<String, Int>,
    val averageWordLength: Double = 0.0,
    val averageWordsPerSentence: Double = 0.0,
    val paragraphsCount: Int = 0,
    val isoControlCharactersCount: Int = 0
  )
}