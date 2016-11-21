import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent}

class EditorIllustration extends AnAction {
  override def actionPerformed(anActionEvent: AnActionEvent): Unit = {
    println("Testing!")
  }
}
