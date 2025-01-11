@file:Suppress("UnstableApiUsage")

package dev.turingcomplete.intellijdevelopertoolsplugin._internal.tool.ui.transformer

import com.fasterxml.jackson.databind.node.ArrayNode
import com.intellij.codeInsight.actions.RearrangeCodeProcessor
import com.intellij.codeInsight.actions.ReformatCodeProcessor
import com.intellij.icons.AllIcons
import com.intellij.json.JsonLanguage
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PopupAction
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.Balloon
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.psi.PsiManager
import com.intellij.testFramework.LightVirtualFile
import com.intellij.ui.JBColor
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.awt.RelativePoint
import com.intellij.ui.components.JBLabel
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.LabelPosition
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.RightGap
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.actionButton
import com.intellij.ui.dsl.builder.bindSelected
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.whenStateChangedFromUi
import com.intellij.ui.dsl.builder.whenTextChangedFromUi
import com.intellij.util.ui.UIUtil
import com.jayway.jsonpath.Configuration
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.JsonPathException
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider
import com.jayway.jsonpath.spi.mapper.JacksonMappingProvider
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.ErrorHolder
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.copyable
import dev.turingcomplete.intellijdevelopertoolsplugin._internal.common.objectMapper
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration.PropertyType.CONFIGURATION
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperToolConfiguration.PropertyType.INPUT
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolContext
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolFactory
import dev.turingcomplete.intellijdevelopertoolsplugin.main.DeveloperUiToolPresentation
import javax.swing.JComponent

