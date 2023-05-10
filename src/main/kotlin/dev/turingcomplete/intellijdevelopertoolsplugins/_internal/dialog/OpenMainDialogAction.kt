package dev.turingcomplete.intellijdevelopertoolsplugins._internal.dialog

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAwareAction
import java.security.Provider
import java.security.Security

class OpenMainDialogAction : DumbAwareAction() {
  // -- Properties -------------------------------------------------------------------------------------------------- //

  private val log = logger<OpenMainDialogAction>()

  // -- Initialization ---------------------------------------------------------------------------------------------- //

  init {
    try {
      val bouncyCastleProviderClass = Class.forName("org.bouncycastle.jce.provider.BouncyCastleProvider")
      val bouncyCastleProvider = bouncyCastleProviderClass.getConstructor().newInstance()
      Security.addProvider(bouncyCastleProvider as Provider)
    }
    catch (e: Exception) {
      log.debug("Can't load BouncyCastleProvider", e)
    }
  }

  // -- Exposed Methods --------------------------------------------------------------------------------------------- //

  override fun actionPerformed(e: AnActionEvent) {
    MainDialog(e.project).show()
  }

  override fun getActionUpdateThread() = ActionUpdateThread.BGT

  // -- Private Methods --------------------------------------------------------------------------------------------- //
  // -- Inner Type -------------------------------------------------------------------------------------------------- //
  // -- Companion Object -------------------------------------------------------------------------------------------- //
}