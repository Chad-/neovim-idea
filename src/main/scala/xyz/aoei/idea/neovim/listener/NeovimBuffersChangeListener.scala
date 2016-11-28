package xyz.aoei.idea.neovim.listener

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.TextEditor
import xyz.aoei.idea.neovim.{BuffersChangeListener, CursorMoveListener, NeovimState}

import scala.concurrent.ExecutionContext.Implicits.global

class NeovimBuffersChangeListener extends BuffersChangeListener {
  override def buffersChanged(state: NeovimState): Unit = {
    if (state.windows.length != state.windowBuffers.length) return

    state.windows.zip(state.windowBuffers).foreach {
      case (window, buffer) => {
        for (lines <- buffer.getLines(0, -1, strict_indexing = false)) yield {
          val text = lines.mkString("\n")

          val editor = state.windowToEditor(window).getSelectedEditor.getEditors.head.asInstanceOf[TextEditor].getEditor

          val project = editor.getProject
          val document = editor.getDocument
          val caretModel = editor.getCaretModel

          WriteCommandAction.runWriteCommandAction(project, new Runnable {
            override def run(): Unit = {
              document.replaceString(0, document.getTextLength, text)

              // Make sure the cursor is updated immediately after changing text
              val cursor = state.windows.zip(state.cursors).filter(x => x._1 == window).head._2
              val curPos = new LogicalPosition(cursor._1, cursor._2)
              caretModel.moveToLogicalPosition(curPos)
            }
          })
        }
      }
    }
  }
}