class JsonPathTransformer(
  context: DeveloperUiToolContext,
  configuration: DeveloperToolConfiguration,
  parentDisposable: Disposable,
  project: Project?
) : TextTransformer(
  textTransformerContext = TextTransformerContext(
    transformActionTitle = "Execute Query",
    sourceTitle = "Original",
    resultTitle = "Result",
    initialSourceExampleText = EXAMPLE_SOURCE,
    inputInitialLanguage = JsonLanguage.INSTANCE,
    outputInitialLanguage = JsonLanguage.INSTANCE
  ),
  context = context,
  configuration = configuration,
  parentDisposable = parentDisposable,
  project = project
) {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val queryText = configuration.register("contentText", "", INPUT, EXAMPLE_QUERY)
  private val formatResult = configuration.register("formatResult", true, CONFIGURATION)
  private var errorHolder = ErrorHolder()

  // -- Initialization ---------------------------------------------------------------------------------------------- //
  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun Panel.buildMiddleConfigurationUi() {
    row {
      expandableTextField()
        .bindText(queryText)
        .label("JSON path:", LabelPosition.TOP)
        .validationOnApply(errorHolder.asValidation())
        .validationRequestor(DUMMY_DIALOG_VALIDATION_REQUESTOR)
        .align(Align.FILL)
        .resizableColumn()
        .gap(RightGap.SMALL)
        .whenTextChangedFromUi { configurationChanged(queryText) }
      lateinit var helpButton: JComponent
      helpButton = actionButton(ShowOperatorsHelpPopup { helpButton }).component
    }
  }

  override fun Row.buildAdditionalActionsUi() {
    checkBox("Format result")
      .bindSelected(formatResult)
      .whenStateChangedFromUi { configurationChanged(queryText) }
  }

  override fun transform() {
    errorHolder.clear()

    val query = queryText.get()
    if (sourceText.get().isBlank() || query.isBlank()) {
      return
    }

    try {
      val resultJsonText = when (val resultJsonNode = JsonPath.parse(sourceText.get(), jsonPathConfiguration).read<Any>(query)) {
        is ArrayNode -> objectMapper.writeValueAsString(resultJsonNode)
        else -> resultJsonNode.toString()
      }
      if (formatResult.get()) {
        val workingVirtualFile = LightVirtualFile(this.javaClass.canonicalName, JsonLanguage.INSTANCE, resultJsonText)
        PsiManager.getInstance(project!!).findFile(workingVirtualFile)?.let { workingPsiFile ->
          val processor = RearrangeCodeProcessor(ReformatCodeProcessor(project, workingPsiFile, null, false))
          processor.setPostRunnable {
            resultText.set(workingPsiFile.text)
          }
          processor.run()
        } ?: error("snh: Can't get PSI file for `LightVirtualFile`")
      }
      else {
        resultText.set(resultJsonText)
      }
    } catch (e: JsonPathException) {
      errorHolder.add(e)
    }

    // The `validate` in this class is not used as a validation mechanism. We
    // make use of its text field error UI to display the `errorHolder`.
    validate()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  class Factory : DeveloperUiToolFactory<JsonPathTransformer> {

    override fun getDeveloperUiToolPresentation() = DeveloperUiToolPresentation(
      menuTitle = "JSON Path",
      contentTitle = "JSON Path Transformer"
    )

    override fun getDeveloperUiToolCreator(
      project: Project?,
      parentDisposable: Disposable,
      context: DeveloperUiToolContext
    ): ((DeveloperToolConfiguration) -> JsonPathTransformer)? {
      if (project == null) {
        return null
      }

      return { configuration ->
        JsonPathTransformer(context, configuration, parentDisposable, project)
      }
    }
  }

  // -- Inner Type -------------------------------------------------------------------------------------------------- //

  private class ShowOperatorsHelpPopup(val helpButton: () -> JComponent) :
    DumbAwareAction("Operators Help", null, AllIcons.General.ContextHelp), PopupAction {

    override fun actionPerformed(e: AnActionEvent) {
      JBPopupFactory.getInstance()
        .createBalloonBuilder(ScrollPaneFactory.createScrollPane(operatorsHelpPanel, true))
        .setDialogMode(true)
        .setFillColor(UIUtil.getPanelBackground())
        .setBorderColor(JBColor.border())
        .setBlockClicksThroughBalloon(true)
        .setRequestFocus(true)
        .createBalloon()
        .apply {
          setAnimationEnabled(false)
          show(RelativePoint.getCenterOf(helpButton()), Balloon.Position.atLeft)
        }
    }

    override fun getActionUpdateThread() = ActionUpdateThread.BGT
  }

  // -- Companion Object -------------------------------------------------------------------------------------------- //

  companion object {

    private val jsonPathConfiguration by lazy {
      Configuration.builder()
        .mappingProvider(JacksonMappingProvider())
        .jsonProvider(JacksonJsonNodeJsonProvider())
        .build()
    }

    @org.intellij.lang.annotations.Language("JSON")
    private const val EXAMPLE_SOURCE = """{
  "starWars": {
    "characters": [
      {
        "forename": "Luke",
        "surname": "Skywalker",
        "birthdate": "19 BBY",
        "homeWorld": "Tatooine"
      },
      {
        "forename": "Leia",
        "surname": "Organa",
        "birthdate": "19 BBY",
        "homeWorld": "Alderaan"
      },
      {
        "forename": "Ben",
        "surname": "Solo",
        "birthdate": "5 ABY",
        "homeWorld": "Chandrila"
      }
    ],
    "ships": [
      {
        "type": "Imperial Star Destroyer",
        "class": "Carrier"
      },
      {
        "type": "X-Wing Starfighter",
        "class": "Space superiority fighter"
      }
    ]
  }
}"""

    private const val EXAMPLE_QUERY = "\$.starWars.characters..forename"

    private val operatorsHelpPanel = JBLabel(
      """
          <html>
          <h2>Operators</h2>
          <table>
              <tr><td><b>Operator</b></td><td><b>Description</b></td></tr>
              <tr><td><code>${'$'}</code></td><td>The root element to query. This starts all path expressions.</td></tr>
              <tr><td><code>@</code></td><td>The current node being processed by a filter predicate.</td></tr>
              <tr><td><code>*</code></td><td>Wildcard. Available anywhere a name or numeric are required.</td></tr>
              <tr><td><code>..</code></td><td>Selects all the descendant elements of the current node.</td></tr>
              <tr><td><code>.&lt;name&gt;</code></td><td>Selects the child element with the specified name.</td></tr>
              <tr><td><code>[&#39;&lt;name&gt;&#39; (, &#39;&lt;name&gt;&#39;)]</code></td><td>Bracket-notated child or children.</td></tr>
              <tr><td><code>[&lt;number&gt; (, &lt;number&gt;)]</code></td><td>Array index or indexes.</td></tr>
              <tr><td><code>[&lt;start&gt;:&lt;end&gt;]</code></td><td>Selects a range of child elements.</td></tr>
              <tr><td><code>[&lt;start&gt;:&lt;end&gt;:&lt;step&gt;]</code></td><td>Selects a range of child elements with the specified step.</td></tr>
              <tr><td><code>[?(&lt;expression&gt;)]</code></td><td>Filter expression. Expression must evaluate to a boolean value.</td></tr>
          </table>
          
          <h2>Examples</h2>
          <table>
              <tr><td><b>Example</b></td><td><b>Description</b></td></tr>
              <tr><td><code>${'$'}</code></td><td>Selects the root.</td></tr>
              <tr><td><code>${'$'}.movie[0]</code></td><td>Selects the first movie.</td></tr>
              <tr><td><code>${'$'}.movie[*]</code></td><td>Selects all movies.</td></tr>
              <tr><td><code>${'$'}.movie[*].director</code></td><td>Selects the directors of all movies.</td></tr>
              <tr><td><code>${'$'}..movie[0]</code></td><td>Selects the first movie in all sub-objects of the root.</td></tr>
              <tr><td><code>${'$'}..length</code></td><td>Selects the lengths of all movies, including sub-objects.</td></tr>
              <tr><td><code>${'$'}.movie[?(@.length&lt;100)]</code></td><td>Selects all length in the store with a length than 100 units.</td></tr>
              <tr><td><code>${'$'}.movie[?(@.director=='Lucas')]</code></td><td>Selects all movies directed by Lucas.</td></tr>
              <tr><td><code>${'$'}.movie[2:].title</code></td><td>Selects the title of all movies starting from the third movie.</td></tr>
              <tr><td><code>${'$'}..movie[-1:].director</code></td><td>Selects the director of the last movie in all sub-objects of the root.</td></tr>
          </table>
          </html>
        """.trimIndent()
    ).copyable()
  }
}