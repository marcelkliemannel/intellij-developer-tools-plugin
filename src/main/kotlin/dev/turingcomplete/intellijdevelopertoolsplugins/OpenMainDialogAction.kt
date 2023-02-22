package dev.turingcomplete.intellijdevelopertoolsplugins

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security

class OpenMainDialogAction : DumbAwareAction() {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    Security.addProvider(BouncyCastleProvider())
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun actionPerformed(e: AnActionEvent) {
    MainDialog(e.project).show()
  }

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}