package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common

import com.intellij.openapi.ui.getUserData
import com.intellij.openapi.ui.putUserData
import com.intellij.openapi.util.Key
import com.intellij.testFramework.junit5.RunMethodInEdt
import javax.swing.JPanel
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TitledTabbedPaneTest {
  @Test
  @RunMethodInEdt(writeIntent = RunMethodInEdt.WriteIntentMode.True)
  fun `on selection changed passes the original wrapped tab content`() {
    val selectionKey = Key.create<String>("selectionKey")
    val firstTab = JPanel().apply { putUserData(selectionKey, "first") }
    val secondTab = JPanel().apply { putUserData(selectionKey, "second") }
    val selections = mutableListOf<String>()

    val titledTabbedPane =
      TitledTabbedPane("Title", listOf("First" to firstTab, "Second" to secondTab)).apply {
        onSelectionChanged { selections.add(checkNotNull(it.getUserData(selectionKey))) }
      }

    titledTabbedPane.selectedIndex = 2

    assertThat(selections).containsExactly("second")
  }
}
