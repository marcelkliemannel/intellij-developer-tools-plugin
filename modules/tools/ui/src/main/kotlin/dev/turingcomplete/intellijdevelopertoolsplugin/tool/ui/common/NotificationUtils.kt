package dev.turingcomplete.intellijdevelopertoolsplugin.tool.ui.common

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project

object NotificationUtils {
  // -- Variables ----------------------------------------------------------- //
  // -- Initialization ------------------------------------------------------ //
  // -- Exported Methods ---------------------------------------------------- //

  fun notifyOnToolWindow(
    message: String,
    project: Project?,
    notificationType: NotificationType = NotificationType.INFORMATION,
    vararg actions: AnAction,
  ) {
    ApplicationManager.getApplication().invokeLater {
      val notification =
        NotificationGroupManager.getInstance()
          .getNotificationGroup("Developer Tools Plugin Notifications")
          .createNotification(message, notificationType)

      actions.forEach { notification.addAction(it) }

      notification.notify(project)
    }
  }

  // -- Private Methods ----------------------------------------------------- //
  // -- Inner Type ---------------------------------------------------------- //
}
